package com.ganpati.vargani.presentation.settings

import com.ganpati.vargani.domain.model.AppLanguage
import com.ganpati.vargani.domain.model.AppSettings

/**
 * Settings screen state mirroring [AppSettings] with UI-only flags.
 */
data class SettingsUiState(
    val darkMode: Boolean = false,
    val dynamicColor: Boolean = false,
    val languageCode: String = AppLanguage.ENGLISH.code,
    val receiptPrefix: String = "GV",
    val receiptCounter: Long = 1L,
    val organizationName: String = "Ganpati Festival Committee",
    val organizationAddress: String = "",
    val organizationLogoUri: String = "",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val accountHolder: String = "",
    val qrImagePath: String = "",
    val whatsappGroupNotifyEnabled: Boolean = true,
    val viewersEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val showPrivacyPolicy: Boolean = false,
    val showAbout: Boolean = false,
    val showLogoutConfirm: Boolean = false,
    val appVersion: String = "",
    val isAdmin: Boolean = false,
) {
    val language: AppLanguage get() = AppLanguage.fromCode(languageCode)

    fun toAppSettings(): AppSettings = AppSettings(
        darkMode = darkMode,
        dynamicColor = dynamicColor,
        languageCode = languageCode,
        receiptPrefix = receiptPrefix,
        receiptCounter = receiptCounter,
        organizationName = organizationName,
        organizationAddress = organizationAddress,
        organizationLogoUri = organizationLogoUri,
        upiId = upiId,
        bankName = bankName,
        accountNumber = accountNumber,
        ifsc = ifsc,
        accountHolder = accountHolder,
        qrImagePath = qrImagePath,
        whatsappGroupNotifyEnabled = whatsappGroupNotifyEnabled,
        viewersEnabled = viewersEnabled,
    )

    companion object {
        fun fromAppSettings(
            settings: AppSettings,
            appVersion: String = "",
        ): SettingsUiState = SettingsUiState(
            darkMode = settings.darkMode,
            dynamicColor = settings.dynamicColor,
            languageCode = settings.languageCode,
            receiptPrefix = settings.receiptPrefix,
            receiptCounter = settings.receiptCounter,
            organizationName = settings.organizationName,
            organizationAddress = settings.organizationAddress,
            organizationLogoUri = settings.organizationLogoUri,
            upiId = settings.upiId,
            bankName = settings.bankName,
            accountNumber = settings.accountNumber,
            ifsc = settings.ifsc,
            accountHolder = settings.accountHolder,
            qrImagePath = settings.qrImagePath,
            whatsappGroupNotifyEnabled = settings.whatsappGroupNotifyEnabled,
            viewersEnabled = settings.viewersEnabled,
            appVersion = appVersion,
        )
    }
}
