package com.ganpati.vargani.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ganpati.vargani.core.constants.AppConstants
import com.ganpati.vargani.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = AppConstants.DATASTORE_NAME
)

/**
 * Lightweight preferences mirror for theme flags and receipt counter.
 * Room [SettingsEntity] is the backup-friendly source of truth for org/receipt fields;
 * both stay in sync via [SettingsRepositoryImpl].
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val RECEIPT_PREFIX = stringPreferencesKey("receipt_prefix")
        val RECEIPT_COUNTER = longPreferencesKey("receipt_counter")
        val ORG_NAME = stringPreferencesKey("organization_name")
        val ORG_ADDRESS = stringPreferencesKey("organization_address")
        val ORG_LOGO = stringPreferencesKey("organization_logo")
        val UPI_ID = stringPreferencesKey("upi_id")
        val BANK_NAME = stringPreferencesKey("bank_name")
        val ACCOUNT_NUMBER = stringPreferencesKey("account_number")
        val IFSC = stringPreferencesKey("ifsc")
        val ACCOUNT_HOLDER = stringPreferencesKey("account_holder")
        val QR_IMAGE_PATH = stringPreferencesKey("qr_image_path")
        val WHATSAPP_GROUP_NOTIFY = booleanPreferencesKey("whatsapp_group_notify")
        val VIEWERS_ENABLED = booleanPreferencesKey("viewers_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            receiptPrefix = prefs[Keys.RECEIPT_PREFIX] ?: AppConstants.DEFAULT_RECEIPT_PREFIX,
            receiptCounter = prefs[Keys.RECEIPT_COUNTER] ?: AppConstants.DEFAULT_RECEIPT_START,
            organizationName = prefs[Keys.ORG_NAME] ?: AppConstants.DEFAULT_ORG_NAME,
            organizationAddress = prefs[Keys.ORG_ADDRESS] ?: AppConstants.DEFAULT_ORG_ADDRESS,
            organizationLogoUri = prefs[Keys.ORG_LOGO].orEmpty(),
            upiId = prefs[Keys.UPI_ID].orEmpty(),
            bankName = prefs[Keys.BANK_NAME].orEmpty(),
            accountNumber = prefs[Keys.ACCOUNT_NUMBER].orEmpty(),
            ifsc = prefs[Keys.IFSC].orEmpty(),
            accountHolder = prefs[Keys.ACCOUNT_HOLDER].orEmpty(),
            qrImagePath = prefs[Keys.QR_IMAGE_PATH].orEmpty(),
            whatsappGroupNotifyEnabled = prefs[Keys.WHATSAPP_GROUP_NOTIFY] ?: true,
            viewersEnabled = prefs[Keys.VIEWERS_ENABLED] ?: true,
        )
    }

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = settings.darkMode
            prefs[Keys.DYNAMIC_COLOR] = settings.dynamicColor
            prefs[Keys.LANGUAGE_CODE] = settings.languageCode
            prefs[Keys.RECEIPT_PREFIX] = settings.receiptPrefix
            prefs[Keys.RECEIPT_COUNTER] = settings.receiptCounter
            prefs[Keys.ORG_NAME] = settings.organizationName
            prefs[Keys.ORG_ADDRESS] = settings.organizationAddress
            prefs[Keys.ORG_LOGO] = settings.organizationLogoUri
            prefs[Keys.UPI_ID] = settings.upiId
            prefs[Keys.BANK_NAME] = settings.bankName
            prefs[Keys.ACCOUNT_NUMBER] = settings.accountNumber
            prefs[Keys.IFSC] = settings.ifsc
            prefs[Keys.ACCOUNT_HOLDER] = settings.accountHolder
            prefs[Keys.QR_IMAGE_PATH] = settings.qrImagePath
            prefs[Keys.WHATSAPP_GROUP_NOTIFY] = settings.whatsappGroupNotifyEnabled
            prefs[Keys.VIEWERS_ENABLED] = settings.viewersEnabled
        }
    }

    /** Updates shared committee fields without overwriting local appearance preferences. */
    suspend fun saveSharedFields(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.RECEIPT_PREFIX] = settings.receiptPrefix
            prefs[Keys.RECEIPT_COUNTER] = settings.receiptCounter
            prefs[Keys.ORG_NAME] = settings.organizationName
            prefs[Keys.ORG_ADDRESS] = settings.organizationAddress
            prefs[Keys.ORG_LOGO] = settings.organizationLogoUri
            prefs[Keys.UPI_ID] = settings.upiId
            prefs[Keys.BANK_NAME] = settings.bankName
            prefs[Keys.ACCOUNT_NUMBER] = settings.accountNumber
            prefs[Keys.IFSC] = settings.ifsc
            prefs[Keys.ACCOUNT_HOLDER] = settings.accountHolder
            prefs[Keys.QR_IMAGE_PATH] = settings.qrImagePath
            prefs[Keys.WHATSAPP_GROUP_NOTIFY] = settings.whatsappGroupNotifyEnabled
            prefs[Keys.VIEWERS_ENABLED] = settings.viewersEnabled
        }
    }

    suspend fun saveAppearance(
        darkMode: Boolean,
        dynamicColor: Boolean,
        languageCode: String,
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = darkMode
            prefs[Keys.DYNAMIC_COLOR] = dynamicColor
            prefs[Keys.LANGUAGE_CODE] = languageCode
        }
    }
}
