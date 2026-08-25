package com.ganpati.vargani.domain.model

/**
 * Authenticated app user profile (Firebase Auth UID as [uid]).
 * [id] remains a stable Long for any legacy callers (hash of uid).
 */
data class AuthUser(
    val id: Long = 0L,
    val uid: String = "",
    val name: String,
    val email: String = "",
    val mobile: String = "",
    val role: UserRole = UserRole.VIEWER,
    val committeeId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
