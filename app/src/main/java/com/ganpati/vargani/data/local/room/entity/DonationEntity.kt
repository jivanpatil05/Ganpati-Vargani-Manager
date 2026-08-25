package com.ganpati.vargani.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity for donations.
 * [receiptNo] is uniquely indexed to enforce auto-numbering integrity offline.
 */
@Serializable
@Entity(
    tableName = "donations",
    indices = [
        Index(value = ["receipt_no"], unique = true),
        Index(value = ["name"]),
        Index(value = ["mobile"]),
        Index(value = ["collector"]),
        Index(value = ["date_epoch"]),
        Index(value = ["amount"])
    ]
)
data class DonationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "receipt_no") val receiptNo: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "mobile") val mobile: String,
    @ColumnInfo(name = "email") val email: String = "",
    @ColumnInfo(name = "address") val address: String = "",
    @ColumnInfo(name = "city") val city: String = "",
    @ColumnInfo(name = "pincode") val pincode: String = "",
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "payment_mode") val paymentMode: String,
    @ColumnInfo(name = "collector") val collector: String,
    @ColumnInfo(name = "date_epoch") val dateEpochMillis: Long,
    @ColumnInfo(name = "time_epoch") val timeEpochMillis: Long,
    @ColumnInfo(name = "notes") val notes: String = "",
    @ColumnInfo(name = "is_receipt_printed") val isReceiptPrinted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
