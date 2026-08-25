package com.ganpati.vargani.presentation.users

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.data.repository.FirestoreUserManagementRepository
import com.ganpati.vargani.domain.model.AppUserProfile
import com.ganpati.vargani.domain.model.UserRole
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.repository.UserManagementRepository
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageUsersUiState(
    val isAdmin: Boolean = false,
    val currentUid: String = "",
    val users: List<AppUserProfile> = emptyList(),
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val mobile: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val formError: String? = null,
) {
    val viewerCount: Int get() = users.count { it.role == UserRole.VIEWER }
    val canCreateViewer: Boolean
        get() = isAdmin && viewerCount < FirestoreUserManagementRepository.MAX_VIEWERS
    val remainingViewerSlots: Int
        get() = (FirestoreUserManagementRepository.MAX_VIEWERS - viewerCount).coerceAtLeast(0)
}

@HiltViewModel
class ManageUsersViewModel @Inject constructor(
    private val userManagementRepository: UserManagementRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageUsersUiState())
    val uiState: StateFlow<ManageUsersUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        isAdmin = session.isAdmin,
                        currentUid = session.uid,
                    )
                }
            }
        }
        viewModelScope.launch {
            userManagementRepository.observeCommitteeUsers().collect { users ->
                _uiState.update { it.copy(users = users.sortedByDescending { u -> u.role == UserRole.ADMIN }) }
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, formError = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun onMobileChanged(value: String) {
        _uiState.update { it.copy(mobile = value.filter { ch -> ch.isDigit() }.take(10), formError = null) }
    }

    fun createViewer() {
        val state = _uiState.value
        if (!state.canCreateViewer || state.isSubmitting) return

        val nameError = if (state.name.isBlank()) {
            context.getString(R.string.auth_error_name_required)
        } else {
            null
        }
        val emailError = if (!state.email.contains("@") || !state.email.contains(".")) {
            context.getString(R.string.auth_error_email_invalid)
        } else {
            null
        }
        val passwordError = if (state.password.length < 6) {
            context.getString(R.string.auth_error_password_short)
        } else {
            null
        }
        if (nameError != null || emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, formError = null) }
            runCatching {
                userManagementRepository.inviteViewer(
                    email = state.email.trim(),
                    name = state.name.trim(),
                    password = state.password,
                    mobile = state.mobile,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        name = "",
                        email = "",
                        password = "",
                        mobile = "",
                        nameError = null,
                        emailError = null,
                        passwordError = null,
                        formError = null,
                    )
                }
                _uiEvents.send(
                    UiEvent.ShowSnackbar(context.getString(R.string.manage_users_created)),
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        formError = friendlyError(error),
                    )
                }
            }
        }
    }

    private fun friendlyError(error: Throwable): String {
        val authCode = (error as? FirebaseAuthException)?.errorCode
        return when {
            authCode == "ERROR_EMAIL_ALREADY_IN_USE" ->
                context.getString(R.string.manage_users_email_in_use)
            error.message?.contains("at most", ignoreCase = true) == true ->
                context.getString(R.string.manage_users_limit_reached)
            !error.message.isNullOrBlank() -> error.message!!
            else -> context.getString(R.string.error_generic)
        }
    }
}
