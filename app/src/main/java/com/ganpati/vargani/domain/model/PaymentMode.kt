package com.ganpati.vargani.domain.model

/**
 * Supported offline payment modes for a donation (vargani).
 */
enum class PaymentMode {
    CASH,
    UPI;

    companion object {
        fun fromStorage(value: String): PaymentMode =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: CASH
    }
}
