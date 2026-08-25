package com.ganpati.vargani.domain.model

/**
 * Filter criteria for donor list queries.
 * Null / empty values mean "no constraint".
 */
data class DonationFilter(
    val query: String = "",
    val collector: String? = null,
    val paymentMode: PaymentMode? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val sort: DonationSort = DonationSort.LATEST
)
