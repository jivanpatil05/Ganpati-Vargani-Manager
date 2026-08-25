package com.ganpati.vargani.domain.model

data class Committee(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class Member(
    val id: String = "",
    val committeeId: String = "",
    val name: String = "",
    val mobile: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class FestivalEvent(
    val id: String = "",
    val committeeId: String = "",
    val title: String = "",
    val description: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Firestore [users] profile linked to Firebase Auth UID.
 */
data class AppUserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val role: UserRole = UserRole.VIEWER,
    val committeeId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
