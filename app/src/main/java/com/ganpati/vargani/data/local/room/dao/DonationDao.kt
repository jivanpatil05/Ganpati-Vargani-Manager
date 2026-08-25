package com.ganpati.vargani.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ganpati.vargani.data.local.room.entity.DonationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for donation records.
 * Heavy aggregation stays in SQL for performance on large offline datasets.
 */
@Dao
interface DonationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(donation: DonationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(donations: List<DonationEntity>)

    @Update
    suspend fun update(donation: DonationEntity): Int

    @Query("DELETE FROM donations WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM donations")
    suspend fun deleteAll()

    @Query("SELECT * FROM donations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DonationEntity?

    @Query("SELECT * FROM donations WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<DonationEntity?>

    @Query("SELECT * FROM donations ORDER BY date_epoch DESC, time_epoch DESC, id DESC")
    fun observeAll(): Flow<List<DonationEntity>>

    @Query("SELECT COUNT(*) FROM donations WHERE receipt_no = :receiptNo AND id != :excludeId")
    suspend fun countByReceiptNo(receiptNo: String, excludeId: Long = -1L): Int

    @Query("SELECT * FROM donations ORDER BY date_epoch DESC, time_epoch DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DonationEntity>>

    @Query(
        """
        SELECT * FROM donations
        WHERE (:query = '' OR
              name LIKE '%' || :query || '%' COLLATE NOCASE OR
              receipt_no LIKE '%' || :query || '%' COLLATE NOCASE OR
              mobile LIKE '%' || :query || '%' OR
              collector LIKE '%' || :query || '%' COLLATE NOCASE OR
              CAST(amount AS TEXT) LIKE '%' || :query || '%')
        AND (:collector IS NULL OR collector = :collector)
        AND (:paymentMode IS NULL OR payment_mode = :paymentMode)
        AND (:startDate IS NULL OR date_epoch >= :startDate)
        AND (:endDate IS NULL OR date_epoch <= :endDate)
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        ORDER BY
            CASE WHEN :sort = 'LATEST' THEN date_epoch END DESC,
            CASE WHEN :sort = 'LATEST' THEN time_epoch END DESC,
            CASE WHEN :sort = 'OLDEST' THEN date_epoch END ASC,
            CASE WHEN :sort = 'OLDEST' THEN time_epoch END ASC,
            CASE WHEN :sort = 'HIGHEST_AMOUNT' THEN amount END DESC,
            CASE WHEN :sort = 'LOWEST_AMOUNT' THEN amount END ASC,
            CASE WHEN :sort = 'ALPHABETICAL' THEN name END COLLATE NOCASE ASC,
            id DESC
        """
    )
    fun observeFiltered(
        query: String,
        collector: String?,
        paymentMode: String?,
        startDate: Long?,
        endDate: Long?,
        minAmount: Double?,
        maxAmount: Double?,
        sort: String
    ): Flow<List<DonationEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM donations")
    fun observeTotalCollection(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM donations WHERE date_epoch >= :start AND date_epoch <= :end")
    fun observeCollectionBetween(start: Long, end: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM donations")
    fun observeDonorCount(): Flow<Int>

    @Query("SELECT COALESCE(AVG(amount), 0) FROM donations")
    fun observeAverageDonation(): Flow<Double>

    @Query("SELECT COALESCE(MAX(amount), 0) FROM donations")
    fun observeHighestDonation(): Flow<Double>

    @Query("SELECT COALESCE(MIN(amount), 0) FROM donations")
    suspend fun getLowestDonation(): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM donations WHERE payment_mode = :mode")
    fun observeCollectionByMode(mode: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM donations WHERE is_receipt_printed = 0")
    fun observePendingReceipts(): Flow<Int>

    @Query(
        """
        SELECT collector AS collector, SUM(amount) AS totalAmount, COUNT(*) AS donationCount
        FROM donations
        GROUP BY collector
        ORDER BY totalAmount DESC
        LIMIT :limit
        """
    )
    fun observeTopCollectors(limit: Int): Flow<List<CollectorAggregate>>

    @Query("SELECT DISTINCT collector FROM donations ORDER BY collector COLLATE NOCASE ASC")
    fun observeCollectors(): Flow<List<String>>

    @Query("SELECT * FROM donations ORDER BY amount DESC LIMIT :limit")
    fun observeTopDonors(limit: Int): Flow<List<DonationEntity>>

    @Query("UPDATE donations SET is_receipt_printed = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markReceiptPrinted(id: Long, updatedAt: Long): Int
}

/**
 * Projection for collector aggregates (Room POJO).
 */
data class CollectorAggregate(
    val collector: String,
    val totalAmount: Double,
    val donationCount: Int
)
