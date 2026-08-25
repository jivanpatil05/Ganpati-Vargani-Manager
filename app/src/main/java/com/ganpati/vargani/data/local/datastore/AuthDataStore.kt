package com.ganpati.vargani.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "vargani_auth")

data class AuthSession(
    val isLoggedIn: Boolean = false,
    val mobile: String = "",
    val name: String = "",
)

data class PendingOtp(
    val otp: String,
    val mobile: String,
    val name: String,
    val isSignUp: Boolean,
    val expiresAtMillis: Long,
)

@Singleton
class AuthDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val MOBILE = stringPreferencesKey("mobile")
        val NAME = stringPreferencesKey("name")
        val PENDING_OTP = stringPreferencesKey("pending_otp")
        val PENDING_MOBILE = stringPreferencesKey("pending_mobile")
        val PENDING_NAME = stringPreferencesKey("pending_name")
        val PENDING_IS_SIGNUP = booleanPreferencesKey("pending_is_signup")
        val OTP_EXPIRES_AT = stringPreferencesKey("otp_expires_at")
    }

    val sessionFlow: Flow<AuthSession> = context.authDataStore.data.map { prefs ->
        AuthSession(
            isLoggedIn = prefs[Keys.LOGGED_IN] ?: false,
            mobile = prefs[Keys.MOBILE].orEmpty(),
            name = prefs[Keys.NAME].orEmpty(),
        )
    }

    suspend fun setLoggedIn(name: String, mobile: String) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.LOGGED_IN] = true
            prefs[Keys.NAME] = name
            prefs[Keys.MOBILE] = mobile
            prefs.remove(Keys.PENDING_OTP)
            prefs.remove(Keys.PENDING_MOBILE)
            prefs.remove(Keys.PENDING_NAME)
            prefs.remove(Keys.PENDING_IS_SIGNUP)
            prefs.remove(Keys.OTP_EXPIRES_AT)
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { it.clear() }
    }

    suspend fun savePendingOtp(
        otp: String,
        mobile: String,
        name: String,
        isSignUp: Boolean,
        expiresAtMillis: Long,
    ) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.PENDING_OTP] = otp
            prefs[Keys.PENDING_MOBILE] = mobile
            prefs[Keys.PENDING_NAME] = name
            prefs[Keys.PENDING_IS_SIGNUP] = isSignUp
            prefs[Keys.OTP_EXPIRES_AT] = expiresAtMillis.toString()
        }
    }

    suspend fun getPendingOtp(): PendingOtp? {
        val prefs = context.authDataStore.data.first()
        val otp = prefs[Keys.PENDING_OTP] ?: return null
        val mobile = prefs[Keys.PENDING_MOBILE] ?: return null
        return PendingOtp(
            otp = otp,
            mobile = mobile,
            name = prefs[Keys.PENDING_NAME].orEmpty(),
            isSignUp = prefs[Keys.PENDING_IS_SIGNUP] ?: false,
            expiresAtMillis = prefs[Keys.OTP_EXPIRES_AT]?.toLongOrNull() ?: 0L,
        )
    }

    suspend fun clearPendingOtp() {
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.PENDING_OTP)
            prefs.remove(Keys.PENDING_MOBILE)
            prefs.remove(Keys.PENDING_NAME)
            prefs.remove(Keys.PENDING_IS_SIGNUP)
            prefs.remove(Keys.OTP_EXPIRES_AT)
        }
    }
}
