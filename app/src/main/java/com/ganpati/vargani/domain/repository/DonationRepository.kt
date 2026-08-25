package com.ganpati.vargani.domain.repository

import com.ganpati.vargani.domain.model.CollectorStat
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.DonationFilter
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over donation persistence.
 * Implementations may later add remote sync without changing use cases.
 */
interface DonationRepository {
    fun observeAll(): Flow<List<Donation>>
    fun observeRecent(limit: Int): Flow<List<Donation>>
    fun observeFiltered(filter: DonationFilter): Flow<List<Donation>>
    fun observeById(id: Long): Flow<Donation?>
    fun observeTopCollectors(limit: Int): Flow<List<CollectorStat>>
    fun observeTopDonors(limit: Int): Flow<List<Donation>>
    fun observeCollectors(): Flow<List<String>>
    fun observeTotalCollection(): Flow<Double>
    fun observeCollectionBetween(start: Long, end: Long): Flow<Double>
    fun observeDonorCount(): Flow<Int>
    fun observeAverageDonation(): Flow<Double>
    fun observeHighestDonation(): Flow<Double>
    fun observeCashCollection(): Flow<Double>
    fun observeUpiCollection(): Flow<Double>
    fun observePendingReceipts(): Flow<Int>

    suspend fun getById(id: Long): Donation?
    suspend fun getLowestDonation(): Double
    suspend fun insert(donation: Donation): Long
    suspend fun update(donation: Donation)
    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun isReceiptUnique(receiptNo: String, excludeId: Long = -1L): Boolean
    suspend fun markReceiptPrinted(id: Long)
}
