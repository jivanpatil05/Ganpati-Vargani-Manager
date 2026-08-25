package com.ganpati.vargani.data.repository

import android.content.Context
import com.ganpati.vargani.data.remote.FirestoreMappers
import com.ganpati.vargani.data.remote.FirestoreMappers.toFestivalEvent
import com.ganpati.vargani.data.remote.FirestoreMappers.toMember
import com.ganpati.vargani.data.remote.FirestoreMappers.toUserProfile
import com.ganpati.vargani.data.remote.FirestorePaths
import com.ganpati.vargani.data.remote.UserSessionStore
import com.ganpati.vargani.domain.model.AppUserProfile
import com.ganpati.vargani.domain.model.FestivalEvent
import com.ganpati.vargani.domain.model.Member
import com.ganpati.vargani.domain.model.UserRole
import com.ganpati.vargani.domain.repository.AuthSessionState
import com.ganpati.vargani.domain.repository.EventRepository
import com.ganpati.vargani.domain.repository.MemberRepository
import com.ganpati.vargani.domain.repository.UserManagementRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserManagementRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionStore: UserSessionStore,
    @ApplicationContext private val appContext: Context,
) : UserManagementRepository {

    companion object {
        const val MAX_VIEWERS = 2
        private const val SECONDARY_APP_NAME = "varganiInviteAuth"
    }

    override fun observeCommitteeUsers(): Flow<List<AppUserProfile>> = callbackFlow {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var registration: ListenerRegistration? = null
        var job: Job? = null

        fun bind(committeeId: String) {
            registration?.remove()
            if (committeeId.isBlank()) {
                trySend(emptyList())
                return
            }
            registration = firestore.collection(FirestorePaths.USERS)
                .whereEqualTo("committeeId", committeeId)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    trySend(snap?.documents?.mapNotNull { it.toUserProfile() }.orEmpty())
                }
        }

        job = scope.launch {
            sessionStore.session.collect { state: AuthSessionState ->
                if (!state.isLoggedIn) trySend(emptyList()) else bind(state.committeeId)
            }
        }
        awaitClose {
            registration?.remove()
            job?.cancel()
            scope.coroutineContext[Job]?.cancel()
        }
    }

    override suspend fun setUserRole(uid: String, role: UserRole) {
        sessionStore.requireAdminAccess()
        firestore.collection(FirestorePaths.USERS).document(uid)
            .update("role", role.name)
            .await()
    }

    /**
     * Creates a Viewer Auth user on a secondary FirebaseApp so the admin session
     * on the default app stays signed in. The profile is written with the *new*
     * user's auth token (self-create rule), which avoids admin write permission issues.
     */
    override suspend fun inviteViewer(
        email: String,
        name: String,
        password: String,
        mobile: String,
    ) {
        sessionStore.requireAdminAccess()
        val committeeId = sessionStore.requireCommitteeId()
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Name is required." }
        require(trimmedEmail.contains("@")) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must be at least 6 characters." }

        val viewerCount = runCatching {
            firestore.collection(FirestorePaths.USERS)
                .whereEqualTo("committeeId", committeeId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toUserProfile() }
                .count { it.role == UserRole.VIEWER }
        }.getOrDefault(0)
        if (viewerCount >= MAX_VIEWERS) {
            error("You can create at most $MAX_VIEWERS viewer accounts.")
        }

        val secondaryApp = secondaryApp()
        val inviteAuth = FirebaseAuth.getInstance(secondaryApp)
        val inviteDb = FirebaseFirestore.getInstance(secondaryApp)
        try {
            val result = inviteAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val user = result.user ?: error("Could not create viewer account.")
            runCatching {
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmedName)
                        .build(),
                ).await()
            }
            val profile = AppUserProfile(
                uid = user.uid,
                name = trimmedName,
                email = trimmedEmail,
                mobile = mobile.trim(),
                role = UserRole.VIEWER,
                committeeId = committeeId,
                createdAt = System.currentTimeMillis(),
            )
            try {
                // Write as the new user so Firestore "self-create" rules allow it.
                inviteDb.collection(FirestorePaths.USERS).document(user.uid)
                    .set(FirestoreMappers.userProfileToMap(profile))
                    .await()
            } catch (writeError: Exception) {
                runCatching { user.delete().await() }
                throw writeError
            }
        } finally {
            inviteAuth.signOut()
        }
    }

    private fun secondaryApp(): FirebaseApp {
        val existing = runCatching { FirebaseApp.getInstance(SECONDARY_APP_NAME) }.getOrNull()
        return existing ?: FirebaseApp.initializeApp(
            appContext,
            FirebaseApp.getInstance().options,
            SECONDARY_APP_NAME,
        ) ?: error("Could not start invite auth.")
    }
}

@Singleton
class FirestoreMemberRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionStore: UserSessionStore,
) : MemberRepository {

    override fun observeMembers(): Flow<List<Member>> = callbackFlow {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var registration: ListenerRegistration? = null
        var job: Job? = null

        fun bind(committeeId: String) {
            registration?.remove()
            if (committeeId.isBlank()) {
                trySend(emptyList())
                return
            }
            registration = firestore.collection(FirestorePaths.MEMBERS)
                .whereEqualTo("committeeId", committeeId)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    trySend(snap?.documents?.mapNotNull { it.toMember() }.orEmpty())
                }
        }

        job = scope.launch {
            sessionStore.session.collect { state: AuthSessionState ->
                if (!state.isLoggedIn) trySend(emptyList()) else bind(state.committeeId)
            }
        }
        awaitClose {
            registration?.remove()
            job?.cancel()
            scope.coroutineContext[Job]?.cancel()
        }
    }

    override suspend fun saveMember(member: Member): String {
        sessionStore.requireWriteAccess()
        val committeeId = sessionStore.requireCommitteeId()
        val ref = if (member.id.isBlank()) {
            firestore.collection(FirestorePaths.MEMBERS).document()
        } else {
            firestore.collection(FirestorePaths.MEMBERS).document(member.id)
        }
        ref.set(FirestoreMappers.memberToMap(member.copy(id = ref.id, committeeId = committeeId))).await()
        return ref.id
    }

    override suspend fun deleteMember(id: String) {
        sessionStore.requireWriteAccess()
        firestore.collection(FirestorePaths.MEMBERS).document(id).delete().await()
    }
}

@Singleton
class FirestoreEventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionStore: UserSessionStore,
) : EventRepository {

    override fun observeEvents(): Flow<List<FestivalEvent>> = callbackFlow {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var registration: ListenerRegistration? = null
        var job: Job? = null

        fun bind(committeeId: String) {
            registration?.remove()
            if (committeeId.isBlank()) {
                trySend(emptyList())
                return
            }
            registration = firestore.collection(FirestorePaths.EVENTS)
                .whereEqualTo("committeeId", committeeId)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    trySend(snap?.documents?.mapNotNull { it.toFestivalEvent() }.orEmpty())
                }
        }

        job = scope.launch {
            sessionStore.session.collect { state: AuthSessionState ->
                if (!state.isLoggedIn) trySend(emptyList()) else bind(state.committeeId)
            }
        }
        awaitClose {
            registration?.remove()
            job?.cancel()
            scope.coroutineContext[Job]?.cancel()
        }
    }

    override suspend fun saveEvent(event: FestivalEvent): String {
        sessionStore.requireWriteAccess()
        val committeeId = sessionStore.requireCommitteeId()
        val ref = if (event.id.isBlank()) {
            firestore.collection(FirestorePaths.EVENTS).document()
        } else {
            firestore.collection(FirestorePaths.EVENTS).document(event.id)
        }
        ref.set(FirestoreMappers.eventToMap(event.copy(id = ref.id, committeeId = committeeId))).await()
        return ref.id
    }

    override suspend fun deleteEvent(id: String) {
        sessionStore.requireWriteAccess()
        firestore.collection(FirestorePaths.EVENTS).document(id).delete().await()
    }
}
