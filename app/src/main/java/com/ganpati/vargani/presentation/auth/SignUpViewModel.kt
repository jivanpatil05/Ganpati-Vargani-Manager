package com.ganpati.vargani.presentation.auth

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.FirebaseErrorMapper
import com.ganpati.vargani.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthFormUiState())
    val uiState: StateFlow<AuthFormUiState> = _uiState.asStateFlow()

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(email = value, emailError = null, errorMessage = null)
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(password = value, passwordError = null, errorMessage = null)
        }
    }

    fun onMobileChanged(value: String) {
        _uiState.update {
            it.copy(
                mobile = value.filter { ch -> ch.isDigit() || ch == '+' }.take(13),
                mobileError = null,
                errorMessage = null,
            )
        }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToHome = false, navigateToOtp = false) }
    }

    fun submitSignUp() {
        val name = _uiState.value.name.trim()
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val mobile = _uiState.value.mobile.trim()

        var hasError = false
        if (name.isBlank()) {
            _uiState.update { it.copy(nameError = context.getString(R.string.auth_error_name_required)) }
            hasError = true
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update {
                it.copy(emailError = context.getString(R.string.auth_error_email_invalid))
            }
            hasError = true
        }
        if (password.length < 6) {
            _uiState.update {
                it.copy(passwordError = context.getString(R.string.auth_error_password_short))
            }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                authRepository.signUpWithEmail(
                    name = name,
                    email = email,
                    password = password,
                    mobile = mobile,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(isLoading = false, navigateToHome = true, errorMessage = null)
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = FirebaseErrorMapper.message(
                            e,
                            context.getString(R.string.error_generic),
                        ),
                    )
                }
            }
        }
    }
}
