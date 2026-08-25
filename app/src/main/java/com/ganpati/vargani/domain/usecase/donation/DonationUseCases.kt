package com.ganpati.vargani.domain.usecase.donation

import com.ganpati.vargani.core.constants.AppConstants
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.domain.model.CollectorStat
import com.ganpati.vargani.domain.model.DashboardStats
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.DonationFilter
import com.ganpati.vargani.domain.repository.DonationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveDashboardStatsUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    operator fun invoke(): Flow<DashboardStats> {
        val todayStart = DateTimeUtils.startOfDay()
        val todayEnd = DateTimeUtils.endOfDay()
        val weekStart = DateTimeUtils.startOfWeek()
        val monthStart = DateTimeUtils.startOfMonth()

        val totals = combine(
            repository.observeTotalCollection(),
            repository.observeCollectionBetween(todayStart, todayEnd),
            repository.observeCollectionBetween(weekStart, todayEnd),
            repository.observeCollectionBetween(monthStart, todayEnd),
            repository.observeDonorCount()
        ) { total, today, week, month, donors ->
            Totals(total, today, week, month, donors)
        }

        val extras = combine(
            repository.observeAverageDonation(),
            repository.observeHighestDonation(),
            repository.observeCashCollection(),
            repository.observeUpiCollection(),
            repository.observePendingReceipts()
        ) { avg, highest, cash, upi, pending ->
            Extras(avg, highest, cash, upi, pending)
        }

        return combine(totals, extras) { t, e ->
            DashboardStats(
                totalCollection = t.total,
                todayCollection = t.today,
                weeklyCollection = t.week,
                monthlyCollection = t.month,
                totalDonors = t.donors,
                averageDonation = e.avg,
                highestDonation = e.highest,
                cashCollection = e.cash,
                upiCollection = e.upi,
                pendingReceipts = e.pending
            )
        }
    }

    private data class Totals(
        val total: Double,
        val today: Double,
        val week: Double,
        val month: Double,
        val donors: Int
    )

    private data class Extras(
        val avg: Double,
        val highest: Double,
        val cash: Double,
        val upi: Double,
        val pending: Int
    )
}

class ObserveRecentDonationsUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    operator fun invoke(limit: Int = AppConstants.RECENT_DONATIONS_LIMIT): Flow<List<Donation>> =
        repository.observeRecent(limit)
}

class ObserveTopCollectorsUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    operator fun invoke(limit: Int = AppConstants.TOP_COLLECTORS_LIMIT): Flow<List<CollectorStat>> =
        repository.observeTopCollectors(limit)
}

class ObserveDonationsUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    operator fun invoke(filter: DonationFilter): Flow<List<Donation>> =
        repository.observeFiltered(filter)
}

class ObserveDonationUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    operator fun invoke(id: Long): Flow<Donation?> = repository.observeById(id)
}

class ObserveCollectorsUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    operator fun invoke(): Flow<List<String>> = repository.observeCollectors()
}

class GetDonationUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    suspend operator fun invoke(id: Long): Donation? = repository.getById(id)
}

class SaveDonationUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    suspend operator fun invoke(donation: Donation): Long {
        require(donation.name.isNotBlank()) { "Name required" }
        require(donation.amount > 0) { "Amount must be positive" }
        require(repository.isReceiptUnique(donation.receiptNo, donation.id)) {
            "Receipt not unique"
        }
        return if (donation.id == 0L) {
            repository.insert(
                donation.copy(
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            repository.update(donation)
            donation.id
        }
    }
}

class DeleteDonationUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}

class MarkReceiptPrintedUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    suspend operator fun invoke(id: Long) = repository.markReceiptPrinted(id)
}

class RestoreDonationUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    suspend operator fun invoke(donation: Donation): Long =
        repository.insert(donation.copy(id = 0L))
}
