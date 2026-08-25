package com.ganpati.vargani.data.repository

import com.ganpati.vargani.core.constants.AppConstants
import com.ganpati.vargani.core.utils.FirebaseErrorMapper
import com.ganpati.vargani.data.remote.FirestoreMappers
import com.ganpati.vargani.data.remote.FirestoreMappers.toUserProfile
import com.ganpati.vargani.data.remote.FirestorePaths
import com.ganpati.vargani.data.remote.UserSessionStore
import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.model.AppUserProfile
import com.ganpati.vargani.domain.model.AuthUser
import com.ganpati.vargani.domain.model.Committee
import com.ganpati.vargani.domain.model.UserRole
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.repository.AuthSessionState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication (Email/Password) + Firestore [users]/[committees]/[settings] bootstrap.
 */
@Singleton
class FirebaseAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionStore: UserSessionStore,
) : AuthRepository {

    override fun observeSession(): Flow<AuthSessionState> = callbackFlow {
        var profileRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            profileRegistration?.remove()
            profileRegistration = null
            val user = firebaseAuth.currentUser
            if (user == null) {
                sessionStore.clear()
                trySend(AuthSessionState())
            } else {
                val cached = sessionStore.session.value
                if (cached.uid == user.uid && cached.isLoggedIn && cached.committeeId.isNotBlank()) {
                    trySend(cached)
                } else {
                    trySend(
                        AuthSessionState(
                            isLoggedIn = true,
                            uid = user.uid,
                            name = user.displayName.orEmpty(),
                            email = user.email.orEmpty(),
                        ),
                    )
                }
                // Snapshot listener works offline from cache; avoids "client is offline" on get().
                profileRegistration = firestore.collection(FirestorePaths.USERS).document(user.uid)
                    .addSnapshotListener { snap, _ ->
                        val profile = snap?.toUserProfile()
                        val viewersCanWrite = sessionStore.session.value.viewersCanWrite
                        val state = if (profile != null && profile.committeeId.isNotBlank()) {
                            AuthSessionState(
                                isLoggedIn = true,
                                uid = profile.uid,
                                name = profile.name,
                                email = profile.email,
                                mobile = profile.mobile,
                                role = profile.role,
                                committeeId = profile.committeeId,
                                viewersCanWrite = viewersCanWrite,
                            )
                        } else {
                            AuthSessionState(
                                isLoggedIn = true,
                                uid = user.uid,
                                name = user.displayName.orEmpty().ifBlank { profile?.name.orEmpty() },
                                email = user.email.orEmpty(),
                                mobile = profile?.mobile.orEmpty(),
                                role = profile?.role ?: UserRole.VIEWER,
                                committeeId = profile?.committeeId.orEmpty(),
                                viewersCanWrite = viewersCanWrite,
                            )
                        }
                        sessionStore.update(state)
                        trySend(state)
                    }
            }
        }
        auth.addAuthStateListener(listener)
        auth.currentUser?.let { listener.onAuthStateChanged(auth) }
            ?: trySend(AuthSessionState())
        awaitClose {
            profileRegistration?.remove()
            auth.removeAuthStateListener(listener)
        }
    }.distinctUntilChanged()

    override suspend fun isLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun loginWithEmail(email: String, password: String) {
        try {
            runCatching { firestore.enableNetwork().await() }
            withTimeout(30_000) {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                ensureUserProfile(
                    name = auth.currentUser?.displayName.orEmpty(),
                    email = email.trim(),
                    mobile = "",
                    isNewSignUp = false,
                )
            }
        } catch (e: Exception) {
            throw IllegalStateException(FirebaseErrorMapper.message(e), e)
        }
    }

    override suspend fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        mobile: String,
    ) {
        try {
            runCatching { firestore.enableNetwork().await() }
            withTimeout(45_000) {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val user = result.user ?: error("Account creation failed")

                runCatching {
                    user.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build(),
                    ).await()
                }

                // New account: skip Firestore get() (fails when client is briefly offline).
                ensureUserProfile(
                    name = name.trim(),
                    email = email.trim(),
                    mobile = mobile.trim(),
                    isNewSignUp = true,
                )
            }
        } catch (e: Exception) {
            throw IllegalStateException(FirebaseErrorMapper.message(e), e)
        }
    }

    /**
     * Creates committee + user + settings if missing (signup), or loads existing profile (login).
     */
    private suspend fun ensureUserProfile(
        name: String,
        email: String,
        mobile: String,
        isNewSignUp: Boolean,
    ) {
        val user = auth.currentUser ?: error("Not signed in")

        if (!isNewSignUp) {
            val existingProfile = loadProfileOfflineFriendly(user.uid)
            if (existingProfile != null && existingProfile.committeeId.isNotBlank()) {
                sessionStore.update(
                    AuthSessionState(
                        isLoggedIn = true,
                        uid = existingProfile.uid,
                        name = existingProfile.name.ifBlank { name },
                        email = existingProfile.email.ifBlank { email },
                        mobile = existingProfile.mobile.ifBlank { mobile },
                        role = existingProfile.role,
                        committeeId = existingProfile.committeeId,
                        viewersCanWrite = sessionStore.session.value.viewersCanWrite,
                    ),
                )
                return
            }
        }

        val committeeRef = firestore.collection(FirestorePaths.COMMITTEES).document()
        val committee = Committee(
            id = committeeRef.id,
            name = AppConstants.DEFAULT_ORG_NAME,
            address = "",
            createdBy = user.uid,
            createdAt = System.currentTimeMillis(),
        )
        val profile = AppUserProfile(
            uid = user.uid,
            name = name.ifBlank { user.displayName.orEmpty() }.ifBlank { email.substringBefore("@") },
            email = email.ifBlank { user.email.orEmpty() },
            mobile = mobile,
            role = UserRole.ADMIN,
            committeeId = committee.id,
            createdAt = System.currentTimeMillis(),
        )
        val settings = AppSettings(
            organizationName = AppConstants.DEFAULT_ORG_NAME,
            receiptPrefix = AppConstants.DEFAULT_RECEIPT_PREFIX,
            receiptCounter = AppConstants.DEFAULT_RECEIPT_START,
        )

        // Writes succeed while offline (queued); do not block signup on server ack forever.
        committeeRef.set(FirestoreMappers.committeeToMap(committee)).await()
        firestore.collection(FirestorePaths.USERS).document(user.uid)
            .set(FirestoreMappers.userProfileToMap(profile))
            .await()
        runCatching {
            firestore.collection(FirestorePaths.SETTINGS).document(committee.id)
                .set(FirestoreMappers.settingsToMap(settings, committee.id))
                .await()
        }

        sessionStore.update(
            AuthSessionState(
                isLoggedIn = true,
                uid = profile.uid,
                name = profile.name,
                email = profile.email,
                mobile = profile.mobile,
                role = profile.role,
                committeeId = profile.committeeId,
            ),
        )
    }

    /** Cache-first read so login works without throwing when Firestore is offline. */
    private suspend fun loadProfileOfflineFriendly(uid: String): AppUserProfile? {
        runCatching {
            val cached = firestore.collection(FirestorePaths.USERS).document(uid)
                .get(Source.CACHE)
                .await()
            if (cached.exists()) return cached.toUserProfile()
        }
        return runCatching {
            firestore.collection(FirestorePaths.USERS).document(uid)
                .get(Source.DEFAULT)
                .await()
                .toUserProfile()
        }.getOrNull()
    }

    override suspend fun logout() {
        auth.signOut()
        sessionStore.clear()
    }

    override suspend fun currentRole(): UserRole = sessionStore.currentRole()

    override suspend fun canWrite(): Boolean = sessionStore.session.value.canWrite

    override suspend fun findUserByMobile(mobile: String): AuthUser? = null
}
