package com.ganpati.vargani.presentation.dashboard

import com.ganpati.vargani.domain.model.CollectorStat
import com.ganpati.vargani.domain.model.DashboardStats
import com.ganpati.vargani.domain.model.Donation

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val recent: List<Donation> = emptyList(),
    val collectors: List<CollectorStat> = emptyList(),
    val totalOutgoing: Double = 0.0,
    val canWrite: Boolean = true,
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true,
) {
    val remainingBalance: Double
        get() = stats.totalCollection - totalOutgoing
}
