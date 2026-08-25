package com.ganpati.vargani.domain.repository

import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseStats
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeAll(): Flow<List<Expense>>
    fun observeFiltered(query: String): Flow<List<Expense>>
    fun observeById(id: Long): Flow<Expense?>
    fun observeStats(): Flow<ExpenseStats>
    suspend fun getById(id: Long): Expense?
    suspend fun save(expense: Expense): Long
    suspend fun delete(id: Long)
}
