package com.ganpati.vargani.core.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Currency
import com.ganpati.vargani.core.constants.AppConstants

/**
 * Date / currency helpers shared across UI and export layers.
 */
object DateTimeUtils {
    private val locale: Locale = Locale("en", "IN")

    fun formatDate(epochMillis: Long, pattern: String = AppConstants.DATE_PATTERN): String =
        SimpleDateFormat(pattern, locale).format(Date(epochMillis))

    fun formatTime(epochMillis: Long, pattern: String = AppConstants.TIME_PATTERN): String =
        SimpleDateFormat(pattern, locale).format(Date(epochMillis))

    fun formatDateTime(epochMillis: Long): String =
        SimpleDateFormat(AppConstants.DATE_TIME_PATTERN, locale).format(Date(epochMillis))

    fun startOfDay(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(locale).apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(locale).apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun startOfWeek(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(locale).apply {
            timeInMillis = epochMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        return startOfDay(cal.timeInMillis)
    }

    fun startOfMonth(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(locale).apply {
            timeInMillis = epochMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return startOfDay(cal.timeInMillis)
    }

    fun startOfYear(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(locale).apply {
            timeInMillis = epochMillis
            set(Calendar.DAY_OF_YEAR, 1)
        }
        return startOfDay(cal.timeInMillis)
    }

    fun daysAgo(days: Int): Long {
        val cal = Calendar.getInstance(locale).apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }
        return startOfDay(cal.timeInMillis)
    }

    fun exportTimestamp(): String =
        SimpleDateFormat(AppConstants.EXPORT_TIMESTAMP_PATTERN, locale)
            .format(Date())
}

object CurrencyUtils {
    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = Currency.getInstance("INR")
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun format(amount: Double): String = formatter.format(amount)

    fun formatCompact(amount: Double): String = "₹${"%.2f".format(Locale.US, amount)}"
}
