package com.ganpati.vargani.domain.model

/**
 * Aggregated dashboard metrics computed from local donations.
 */
data class DashboardStats(
    val totalCollection: Double = 0.0,
    val todayCollection: Double = 0.0,
    val weeklyCollection: Double = 0.0,
    val monthlyCollection: Double = 0.0,
    val totalDonors: Int = 0,
    val averageDonation: Double = 0.0,
    val highestDonation: Double = 0.0,
    val cashCollection: Double = 0.0,
    val upiCollection: Double = 0.0,
    val pendingReceipts: Int = 0
)
