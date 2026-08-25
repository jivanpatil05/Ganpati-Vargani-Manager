package com.ganpati.vargani.domain.model

/**
 * Outgoing expense (vargani spend) record.
 */
data class Expense(
    val id: Long = 0L,
    val title: String,
    val category: ExpenseCategory = ExpenseCategory.MISC,
    val amount: Double,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val paidBy: String,
    val dateEpochMillis: Long,
    val timeEpochMillis: Long,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class ExpenseStats(
    val totalExpenses: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val cashTotal: Double = 0.0,
    val upiTotal: Double = 0.0,
    val count: Int = 0,
)
