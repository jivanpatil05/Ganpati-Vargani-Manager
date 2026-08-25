package com.ganpati.vargani.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.ganpati.vargani.R
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseCategory
import com.ganpati.vargani.domain.model.PaymentMode

/**
 * Opens WhatsApp with a pre-filled donation/expense message so the user can
 * pick their committee group and tap Send.
 *
 * WhatsApp does not allow silent auto-posting into a group from third-party apps
 * without the Business Cloud API; this is the supported on-device approach.
 */
object WhatsAppGroupNotifyHelper {

    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    sealed class ShareResult {
        data object Success : ShareResult()
        data object WhatsAppNotInstalled : ShareResult()
        data object Failed : ShareResult()
    }

    fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return isPackageInstalled(pm, WHATSAPP) || isPackageInstalled(pm, WHATSAPP_BUSINESS)
    }

    fun shareDonation(context: Context, donation: Donation, orgName: String): ShareResult {
        val payment = paymentLabel(context, donation.paymentMode)
        val message = buildString {
            appendLine("🪔 ${context.getString(R.string.wa_new_donation_title)}")
            if (orgName.isNotBlank()) appendLine(orgName)
            appendLine()
            appendLine("${context.getString(R.string.receipt_number)}: ${donation.receiptNo}")
            appendLine("${context.getString(R.string.donor_name)}: ${donation.name}")
            if (donation.mobile.isNotBlank()) {
                appendLine("${context.getString(R.string.mobile_number)}: ${donation.mobile}")
            }
            appendLine("${context.getString(R.string.amount)}: ${CurrencyUtils.format(donation.amount)}")
            appendLine("${context.getString(R.string.payment_mode)}: $payment")
            appendLine("${context.getString(R.string.collector_name)}: ${donation.collector}")
            appendLine(
                "${context.getString(R.string.date)}: ${DateTimeUtils.formatDate(donation.dateEpochMillis)}",
            )
            if (donation.notes.isNotBlank()) {
                appendLine("${context.getString(R.string.notes)}: ${donation.notes}")
            }
            appendLine()
            append(context.getString(R.string.wa_message_footer, context.getString(R.string.app_name)))
        }
        return shareText(context, message)
    }

    fun shareExpense(context: Context, expense: Expense, orgName: String): ShareResult {
        val payment = paymentLabel(context, expense.paymentMode)
        val category = categoryLabel(context, expense.category)
        val message = buildString {
            appendLine("📤 ${context.getString(R.string.wa_new_expense_title)}")
            if (orgName.isNotBlank()) appendLine(orgName)
            appendLine()
            appendLine("${context.getString(R.string.expense_title)}: ${expense.title}")
            appendLine("${context.getString(R.string.expense_category)}: $category")
            appendLine("${context.getString(R.string.amount)}: ${CurrencyUtils.format(expense.amount)}")
            appendLine("${context.getString(R.string.payment_mode)}: $payment")
            appendLine("${context.getString(R.string.expense_paid_by)}: ${expense.paidBy}")
            appendLine(
                "${context.getString(R.string.date)}: ${DateTimeUtils.formatDate(expense.dateEpochMillis)}",
            )
            if (expense.notes.isNotBlank()) {
                appendLine("${context.getString(R.string.notes)}: ${expense.notes}")
            }
            appendLine()
            append(context.getString(R.string.wa_message_footer, context.getString(R.string.app_name)))
        }
        return shareText(context, message)
    }

    fun shareText(context: Context, message: String): ShareResult {
        if (!isWhatsAppInstalled(context)) return ShareResult.WhatsAppNotInstalled

        val packageName = when {
            isPackageInstalled(context.packageManager, WHATSAPP) -> WHATSAPP
            isPackageInstalled(context.packageManager, WHATSAPP_BUSINESS) -> WHATSAPP_BUSINESS
            else -> return ShareResult.WhatsAppNotInstalled
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage(packageName)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (sendIntent.resolveActivity(context.packageManager) == null) {
                return ShareResult.WhatsAppNotInstalled
            }
            context.startActivity(sendIntent)
            ShareResult.Success
        } catch (_: ActivityNotFoundException) {
            ShareResult.WhatsAppNotInstalled
        } catch (_: Exception) {
            ShareResult.Failed
        }
    }

    private fun paymentLabel(context: Context, mode: PaymentMode): String = when (mode) {
        PaymentMode.CASH -> context.getString(R.string.payment_cash)
        PaymentMode.UPI -> context.getString(R.string.payment_upi)
    }

    private fun categoryLabel(context: Context, category: ExpenseCategory): String = when (category) {
        ExpenseCategory.PUJA_ITEMS -> context.getString(R.string.expense_cat_puja)
        ExpenseCategory.DECORATION -> context.getString(R.string.expense_cat_decoration)
        ExpenseCategory.PRASAD -> context.getString(R.string.expense_cat_prasad)
        ExpenseCategory.SOUND_LIGHT -> context.getString(R.string.expense_cat_sound)
        ExpenseCategory.TRANSPORT -> context.getString(R.string.expense_cat_transport)
        ExpenseCategory.RENT -> context.getString(R.string.expense_cat_rent)
        ExpenseCategory.UTILITIES -> context.getString(R.string.expense_cat_utilities)
        ExpenseCategory.MISC -> context.getString(R.string.expense_cat_misc)
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean =
        try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}
