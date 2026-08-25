package com.ganpati.vargani.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Singleton settings row (id is always 1).
 * Complements DataStore; Room copy is included in DB backup/restore.
 */
@Serializable
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "dark_mode") val darkMode: Boolean = false,
    @ColumnInfo(name = "dynamic_color") val dynamicColor: Boolean = false,
    @ColumnInfo(name = "language_code") val languageCode: String = "en",
    @ColumnInfo(name = "receipt_prefix") val receiptPrefix: String = "GV",
    @ColumnInfo(name = "receipt_counter") val receiptCounter: Long = 1L,
    @ColumnInfo(name = "organization_name") val organizationName: String = "Ganpati Festival Committee",
    @ColumnInfo(name = "organization_address") val organizationAddress: String = "",
    @ColumnInfo(name = "organization_logo") val organizationLogo: String = "",
    @ColumnInfo(name = "upi_id") val upiId: String = "",
    @ColumnInfo(name = "bank_name") val bankName: String = "",
    @ColumnInfo(name = "account_number") val accountNumber: String = "",
    @ColumnInfo(name = "ifsc") val ifsc: String = "",
    @ColumnInfo(name = "account_holder") val accountHolder: String = "",
    @ColumnInfo(name = "qr_image_path") val qrImagePath: String = "",
    @ColumnInfo(name = "whatsapp_group_notify") val whatsappGroupNotifyEnabled: Boolean = true,
)
