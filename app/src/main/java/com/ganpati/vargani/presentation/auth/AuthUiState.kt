package com.ganpati.vargani.presentation.auth

data class AuthFormUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val mobile: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val mobileError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val navigateToHome: Boolean = false,
    /** @deprecated OTP flow replaced by Firebase Email/Password */
    val navigateToOtp: Boolean = false,
)

data class OtpUiState(
    val otpInput: String = "",
    val maskedMobile: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val canResend: Boolean = true,
)
