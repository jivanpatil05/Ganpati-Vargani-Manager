package com.ganpati.vargani.presentation.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.usecase.donation.ObserveDashboardStatsUseCase
import com.ganpati.vargani.domain.usecase.donation.ObserveRecentDonationsUseCase
import com.ganpati.vargani.domain.usecase.donation.ObserveTopCollectorsUseCase
import com.ganpati.vargani.domain.usecase.expense.ObserveExpenseStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboardStats: ObserveDashboardStatsUseCase,
    observeRecentDonations: ObserveRecentDonationsUseCase,
    observeTopCollectors: ObserveTopCollectorsUseCase,
    observeExpenseStats: ObserveExpenseStatsUseCase,
    authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _section = MutableStateFlow(restoreSection())
    val section: StateFlow<HomeSection> = _section.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        observeDashboardStats(),
        observeRecentDonations(),
        observeTopCollectors(),
        observeExpenseStats(),
        authRepository.observeSession(),
    ) { stats, recent, collectors, expenseStats, session ->
        DashboardUiState(
            stats = stats,
            recent = recent,
            collectors = collectors,
            totalOutgoing = expenseStats.totalExpenses,
            canWrite = session.canWrite,
            isAdmin = session.isAdmin,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = DashboardUiState(isLoading = true),
    )

    fun selectSection(section: HomeSection) {
        _section.value = section
        savedStateHandle[KEY_HOME_SECTION] = section.name
    }

    private fun restoreSection(): HomeSection {
        val saved = savedStateHandle.get<String>(KEY_HOME_SECTION) ?: return HomeSection.Incoming
        // Legacy Balance/History tabs map back to Incoming.
        if (saved == "Balance" || saved == "History") return HomeSection.Incoming
        return HomeSection.entries.find { it.name == saved } ?: HomeSection.Incoming
    }

    companion object {
        private const val KEY_HOME_SECTION = "home_section"
    }
}
