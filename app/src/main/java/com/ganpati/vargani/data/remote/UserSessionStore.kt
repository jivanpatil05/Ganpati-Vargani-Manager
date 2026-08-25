package com.ganpati.vargani.data.remote

import com.ganpati.vargani.domain.model.UserRole
import com.ganpati.vargani.domain.repository.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory mirror of the signed-in user's Firestore profile for repository scoping.
 */
@Singleton
class UserSessionStore @Inject constructor() {

    private val _session = MutableStateFlow(AuthSessionState())
    val session: StateFlow<AuthSessionState> = _session.asStateFlow()

    fun update(state: AuthSessionState) {
        _session.value = state
    }

    fun clear() {
        _session.value = AuthSessionState()
    }

    fun requireCommitteeId(): String {
        val id = _session.value.committeeId
        require(id.isNotBlank()) { "No committee linked to this account. Sign in again." }
        return id
    }

    fun requireWriteAccess() {
        val state = _session.value
        require(state.isLoggedIn) { "Please sign in to continue." }
        require(state.canWrite) {
            "Add, edit and delete are turned off for viewers. Ask an admin to enable them."
        }
    }

    fun requireAdminAccess() {
        val state = _session.value
        require(state.isLoggedIn) { "Please sign in to continue." }
        require(state.isAdmin) { "Only an admin can do this." }
    }

    fun currentRole(): UserRole = _session.value.role

    fun patch(block: AuthSessionState.() -> AuthSessionState) {
        _session.update(block)
    }
}
