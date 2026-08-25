package com.ganpati.vargani.domain.model

/**
 * Chart / report helper models used by the presentation layer.
 */
data class CollectionPoint(
    val label: String,
    val amount: Double,
    val epochMillis: Long = 0L
)

data class CollectorStat(
    val collector: String,
    val totalAmount: Double,
    val donationCount: Int
)

data class PaymentModeStat(
    val paymentMode: PaymentMode,
    val totalAmount: Double,
    val donationCount: Int
)

data class ReportSummary(
    val totalCollection: Double,
    val averageDonation: Double,
    val highestDonation: Double,
    val lowestDonation: Double,
    val totalDonors: Int,
    val cashTotal: Double,
    val upiTotal: Double,
    val daily: List<CollectionPoint>,
    val weekly: List<CollectionPoint>,
    val monthly: List<CollectionPoint>,
    val yearly: List<CollectionPoint>,
    val collectors: List<CollectorStat>,
    val paymentModes: List<PaymentModeStat>,
    val topDonors: List<Donation>,
    val trend: List<CollectionPoint>
)
