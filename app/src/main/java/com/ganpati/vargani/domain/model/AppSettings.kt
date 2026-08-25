package com.ganpati.vargani.domain.model

/**
 * User-configurable application settings persisted via DataStore / Room settings table.
 */
data class AppSettings(
    val darkMode: Boolean = false,
    val dynamicColor: Boolean = false,
    val languageCode: String = "en",
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
    /** When true, opens WhatsApp with a ready message after new donation/expense. */
    val whatsappGroupNotifyEnabled: Boolean = true,
    /** When false, Viewer accounts cannot access committee data (admin master switch). */
    val viewersEnabled: Boolean = true,
)
