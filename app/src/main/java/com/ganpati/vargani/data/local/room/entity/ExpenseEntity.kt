package com.ganpati.vargani.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["title"]),
        Index(value = ["category"]),
        Index(value = ["date_epoch"]),
        Index(value = ["amount"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "payment_mode") val paymentMode: String,
    @ColumnInfo(name = "paid_by") val paidBy: String,
    @ColumnInfo(name = "date_epoch") val dateEpochMillis: Long,
    @ColumnInfo(name = "time_epoch") val timeEpochMillis: Long,
    @ColumnInfo(name = "notes") val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
