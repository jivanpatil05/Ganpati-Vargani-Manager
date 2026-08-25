package com.ganpati.vargani.domain.usecase.report

import com.ganpati.vargani.core.constants.AppConstants
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.domain.model.CollectionPoint
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.model.PaymentModeStat
import com.ganpati.vargani.domain.model.ReportSummary
import com.ganpati.vargani.domain.repository.DonationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class ObserveReportSummaryUseCase @Inject constructor(
    private val repository: DonationRepository
) {
    operator fun invoke(): Flow<ReportSummary> {
        return combine(
            repository.observeAll(),
            repository.observeTopCollectors(20),
            repository.observeTopDonors(AppConstants.TOP_DONORS_LIMIT)
        ) { donations, collectors, topDonors ->
            buildSummary(donations, collectors, topDonors)
        }
    }

    suspend fun once(): ReportSummary {
        val donations = repository.observeAll().first()
        val collectors = repository.observeTopCollectors(20).first()
        val topDonors = repository.observeTopDonors(AppConstants.TOP_DONORS_LIMIT).first()
        return buildSummary(donations, collectors, topDonors)
    }

    private fun buildSummary(
        donations: List<Donation>,
        collectors: List<com.ganpati.vargani.domain.model.CollectorStat>,
        topDonors: List<Donation>
    ): ReportSummary {
        val total = donations.sumOf { it.amount }
        val avg = if (donations.isEmpty()) 0.0 else total / donations.size
        val highest = donations.maxOfOrNull { it.amount } ?: 0.0
        val lowest = donations.minOfOrNull { it.amount } ?: 0.0
        val cash = donations.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.amount }
        val upi = donations.filter { it.paymentMode == PaymentMode.UPI }.sumOf { it.amount }

        return ReportSummary(
            totalCollection = total,
            averageDonation = avg,
            highestDonation = highest,
            lowestDonation = lowest,
            totalDonors = donations.size,
            cashTotal = cash,
            upiTotal = upi,
            daily = groupByDay(donations, 14),
            weekly = groupByWeek(donations, 8),
            monthly = groupByMonth(donations, 12),
            yearly = groupByYear(donations),
            collectors = collectors,
            paymentModes = listOf(
                PaymentModeStat(PaymentMode.CASH, cash, donations.count { it.paymentMode == PaymentMode.CASH }),
                PaymentModeStat(PaymentMode.UPI, upi, donations.count { it.paymentMode == PaymentMode.UPI })
            ),
            topDonors = topDonors,
            trend = groupByDay(donations, AppConstants.TREND_DAYS)
        )
    }

    private fun groupByDay(donations: List<Donation>, days: Int): List<CollectionPoint> {
        val cal = Calendar.getInstance(Locale("en", "IN"))
        return (days - 1 downTo 0).map { offset ->
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val start = DateTimeUtils.startOfDay(cal.timeInMillis)
            val end = DateTimeUtils.endOfDay(cal.timeInMillis)
            val amount = donations.filter { it.dateEpochMillis in start..end }.sumOf { it.amount }
            CollectionPoint(
                label = DateTimeUtils.formatDate(start, "dd MMM"),
                amount = amount,
                epochMillis = start
            )
        }
    }

    private fun groupByWeek(donations: List<Donation>, weeks: Int): List<CollectionPoint> {
        val cal = Calendar.getInstance(Locale("en", "IN"))
        return (weeks - 1 downTo 0).map { offset ->
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.WEEK_OF_YEAR, -offset)
            val start = DateTimeUtils.startOfWeek(cal.timeInMillis)
            cal.timeInMillis = start
            cal.add(Calendar.DAY_OF_YEAR, 6)
            val end = DateTimeUtils.endOfDay(cal.timeInMillis)
            val amount = donations.filter { it.dateEpochMillis in start..end }.sumOf { it.amount }
            CollectionPoint(label = "W${weeks - offset}", amount = amount, epochMillis = start)
        }
    }

    private fun groupByMonth(donations: List<Donation>, months: Int): List<CollectionPoint> {
        val cal = Calendar.getInstance(Locale("en", "IN"))
        return (months - 1 downTo 0).map { offset ->
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.MONTH, -offset)
            val start = DateTimeUtils.startOfMonth(cal.timeInMillis)
            cal.timeInMillis = start
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.MILLISECOND, -1)
            val end = cal.timeInMillis
            val amount = donations.filter { it.dateEpochMillis in start..end }.sumOf { it.amount }
            CollectionPoint(
                label = DateTimeUtils.formatDate(start, "MMM yy"),
                amount = amount,
                epochMillis = start
            )
        }
    }

    private fun groupByYear(donations: List<Donation>): List<CollectionPoint> {
        return donations.groupBy {
            Calendar.getInstance(Locale("en", "IN")).apply { timeInMillis = it.dateEpochMillis }
                .get(Calendar.YEAR)
        }.toSortedMap().map { (year, list) ->
            CollectionPoint(label = year.toString(), amount = list.sumOf { it.amount })
        }
    }
}
