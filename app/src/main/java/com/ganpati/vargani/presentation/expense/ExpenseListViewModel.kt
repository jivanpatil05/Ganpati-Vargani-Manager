package com.ganpati.vargani.presentation.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseStats
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.usecase.expense.DeleteExpenseUseCase
import com.ganpati.vargani.domain.usecase.expense.ObserveExpenseStatsUseCase
import com.ganpati.vargani.domain.usecase.expense.ObserveExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseListUiState(
    val expenses: List<Expense> = emptyList(),
    val stats: ExpenseStats = ExpenseStats(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val canWrite: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    observeExpenses: ObserveExpensesUseCase,
    observeExpenseStats: ObserveExpenseStatsUseCase,
    authRepository: AuthRepository,
    private val deleteExpense: DeleteExpenseUseCase,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ExpenseListUiState> = combine(
        searchQuery.flatMapLatest { query -> observeExpenses(query) },
        observeExpenseStats(),
        searchQuery,
        authRepository.observeSession(),
    ) { expenses, stats, query, session ->
        ExpenseListUiState(
            expenses = expenses,
            stats = stats,
            searchQuery = query,
            isLoading = false,
            canWrite = session.canWrite,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpenseListUiState(),
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun delete(id: Long) {
        if (!uiState.value.canWrite) return
        viewModelScope.launch { deleteExpense(id) }
    }
}
