package com.ganpati.vargani.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.CurrencyUtils
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseCategory
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.model.ReportSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full festival report PDF: summary cards + donation/expense tables (multi-page).
 */
@Singleton
class PdfReportWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun write(
        summary: ReportSummary,
        donations: List<Donation>,
        expenses: List<Expense>,
        organizationName: String,
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "report_${DateTimeUtils.exportTimestamp()}.pdf")

        val sortedDonations = donations.sortedByDescending { it.dateEpochMillis }
        val sortedExpenses = expenses.sortedByDescending { it.dateEpochMillis }
        val donationTotal = sortedDonations.sumOf { it.amount }
        val expenseTotal = sortedExpenses.sumOf { it.amount }
        val remaining = donationTotal - expenseTotal

        val document = PdfDocument()
        val drawer = ReportPdfDrawer(
            context = context,
            document = document,
            organizationName = organizationName.ifBlank {
                context.getString(R.string.organization_name)
            },
            donationTotal = donationTotal,
            expenseTotal = expenseTotal,
            remaining = remaining,
            donationCount = sortedDonations.size,
            expenseCount = sortedExpenses.size,
            donorCount = summary.totalDonors.coerceAtLeast(sortedDonations.size),
            cashTotal = summary.cashTotal,
            upiTotal = summary.upiTotal,
        )

        drawer.drawHeaderAndCards()
        drawer.drawDonationsTable(sortedDonations)
        drawer.drawExpensesTable(sortedExpenses)
        drawer.finish()

        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }
}

private class ReportPdfDrawer(
    private val context: Context,
    private val document: PdfDocument,
    private val organizationName: String,
    private val donationTotal: Double,
    private val expenseTotal: Double,
    private val remaining: Double,
    private val donationCount: Int,
    private val expenseCount: Int,
    private val donorCount: Int,
    private val cashTotal: Double,
    private val upiTotal: Double,
) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 36f
    private val contentRight = pageWidth - margin
    private val contentWidth = contentRight - margin
    private val bottomLimit = pageHeight - 48f

    private val primary = Color.parseColor("#1D8FE8")
    private val primaryDark = Color.parseColor("#0B6BCB")
    private val green = Color.parseColor("#16A34A")
    private val greenSoft = Color.parseColor("#ECFDF5")
    private val red = Color.parseColor("#DC2626")
    private val redSoft = Color.parseColor("#FEF2F2")
    private val blueSoft = Color.parseColor("#EFF6FF")
    private val gold = Color.parseColor("#F59E0B")
    private val amberSoft = Color.parseColor("#FFFBEB")
    private val textPrimary = Color.parseColor("#111827")
    private val textSecondary = Color.parseColor("#6B7280")
    private val lineColor = Color.parseColor("#E5E7EB")
    private val headerBg = Color.parseColor("#1E3A5F")
    private val altRow = Color.parseColor("#F8FAFC")
    private val totalRowBg = Color.parseColor("#F1F5F9")

    private var pageNumber = 0
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = 0f

    fun drawHeaderAndCards() {
        newPage(includeSummary = true)
    }

    fun drawDonationsTable(donations: List<Donation>) {
        ensureSpace(60f)
        sectionTitle(
            "${context.getString(R.string.section_incoming)} — ${context.getString(R.string.recent_donations)}",
        )

        val cols = floatArrayOf(28f, 200f, 95f, 70f, 90f)
        val rightAlign = setOf(2)
        val headers = listOf(
            "#",
            context.getString(R.string.donor_name),
            context.getString(R.string.amount),
            context.getString(R.string.payment_mode),
            context.getString(R.string.date),
        )
        drawTableHeader(cols, headers, rightAlign)

        if (donations.isEmpty()) {
            ensureSpace(28f)
            drawText(context.getString(R.string.no_donations), margin + 8f, y + 16f, paint(textSecondary, 10f))
            y += 28f
            return
        }

        donations.forEachIndexed { index, d ->
            if (ensureSpace(22f)) {
                drawTableHeader(cols, headers, rightAlign)
            }
            val bg = if (index % 2 == 0) altRow else Color.WHITE
            drawRowBackground(22f, bg)
            drawTableRow(
                cols = cols,
                values = listOf(
                    (index + 1).toString(),
                    d.name,
                    CurrencyUtils.format(d.amount),
                    paymentLabel(d.paymentMode),
                    DateTimeUtils.formatDate(d.dateEpochMillis),
                ),
                height = 22f,
                rightAlignCols = rightAlign,
            )
            y += 22f
        }

        if (ensureSpace(28f)) {
            // no header needed for total
        }
        drawTotalRow(context.getString(R.string.total_incoming), donationTotal, green)
        y += 16f
    }

    fun drawExpensesTable(expenses: List<Expense>) {
        ensureSpace(60f)
        sectionTitle(
            "${context.getString(R.string.section_outgoing)} — ${context.getString(R.string.recent_expenses)}",
        )

        val cols = floatArrayOf(24f, 145f, 100f, 85f, 55f, 74f)
        val rightAlign = setOf(3)
        val headers = listOf(
            "#",
            context.getString(R.string.expense_title),
            context.getString(R.string.expense_category),
            context.getString(R.string.amount),
            context.getString(R.string.payment_mode),
            context.getString(R.string.date),
        )
        drawTableHeader(cols, headers, rightAlign)

        if (expenses.isEmpty()) {
            ensureSpace(28f)
            drawText(context.getString(R.string.no_expenses), margin + 8f, y + 16f, paint(textSecondary, 10f))
            y += 28f
            return
        }

        expenses.forEachIndexed { index, e ->
            if (ensureSpace(22f)) {
                drawTableHeader(cols, headers, rightAlign)
            }
            val bg = if (index % 2 == 0) altRow else Color.WHITE
            drawRowBackground(22f, bg)
            drawTableRow(
                cols = cols,
                values = listOf(
                    (index + 1).toString(),
                    e.title,
                    categoryLabel(e.category),
                    CurrencyUtils.format(e.amount),
                    paymentLabel(e.paymentMode),
                    DateTimeUtils.formatDate(e.dateEpochMillis),
                ),
                height = 22f,
                rightAlignCols = rightAlign,
            )
            y += 22f
        }

        ensureSpace(28f)
        drawTotalRow(context.getString(R.string.total_outgoing), expenseTotal, red)
        y += 12f
    }

    fun finish() {
        page?.let { document.finishPage(it) }
        page = null
        canvas = null
    }

    private fun newPage(includeSummary: Boolean) {
        page?.let { document.finishPage(it) }
        pageNumber += 1
        page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create(),
        )
        canvas = page!!.canvas
        canvas!!.drawColor(Color.WHITE)
        y = margin

        canvas!!.drawRect(0f, 0f, pageWidth.toFloat(), 8f, fill(primary))

        if (includeSummary) {
            drawSummaryBlock()
        } else {
            drawText(organizationName, margin, y + 14f, paint(primaryDark, 11f, bold = true))
            drawText(
                "${context.getString(R.string.reports_title)} (cont.)",
                contentRight,
                y + 14f,
                paint(textSecondary, 10f).apply { textAlign = Paint.Align.RIGHT },
            )
            y += 24f
            canvas!!.drawLine(margin, y, contentRight, y, stroke(lineColor, 1f))
            y += 14f
        }
        drawFooter()
    }

    private fun drawSummaryBlock() {
        drawText(organizationName, margin, y + 16f, paint(primaryDark, 12f, bold = true))
        y += 22f
        drawText(context.getString(R.string.reports_title), margin, y + 18f, paint(textPrimary, 18f, bold = true))
        drawText(
            DateTimeUtils.formatDateTime(System.currentTimeMillis()),
            contentRight,
            y + 18f,
            paint(textSecondary, 9f).apply { textAlign = Paint.Align.RIGHT },
        )
        y += 28f
        canvas!!.drawRoundRect(RectF(margin, y, margin + 48f, y + 3f), 2f, 2f, fill(gold))
        y += 16f

        // Total collection card
        val totalH = 64f
        val totalRect = RectF(margin, y, contentRight, y + totalH)
        canvas!!.drawRoundRect(totalRect, 12f, 12f, fill(blueSoft))
        canvas!!.drawRoundRect(totalRect, 12f, 12f, stroke(primary, 1.5f))
        drawText(context.getString(R.string.total_collection), margin + 16f, y + 22f, paint(textSecondary, 11f))
        drawText(CurrencyUtils.format(donationTotal), margin + 16f, y + 48f, paint(primaryDark, 22f, bold = true))
        drawText(
            "$donorCount ${context.getString(R.string.total_donors)}",
            contentRight - 16f,
            y + 48f,
            paint(textSecondary, 10f).apply { textAlign = Paint.Align.RIGHT },
        )
        y += totalH + 12f

        // Incoming / Outgoing cards
        val gap = 12f
        val cardW = (contentWidth - gap) / 2f
        val cardH = 78f
        drawStatCard(
            left = margin,
            top = y,
            width = cardW,
            height = cardH,
            fillColor = greenSoft,
            strokeColor = green,
            title = context.getString(R.string.total_incoming),
            amount = CurrencyUtils.format(donationTotal),
            subtitle = "Cash ${CurrencyUtils.format(cashTotal)}  ·  UPI ${CurrencyUtils.format(upiTotal)}",
            amountColor = green,
            countLabel = "$donationCount",
        )
        drawStatCard(
            left = margin + cardW + gap,
            top = y,
            width = cardW,
            height = cardH,
            fillColor = redSoft,
            strokeColor = red,
            title = context.getString(R.string.total_outgoing),
            amount = CurrencyUtils.format(expenseTotal),
            subtitle = "$expenseCount ${context.getString(R.string.recent_expenses)}",
            amountColor = red,
            countLabel = "$expenseCount",
        )
        y += cardH + 12f

        // Remaining
        val remH = 36f
        val remRect = RectF(margin, y, contentRight, y + remH)
        canvas!!.drawRoundRect(remRect, 10f, 10f, fill(amberSoft))
        canvas!!.drawRoundRect(remRect, 10f, 10f, stroke(gold, 1.2f))
        drawText(
            context.getString(R.string.remaining_balance),
            margin + 14f,
            y + 23f,
            paint(textSecondary, 11f),
        )
        drawText(
            CurrencyUtils.format(remaining),
            contentRight - 14f,
            y + 23f,
            paint(if (remaining >= 0) green else red, 14f, bold = true).apply {
                textAlign = Paint.Align.RIGHT
            },
        )
        y += remH + 20f
    }

    private fun drawStatCard(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        fillColor: Int,
        strokeColor: Int,
        title: String,
        amount: String,
        subtitle: String,
        amountColor: Int,
        countLabel: String,
    ) {
        val rect = RectF(left, top, left + width, top + height)
        canvas!!.drawRoundRect(rect, 12f, 12f, fill(fillColor))
        canvas!!.drawRoundRect(rect, 12f, 12f, stroke(strokeColor, 1.4f))
        drawText(title, left + 12f, top + 18f, paint(textSecondary, 10f))
        drawText(amount, left + 12f, top + 44f, paint(amountColor, 16f, bold = true))
        drawText(subtitle, left + 12f, top + 64f, paint(textSecondary, 8f))
        // small count badge top-right
        drawText(
            countLabel,
            left + width - 12f,
            top + 18f,
            paint(strokeColor, 10f, bold = true).apply { textAlign = Paint.Align.RIGHT },
        )
    }

    private fun sectionTitle(title: String) {
        drawText(title, margin, y + 14f, paint(textPrimary, 13f, bold = true))
        y += 20f
        canvas!!.drawLine(margin, y, contentRight, y, stroke(lineColor, 1f))
        y += 8f
    }

    private fun drawTableHeader(cols: FloatArray, headers: List<String>, rightAlign: Set<Int>) {
        ensureSpace(24f)
        val h = 22f
        canvas!!.drawRoundRect(RectF(margin, y, contentRight, y + h), 6f, 6f, fill(headerBg))
        drawTableRow(cols, headers, h, Color.WHITE, bold = true, rightAlign)
        y += h
    }

    private fun drawRowBackground(height: Float, color: Int) {
        canvas!!.drawRect(margin, y, contentRight, y + height, fill(color))
        canvas!!.drawLine(margin, y + height, contentRight, y + height, stroke(lineColor, 0.8f))
    }

    private fun drawTableRow(
        cols: FloatArray,
        values: List<String>,
        height: Float,
        textColor: Int = textPrimary,
        bold: Boolean = false,
        rightAlignCols: Set<Int> = emptySet(),
    ) {
        var x = margin + 6f
        val baseline = y + height * 0.68f
        values.forEachIndexed { i, raw ->
            val colW = cols.getOrElse(i) { 60f }
            val maxW = colW - 10f
            val p = paint(textColor, 9f, bold = bold)
            val text = ellipsize(raw, p, maxW)
            if (i in rightAlignCols) {
                p.textAlign = Paint.Align.RIGHT
                drawText(text, x + maxW, baseline, p)
            } else {
                drawText(text, x, baseline, p)
            }
            x += colW
        }
    }

    private fun drawTotalRow(label: String, amount: Double, accent: Int) {
        val h = 24f
        canvas!!.drawRoundRect(RectF(margin, y, contentRight, y + h), 6f, 6f, fill(totalRowBg))
        drawText(label, margin + 10f, y + 16f, paint(textPrimary, 10f, bold = true))
        drawText(
            CurrencyUtils.format(amount),
            contentRight - 10f,
            y + 16f,
            paint(accent, 11f, bold = true).apply { textAlign = Paint.Align.RIGHT },
        )
        y += h
    }

    /** @return true if a new page was started */
    private fun ensureSpace(needed: Float): Boolean {
        if (y + needed <= bottomLimit) return false
        newPage(includeSummary = false)
        return true
    }

    private fun drawFooter() {
        val c = canvas ?: return
        c.drawText(
            "${context.getString(R.string.app_name)}  ·  $pageNumber",
            pageWidth / 2f,
            pageHeight - 20f,
            paint(textSecondary, 8f).apply { textAlign = Paint.Align.CENTER },
        )
    }

    private fun drawText(text: String, x: Float, baseline: Float, paint: Paint) {
        canvas?.drawText(text, x, baseline, paint)
    }

    private fun paymentLabel(mode: PaymentMode): String = when (mode) {
        PaymentMode.CASH -> context.getString(R.string.payment_cash)
        PaymentMode.UPI -> context.getString(R.string.payment_upi)
    }

    private fun categoryLabel(category: ExpenseCategory): String = when (category) {
        ExpenseCategory.PUJA_ITEMS -> context.getString(R.string.expense_cat_puja)
        ExpenseCategory.DECORATION -> context.getString(R.string.expense_cat_decoration)
        ExpenseCategory.PRASAD -> context.getString(R.string.expense_cat_prasad)
        ExpenseCategory.SOUND_LIGHT -> context.getString(R.string.expense_cat_sound)
        ExpenseCategory.TRANSPORT -> context.getString(R.string.expense_cat_transport)
        ExpenseCategory.RENT -> context.getString(R.string.expense_cat_rent)
        ExpenseCategory.UTILITIES -> context.getString(R.string.expense_cat_utilities)
        ExpenseCategory.MISC -> context.getString(R.string.expense_cat_misc)
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 0 && paint.measureText(text.take(end) + ellipsis) > maxWidth) end--
        return text.take(end.coerceAtLeast(0)) + ellipsis
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }
}
