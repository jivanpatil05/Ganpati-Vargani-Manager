package com.ganpati.vargani.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.CurrencyUtils
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.ReceiptRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline PDF receipt styled to match the in-app Receipt Preview screen.
 */
@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReceiptRepository {

    override suspend fun generateReceiptPdf(
        donation: Donation,
        settings: AppSettings,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "receipt_${donation.receiptNo.replace('/', '-')}.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        drawReceipt(page.canvas, donation, settings)
        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        file
    }

    private fun drawReceipt(canvas: Canvas, donation: Donation, settings: AppSettings) {
        val primary = Color.parseColor("#3DA9FC")
        val primaryDark = Color.parseColor("#1D8FE8")
        val primaryMid = Color.parseColor("#2B9CF0")
        val gold = Color.parseColor("#F59E0B")
        val textPrimary = Color.parseColor("#111827")
        val textSecondary = Color.parseColor("#4B5563")
        val softPrimaryFill = Color.argb(20, 61, 169, 252)
        val cashGreen = Color.parseColor("#22C55E")
        val upiBlue = Color.parseColor("#3DA9FC")
        val dividerColor = Color.parseColor("#E5E7EB")

        canvas.drawColor(Color.WHITE)

        val margin = 40f
        val contentWidth = PAGE_WIDTH - margin * 2f
        val contentRight = PAGE_WIDTH - margin
        val labelWidth = contentWidth * 0.42f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 11f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val whiteTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val whiteSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 255, 255)
            textSize = 11f
        }

        val headerHeight = 148f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, PAGE_WIDTH.toFloat(), 0f,
                intArrayOf(primaryDark, primary, primaryMid),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, headerPaint)

        val badgeCx = margin + 22f
        val badgeCy = 34f
        canvas.drawCircle(
            badgeCx,
            badgeCy,
            18f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(51, 255, 255, 255) },
        )
        val omPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "ॐ",
            badgeCx,
            badgeCy - (omPaint.descent() + omPaint.ascent()) / 2f,
            omPaint,
        )

        val orgName = settings.organizationName.ifBlank {
            context.getString(R.string.organization_name)
        }
        var y = 62f
        y = drawWrappedText(canvas, orgName, margin, y, contentWidth, whiteTitle, 24f, 2) + 6f
        if (settings.organizationAddress.isNotBlank()) {
            y = drawWrappedText(
                canvas,
                settings.organizationAddress,
                margin,
                y,
                contentWidth,
                whiteSub,
                14f,
                2,
            )
        }

        canvas.drawRect(
            0f,
            headerHeight,
            PAGE_WIDTH.toFloat(),
            headerHeight + 5f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold },
        )

        y = headerHeight + 28f
        canvas.drawText(
            context.getString(R.string.receipt_preview),
            margin,
            y,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textPrimary
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
        )
        y += 22f

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            strokeWidth = 1f
        }

        fun row(label: String, value: String) {
            canvas.drawText(label, margin, y, labelPaint)
            val maxValueWidth = contentWidth - labelWidth - 8f
            val lines = wrapText(value, valuePaint, maxValueWidth)
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, contentRight, y + index * 14f, valuePaint)
            }
            y += maxOf(18f, lines.size * 14f + 4f)
            canvas.drawLine(margin, y, contentRight, y, dividerPaint)
            y += 14f
        }

        row(context.getString(R.string.receipt_number), donation.receiptNo)
        row(context.getString(R.string.donor_name), donation.name)
        if (donation.mobile.isNotBlank()) {
            row(context.getString(R.string.mobile_number), donation.mobile)
        }
        if (donation.address.isNotBlank()) {
            row(context.getString(R.string.address), donation.address)
        }
        row(context.getString(R.string.collector_name), donation.collector)
        row(context.getString(R.string.date), DateTimeUtils.formatDate(donation.dateEpochMillis))
        if (donation.notes.isNotBlank()) {
            row(context.getString(R.string.notes), donation.notes)
        }

        y += 8f
        val amountBoxTop = y
        val amountBoxHeight = 72f
        val amountBox = RectF(margin, amountBoxTop, contentRight, amountBoxTop + amountBoxHeight)
        canvas.drawRoundRect(
            amountBox,
            12f,
            12f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = softPrimaryFill },
        )
        canvas.drawRoundRect(
            amountBox,
            12f,
            12f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primary
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            },
        )

        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primary
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            CurrencyUtils.format(donation.amount),
            PAGE_WIDTH / 2f,
            amountBoxTop + 34f,
            amountPaint,
        )

        val modeLabel = when (donation.paymentMode) {
            PaymentMode.CASH -> context.getString(R.string.payment_cash)
            PaymentMode.UPI -> context.getString(R.string.payment_upi)
        }
        val badgeColor = when (donation.paymentMode) {
            PaymentMode.CASH -> cashGreen
            PaymentMode.UPI -> upiBlue
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeW = badgeTextPaint.measureText(modeLabel) + 24f
        val badgeH = 18f
        val badgeLeft = (PAGE_WIDTH - badgeW) / 2f
        val badgeTop = amountBoxTop + 42f
        canvas.drawRoundRect(
            RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeH),
            9f,
            9f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor },
        )
        canvas.drawText(
            modeLabel,
            PAGE_WIDTH / 2f,
            badgeTop + badgeH / 2f - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f,
            badgeTextPaint,
        )

        y = amountBoxTop + amountBoxHeight + 28f
        canvas.drawText(
            "Generated offline by ${context.getString(R.string.app_name)}",
            PAGE_WIDTH / 2f,
            y,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 11f
                textAlign = Paint.Align.CENTER
            },
        )
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float,
        maxLines: Int,
    ): Float {
        val lines = wrapText(text, paint, maxWidth).take(maxLines)
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, y + index * lineHeight, paint)
        }
        return y + (lines.size - 1).coerceAtLeast(0) * lineHeight
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("-")
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines.ifEmpty { listOf(text) }
    }

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
    }
}
