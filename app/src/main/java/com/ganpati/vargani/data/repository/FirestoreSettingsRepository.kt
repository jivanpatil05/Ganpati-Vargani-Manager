package com.ganpati.vargani.data.repository

import com.ganpati.vargani.core.constants.AppConstants
import com.ganpati.vargani.data.local.datastore.SettingsDataStore
import com.ganpati.vargani.data.remote.FirestoreMappers
import com.ganpati.vargani.data.remote.FirestoreMappers.toAppSettings
import com.ganpati.vargani.data.remote.FirestorePaths
import com.ganpati.vargani.data.remote.UserSessionStore
import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.repository.AuthSessionState
import com.ganpati.vargani.domain.repository.SettingsRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-committee settings document: settings/{committeeId}
 *
 * Appearance (dark mode / language) is device-local so Viewers can change theme
 * without Firestore write permission.
 */
@Singleton
class FirestoreSettingsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionStore: UserSessionStore,
    private val settingsDataStore: SettingsDataStore,
) : SettingsRepository {

    private fun settingsDoc(committeeId: String) =
        firestore.collection(FirestorePaths.SETTINGS).document(committeeId)

    override fun observeSettings(): Flow<AppSettings> {
        // Keep a Firestore listener alive for the signed-in committee.
        val remoteFlow = callbackFlow {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            var registration: ListenerRegistration? = null
            var collectJob: Job? = null

            fun bind(committeeId: String) {
                registration?.remove()
                if (committeeId.isBlank()) {
                    trySend(AppSettings())
                    return
                }
                registration = settingsDoc(committeeId).addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(AppSettings())
                        return@addSnapshotListener
                    }
                    val settings = snap?.toAppSettings() ?: AppSettings()
                    sessionStore.patch { copy(viewersCanWrite = settings.viewersEnabled) }
                    trySend(settings)
                    // Sync shared committee fields only — keep local theme/language.
                    scope.launch { runCatching { settingsDataStore.saveSharedFields(settings) } }
                }
            }

            collectJob = scope.launch {
                sessionStore.session.collect { state: AuthSessionState ->
                    if (!state.isLoggedIn) {
                        registration?.remove()
                        trySend(AppSettings())
                    } else {
                        bind(state.committeeId)
                    }
                }
            }

            awaitClose {
                registration?.remove()
                collectJob?.cancel()
                scope.coroutineContext[Job]?.cancel()
            }
        }

        return combine(remoteFlow, settingsDataStore.settingsFlow) { remote, local ->
            remote.copy(
                darkMode = local.darkMode,
                dynamicColor = local.dynamicColor,
                languageCode = local.languageCode,
            )
        }
    }

    override suspend fun getSettings(): AppSettings {
        val committeeId = sessionStore.session.value.committeeId
        if (committeeId.isBlank()) return AppSettings()
        val fromCache = runCatching {
            settingsDoc(committeeId).get(com.google.firebase.firestore.Source.CACHE).await()
        }.getOrNull()
        if (fromCache != null && fromCache.exists()) {
            return fromCache.toAppSettings()
        }
        val snap = runCatching {
            settingsDoc(committeeId).get().await()
        }.getOrNull()
        return if (snap != null && snap.exists()) {
            snap.toAppSettings()
        } else {
            val defaults = AppSettings(
                organizationName = AppConstants.DEFAULT_ORG_NAME,
                receiptPrefix = AppConstants.DEFAULT_RECEIPT_PREFIX,
                receiptCounter = AppConstants.DEFAULT_RECEIPT_START,
            )
            runCatching {
                settingsDoc(committeeId)
                    .set(FirestoreMappers.settingsToMap(defaults, committeeId))
                    .await()
            }
            defaults
        }
    }

    override suspend fun saveSettings(settings: AppSettings) {
        // Always persist appearance locally (Viewers and Admins).
        settingsDataStore.saveAppearance(
            darkMode = settings.darkMode,
            dynamicColor = settings.dynamicColor,
            languageCode = settings.languageCode,
        )

        if (!sessionStore.session.value.isAdmin) {
            // Viewer: local theme/language only — no Firestore write.
            return
        }

        val committeeId = sessionStore.requireCommitteeId()
        settingsDoc(committeeId)
            .set(FirestoreMappers.settingsToMap(settings, committeeId))
            .await()
        settingsDataStore.save(settings)
        sessionStore.patch { copy(viewersCanWrite = settings.viewersEnabled) }
    }

    override suspend fun nextReceiptNumber(): String {
        sessionStore.requireWriteAccess()
        val committeeId = sessionStore.requireCommitteeId()
        val ref = settingsDoc(committeeId)
        return firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val current = if (snap.exists()) snap.toAppSettings() else AppSettings()
            val number = current.receiptCounter.coerceAtLeast(1L)
            val prefix = current.receiptPrefix.ifBlank { AppConstants.DEFAULT_RECEIPT_PREFIX }
            val receipt = "%s-%04d".format(prefix, number)
            val updated = current.copy(receiptCounter = number + 1)
            tx.set(ref, FirestoreMappers.settingsToMap(updated, committeeId))
            receipt
        }.await()
    }

    override suspend fun peekNextReceiptNumber(): String {
        val settings = getSettings()
        val prefix = settings.receiptPrefix.ifBlank { AppConstants.DEFAULT_RECEIPT_PREFIX }
        return "%s-%04d".format(prefix, settings.receiptCounter.coerceAtLeast(1L))
    }
}
