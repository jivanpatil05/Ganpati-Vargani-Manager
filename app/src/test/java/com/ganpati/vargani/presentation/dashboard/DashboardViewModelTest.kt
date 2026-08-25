package com.ganpati.vargani.presentation.dashboard

import app.cash.turbine.test
import com.ganpati.vargani.domain.model.CollectorStat
import com.ganpati.vargani.domain.model.DashboardStats
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.ExpenseStats
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.model.UserRole
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.repository.AuthSessionState
import com.ganpati.vargani.domain.usecase.donation.ObserveDashboardStatsUseCase
import com.ganpati.vargani.domain.usecase.donation.ObserveRecentDonationsUseCase
import com.ganpati.vargani.domain.usecase.donation.ObserveTopCollectorsUseCase
import com.ganpati.vargani.domain.usecase.expense.ObserveExpenseStatsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var observeDashboardStats: ObserveDashboardStatsUseCase
    private lateinit var observeRecentDonations: ObserveRecentDonationsUseCase
    private lateinit var observeTopCollectors: ObserveTopCollectorsUseCase
    private lateinit var observeExpenseStats: ObserveExpenseStatsUseCase
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        observeDashboardStats = mockk()
        observeRecentDonations = mockk()
        observeTopCollectors = mockk()
        observeExpenseStats = mockk()
        authRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_combinesDomainFlows() = runTest {
        val stats = DashboardStats(
            totalCollection = 10_000.0,
            todayCollection = 500.0,
            totalDonors = 25,
        )
        val recent = listOf(
            Donation(
                id = 1L,
                receiptNo = "GV-0001",
                name = "Donor",
                mobile = "9876543210",
                amount = 100.0,
                paymentMode = PaymentMode.CASH,
                collector = "A",
                dateEpochMillis = 0L,
                timeEpochMillis = 0L,
            ),
        )
        val collectors = listOf(
            CollectorStat(collector = "A", totalAmount = 5000.0, donationCount = 10),
        )
        val expenseStats = ExpenseStats(totalExpenses = 2_500.0)
        val session = AuthSessionState(
            isLoggedIn = true,
            uid = "admin-1",
            name = "Admin",
            email = "admin@example.com",
            role = UserRole.ADMIN,
            committeeId = "committee-1",
        )

        every { observeDashboardStats() } returns flowOf(stats)
        every { observeRecentDonations() } returns flowOf(recent)
        every { observeTopCollectors() } returns flowOf(collectors)
        every { observeExpenseStats() } returns flowOf(expenseStats)
        every { authRepository.observeSession() } returns flowOf(session)

        val viewModel = DashboardViewModel(
            observeDashboardStats = observeDashboardStats,
            observeRecentDonations = observeRecentDonations,
            observeTopCollectors = observeTopCollectors,
            observeExpenseStats = observeExpenseStats,
            authRepository = authRepository,
            savedStateHandle = androidx.lifecycle.SavedStateHandle(),
        )

        viewModel.uiState.test {
            val loading = awaitItem()
            assertThat(loading.isLoading).isTrue()

            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.stats.totalCollection).isEqualTo(10_000.0)
            assertThat(state.recent).hasSize(1)
            assertThat(state.collectors).hasSize(1)
            assertThat(state.totalOutgoing).isEqualTo(2_500.0)
            assertThat(state.remainingBalance).isEqualTo(7_500.0)
            assertThat(state.canWrite).isTrue()
            assertThat(state.isAdmin).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
