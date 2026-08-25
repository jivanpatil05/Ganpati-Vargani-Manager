package com.ganpati.vargani.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ganpati.vargani.data.local.room.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity): Int

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses ORDER BY date_epoch DESC, time_epoch DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE (:query = '' OR
              title LIKE '%' || :query || '%' COLLATE NOCASE OR
              category LIKE '%' || :query || '%' COLLATE NOCASE OR
              paid_by LIKE '%' || :query || '%' COLLATE NOCASE OR
              notes LIKE '%' || :query || '%' COLLATE NOCASE OR
              CAST(amount AS TEXT) LIKE '%' || :query || '%')
        ORDER BY date_epoch DESC, time_epoch DESC, id DESC
        """
    )
    fun observeFiltered(query: String): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses")
    fun observeTotal(): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE date_epoch >= :startOfDay AND date_epoch < :endOfDay
        """
    )
    fun observeTotalBetween(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE payment_mode = :mode")
    fun observeTotalByPaymentMode(mode: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM expenses")
    fun observeCount(): Flow<Int>
}
