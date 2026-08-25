package com.ganpati.vargani.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.domain.model.DonationFilter
import com.ganpati.vargani.domain.usecase.donation.ObserveDashboardStatsUseCase
import com.ganpati.vargani.domain.usecase.donation.ObserveDonationsUseCase
import com.ganpati.vargani.domain.usecase.expense.ObserveExpenseStatsUseCase
import com.ganpati.vargani.domain.usecase.expense.ObserveExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AmountBreakdownUiState(
    val items: List<BreakdownItem> = emptyList(),
    val totalIncoming: Double = 0.0,
    val totalOutgoing: Double = 0.0,
    val remaining: Double = 0.0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class AmountBreakdownViewModel @Inject constructor(
    observeDashboardStats: ObserveDashboardStatsUseCase,
    observeExpenseStats: ObserveExpenseStatsUseCase,
    observeDonations: ObserveDonationsUseCase,
    observeExpenses: ObserveExpensesUseCase,
) : ViewModel() {

    val uiState: StateFlow<AmountBreakdownUiState> = combine(
        observeDashboardStats(),
        observeExpenseStats(),
        observeDonations(DonationFilter()),
        observeExpenses(""),
    ) { donationStats, expenseStats, donations, expenses ->
        val incoming = donationStats.totalCollection
        val outgoing = expenseStats.totalExpenses
        AmountBreakdownUiState(
            items = buildBreakdownItems(donations, expenses),
            totalIncoming = incoming,
            totalOutgoing = outgoing,
            remaining = incoming - outgoing,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AmountBreakdownUiState(),
    )
}
