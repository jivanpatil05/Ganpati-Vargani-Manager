package com.ganpati.vargani.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.MobileUtils
import com.ganpati.vargani.core.utils.WhatsAppOtpHelper
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
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val pending = authRepository.getPendingOtp()
            if (pending != null) {
                _uiState.update {
                    it.copy(maskedMobile = maskMobile(pending.mobile))
                }
            }
        }
    }

    fun onOtpChanged(value: String) {
        _uiState.update {
            it.copy(
                otpInput = value.filter { ch -> ch.isDigit() }.take(6),
                errorMessage = null,
            )
        }
    }

    fun verifyOtp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val pending = authRepository.getPendingOtp()
            if (pending == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.auth_otp_expired),
                    )
                }
                return@launch
            }
            if (System.currentTimeMillis() > pending.expiresAtMillis) {
                authRepository.clearPendingOtp()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.auth_otp_expired),
                    )
                }
                return@launch
            }
            if (_uiState.value.otpInput != pending.otp) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.auth_otp_invalid),
                    )
                }
                return@launch
            }

            val name = if (pending.isSignUp) {
                authRepository.registerUser(pending.name, pending.mobile).name
            } else {
                authRepository.findUserByMobile(pending.mobile)?.name ?: pending.name
            }
            authRepository.completeLogin(name = name, mobile = pending.mobile)
            _uiState.update { it.copy(isLoading = false, isVerified = true) }
        }
    }

    fun resendOtp() {
        viewModelScope.launch {
            val pending = authRepository.getPendingOtp() ?: run {
                _uiState.update {
                    it.copy(errorMessage = context.getString(R.string.auth_otp_expired))
                }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null, canResend = false) }
            val otp = MobileUtils.generateOtp()
            when (
                WhatsAppOtpHelper.sendOtp(
                    context = context,
                    mobile = pending.mobile,
                    otp = otp,
                    appName = context.getString(R.string.app_name),
                )
            ) {
                WhatsAppOtpHelper.SendResult.Success -> {
                    authRepository.savePendingOtp(
                        otp = otp,
                        mobile = pending.mobile,
                        name = pending.name,
                        isSignUp = pending.isSignUp,
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            canResend = true,
                            otpInput = "",
                            errorMessage = null,
                        )
                    }
                }
                WhatsAppOtpHelper.SendResult.WhatsAppNotInstalled -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            canResend = true,
                            errorMessage = context.getString(R.string.auth_whatsapp_not_installed),
                        )
                    }
                }
                WhatsAppOtpHelper.SendResult.NoWhatsAppAccountOrSendFailed -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            canResend = true,
                            errorMessage = context.getString(R.string.auth_no_whatsapp_account),
                        )
                    }
                }
            }
        }
    }

    private fun maskMobile(mobile: String): String {
        if (mobile.length < 4) return mobile
        return "******${mobile.takeLast(4)}"
    }
}
