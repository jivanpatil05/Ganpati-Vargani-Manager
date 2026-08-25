package com.ganpati.vargani.data.repository

import com.ganpati.vargani.data.local.room.EntityMappers.toDomain
import com.ganpati.vargani.data.local.room.EntityMappers.toEntity
import com.ganpati.vargani.data.local.room.dao.DonationDao
import com.ganpati.vargani.domain.model.CollectorStat
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.DonationFilter
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.DonationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DonationRepositoryImpl @Inject constructor(
    private val donationDao: DonationDao
) : DonationRepository {

    override fun observeAll(): Flow<List<Donation>> =
        donationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeRecent(limit: Int): Flow<List<Donation>> =
        donationDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeFiltered(filter: DonationFilter): Flow<List<Donation>> =
        donationDao.observeFiltered(
            query = filter.query.trim(),
            collector = filter.collector,
            paymentMode = filter.paymentMode?.name,
            startDate = filter.startDateMillis,
            endDate = filter.endDateMillis,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            sort = filter.sort.name
        ).map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Donation?> =
        donationDao.observeById(id).map { it?.toDomain() }

    override fun observeTopCollectors(limit: Int): Flow<List<CollectorStat>> =
        donationDao.observeTopCollectors(limit).map { rows ->
            rows.map {
                CollectorStat(
                    collector = it.collector,
                    totalAmount = it.totalAmount,
                    donationCount = it.donationCount
                )
            }
        }

    override fun observeTopDonors(limit: Int): Flow<List<Donation>> =
        donationDao.observeTopDonors(limit).map { list -> list.map { it.toDomain() } }

    override fun observeCollectors(): Flow<List<String>> = donationDao.observeCollectors()

    override fun observeTotalCollection(): Flow<Double> = donationDao.observeTotalCollection()

    override fun observeCollectionBetween(start: Long, end: Long): Flow<Double> =
        donationDao.observeCollectionBetween(start, end)

    override fun observeDonorCount(): Flow<Int> = donationDao.observeDonorCount()

    override fun observeAverageDonation(): Flow<Double> = donationDao.observeAverageDonation()

    override fun observeHighestDonation(): Flow<Double> = donationDao.observeHighestDonation()

    override fun observeCashCollection(): Flow<Double> =
        donationDao.observeCollectionByMode(PaymentMode.CASH.name)

    override fun observeUpiCollection(): Flow<Double> =
        donationDao.observeCollectionByMode(PaymentMode.UPI.name)

    override fun observePendingReceipts(): Flow<Int> = donationDao.observePendingReceipts()

    override suspend fun getById(id: Long): Donation? = donationDao.getById(id)?.toDomain()

    override suspend fun getLowestDonation(): Double = donationDao.getLowestDonation()

    override suspend fun insert(donation: Donation): Long =
        donationDao.insert(donation.toEntity())

    override suspend fun update(donation: Donation) {
        donationDao.update(donation.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) {
        donationDao.deleteById(id)
    }

    override suspend fun deleteAll() {
        donationDao.deleteAll()
    }

    override suspend fun isReceiptUnique(receiptNo: String, excludeId: Long): Boolean =
        donationDao.countByReceiptNo(receiptNo, excludeId) == 0

    override suspend fun markReceiptPrinted(id: Long) {
        donationDao.markReceiptPrinted(id, System.currentTimeMillis())
    }
}
