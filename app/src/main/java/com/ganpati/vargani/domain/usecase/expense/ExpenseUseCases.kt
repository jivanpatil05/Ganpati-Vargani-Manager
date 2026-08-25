package com.ganpati.vargani.domain.usecase.expense

import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseStats
import com.ganpati.vargani.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(query: String = ""): Flow<List<Expense>> =
        if (query.isBlank()) repository.observeAll() else repository.observeFiltered(query)
}

class ObserveExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(id: Long): Flow<Expense?> = repository.observeById(id)
}

class ObserveExpenseStatsUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(): Flow<ExpenseStats> = repository.observeStats()
}

class GetExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(id: Long): Expense? = repository.getById(id)
}

class SaveExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(expense: Expense): Long = repository.save(expense)
}

class DeleteExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
