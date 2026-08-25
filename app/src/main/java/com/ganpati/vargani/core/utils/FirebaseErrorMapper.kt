package com.ganpati.vargani.core.utils

/**
 * Maps Firebase / network failures to user-facing messages without leaking internals.
 */
object FirebaseErrorMapper {
    fun message(throwable: Throwable, fallback: String = "Something went wrong. Please try again."): String {
        val raw = buildString {
            var t: Throwable? = throwable
            while (t != null) {
                if (!t.message.isNullOrBlank()) {
                    if (isNotEmpty()) append(" | ")
                    append(t.message)
                }
                t = t.cause
            }
        }
        return when {
            raw.contains("network", ignoreCase = true) ||
                raw.contains("Unable to resolve host", ignoreCase = true) ||
                raw.contains("UNAVAILABLE", ignoreCase = true) ||
                raw.contains("Timed out", ignoreCase = true) ->
                "No internet connection. Check your network and retry."
            raw.contains("password is invalid", ignoreCase = true) ||
                raw.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                raw.contains("invalid-credential", ignoreCase = true) ||
                raw.contains("ERROR_INVALID_CREDENTIAL", ignoreCase = true) ||
                raw.contains("supplied auth credential is incorrect", ignoreCase = true) ||
                raw.contains("malformed or has expired", ignoreCase = true) ||
                raw.contains("wrong-password", ignoreCase = true) ||
                raw.contains("user-not-found", ignoreCase = true) ||
                raw.contains("USER_NOT_FOUND", ignoreCase = true) ->
                "Incorrect email or password. If you just signed up and this fails, use Sign up again or reset password in Firebase Console."
            raw.contains("email address is already in use", ignoreCase = true) ||
                raw.contains("EMAIL_EXISTS", ignoreCase = true) ||
                raw.contains("email-already-in-use", ignoreCase = true) ->
                "This email is already registered. Please log in."
            raw.contains("badly formatted", ignoreCase = true) ||
                raw.contains("INVALID_EMAIL", ignoreCase = true) ->
                "Enter a valid email address."
            raw.contains("weak-password", ignoreCase = true) ||
                raw.contains("Password should be at least", ignoreCase = true) ->
                "Password must be at least 6 characters."
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ||
                raw.contains("permission-denied", ignoreCase = true) ->
                "You do not have permission for this action. Deploy updated Firestore rules from firestore.rules."
            raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
                raw.contains("configuration not found", ignoreCase = true) ->
                "Firebase Auth is not set up. In Firebase Console open Authentication → Sign-in method → enable Email/Password, then try again."
            raw.contains("client is offline", ignoreCase = true) ||
                rawContainsOffline(raw) ->
                "No internet / Firestore offline. Check Wi‑Fi or mobile data, confirm Firestore is created in Firebase Console, then retry."
            raw.contains("API key not valid", ignoreCase = true) ||
                raw.contains("API_KEY_INVALID", ignoreCase = true) ->
                "Invalid Firebase API key. Replace google-services.json with the file downloaded from your Firebase project."
            raw.contains("read-only", ignoreCase = true) ||
                raw.contains("Viewer accounts", ignoreCase = true) ->
                raw
            else -> raw.ifBlank { fallback }
        }
    }

    private fun rawContainsOffline(raw: String): Boolean =
        raw.contains("Failed to get document because the client is offline", ignoreCase = true)
}
