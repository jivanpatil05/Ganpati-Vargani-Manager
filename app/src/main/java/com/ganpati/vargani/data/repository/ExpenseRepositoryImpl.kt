package com.ganpati.vargani.data.repository

import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.data.local.room.EntityMappers.toDomain
import com.ganpati.vargani.data.local.room.EntityMappers.toEntity
import com.ganpati.vargani.data.local.room.dao.ExpenseDao
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseStats
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
) : ExpenseRepository {

    override fun observeAll(): Flow<List<Expense>> =
        expenseDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeFiltered(query: String): Flow<List<Expense>> =
        expenseDao.observeFiltered(query.trim()).map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Expense?> =
        expenseDao.observeById(id).map { it?.toDomain() }

    override fun observeStats(): Flow<ExpenseStats> {
        val now = System.currentTimeMillis()
        val start = DateTimeUtils.startOfDay(now)
        val end = start + 24L * 60L * 60L * 1000L
        return combine(
            expenseDao.observeTotal(),
            expenseDao.observeTotalBetween(start, end),
            expenseDao.observeTotalByPaymentMode(PaymentMode.CASH.name),
            expenseDao.observeTotalByPaymentMode(PaymentMode.UPI.name),
            expenseDao.observeCount(),
        ) { total, today, cash, upi, count ->
            ExpenseStats(
                totalExpenses = total,
                todayExpenses = today,
                cashTotal = cash,
                upiTotal = upi,
                count = count,
            )
        }
    }

    override suspend fun getById(id: Long): Expense? =
        expenseDao.getById(id)?.toDomain()

    override suspend fun save(expense: Expense): Long {
        require(expense.title.isNotBlank()) { "Expense title required" }
        require(expense.amount > 0.0) { "Expense amount must be positive" }
        val now = System.currentTimeMillis()
        val entity = expense.toEntity().copy(
            createdAt = if (expense.id == 0L) now else expense.createdAt,
            updatedAt = now,
        )
        return if (expense.id == 0L) {
            expenseDao.insert(entity)
        } else {
            expenseDao.update(entity)
            expense.id
        }
    }

    override suspend fun delete(id: Long) {
        expenseDao.deleteById(id)
    }
}
