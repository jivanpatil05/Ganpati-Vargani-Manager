package com.ganpati.vargani.domain.model

/**
 * Domain model for a single donation (vargani) record.
 * Kept immutable so UI state remains predictable and thread-safe.
 */
data class Donation(
    val id: Long = 0L,
    val receiptNo: String,
    val name: String,
    val mobile: String,
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val pincode: String = "",
    val amount: Double,
    val paymentMode: PaymentMode,
    val collector: String,
    val dateEpochMillis: Long,
    val timeEpochMillis: Long,
    val notes: String = "",
    val isReceiptPrinted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
