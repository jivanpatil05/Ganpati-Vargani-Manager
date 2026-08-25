package com.ganpati.vargani.presentation.profile

/**
 * Single row in the Profile amount breakdown (incoming donation or outgoing expense).
 */
enum class BreakdownType {
    Incoming,
    Outgoing,
}

data class BreakdownItem(
    val id: Long,
    val type: BreakdownType,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val dateEpochMillis: Long,
)
