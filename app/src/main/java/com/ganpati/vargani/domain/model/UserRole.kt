package com.ganpati.vargani.domain.model

/**
 * App role stored on the Firestore [users] document.
 * Admin = full access. Viewer = always can read; add/edit/delete follows settings.viewersEnabled.
 */
enum class UserRole {
    ADMIN,
    VIEWER;

    /** Role-level default only — prefer [AuthSessionState.canWrite] which includes the admin toggle. */
    val canWrite: Boolean get() = this == ADMIN

    companion object {
        fun fromStorage(value: String?): UserRole =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: VIEWER
    }
}
