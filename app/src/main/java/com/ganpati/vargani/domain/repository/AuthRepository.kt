package com.ganpati.vargani.domain.repository

import com.ganpati.vargani.domain.model.AuthUser
import com.ganpati.vargani.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeSession(): Flow<AuthSessionState>
    suspend fun isLoggedIn(): Boolean
    suspend fun loginWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        mobile: String = "",
    )
    suspend fun logout()
    suspend fun currentRole(): UserRole
    suspend fun canWrite(): Boolean

    /** @deprecated Prefer [loginWithEmail] / [signUpWithEmail]. Kept for OTP screen compatibility. */
    suspend fun findUserByMobile(mobile: String): AuthUser? = null

    /** @deprecated OTP flow replaced by Firebase Email/Password. */
    suspend fun registerUser(name: String, mobile: String): AuthUser =
        error("Use signUpWithEmail")

    /** @deprecated OTP flow replaced by Firebase Email/Password. */
    suspend fun savePendingOtp(otp: String, mobile: String, name: String, isSignUp: Boolean) = Unit

    /** @deprecated OTP flow replaced by Firebase Email/Password. */
    suspend fun getPendingOtp(): PendingOtpState? = null

    /** @deprecated OTP flow replaced by Firebase Email/Password. */
    suspend fun clearPendingOtp() = Unit

    /** @deprecated Prefer [loginWithEmail] / [signUpWithEmail]. */
    suspend fun completeLogin(name: String, mobile: String) = Unit
}

data class AuthSessionState(
    val isLoggedIn: Boolean = false,
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val role: UserRole = UserRole.VIEWER,
    val committeeId: String = "",
    /** Mirrors committee settings.viewersEnabled — viewers may add/edit/delete when true. */
    val viewersCanWrite: Boolean = true,
) {
    /** Admin always; Viewer only when [viewersCanWrite] is on. Does not affect read access. */
    val canWrite: Boolean
        get() = isLoggedIn && (
            role == UserRole.ADMIN || (role == UserRole.VIEWER && viewersCanWrite)
        )
    val isAdmin: Boolean get() = isLoggedIn && role == UserRole.ADMIN
}

data class PendingOtpState(
    val otp: String,
    val mobile: String,
    val name: String,
    val isSignUp: Boolean,
    val expiresAtMillis: Long,
)
