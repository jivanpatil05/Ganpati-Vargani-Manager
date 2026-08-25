package com.ganpati.vargani.data.repository

import android.content.Context
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ReportSummary
import com.ganpati.vargani.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline CSV / Excel / report PDF exporters.
 */
@Singleton
class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfReportWriter: PdfReportWriter,
) : ExportRepository {

    override suspend fun exportCsv(donations: List<Donation>): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "donations_${DateTimeUtils.exportTimestamp()}.csv")
        val sorted = donations.sortedByDescending { it.dateEpochMillis }
        BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)).use { writer ->
            writer.write("\uFEFF") // Excel-friendly BOM
            writer.appendLine(HEADER.joinToString(","))
            sorted.forEach { d ->
                writer.appendLine(donationCells(d).joinToString(",") { csvEscape(it) })
            }
            val total = sorted.sumOf { it.amount }
            writer.appendLine(
                listOf("", "TOTAL", "", "", "", "", "", "%.2f".format(total), "", "", "", "", "", "")
                    .joinToString(",") { csvEscape(it) },
            )
        }
        file
    }

    override suspend fun exportExcel(donations: List<Donation>): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "donations_${DateTimeUtils.exportTimestamp()}.xlsx")
        writeXlsx(file, donations.sortedByDescending { it.dateEpochMillis })
        file
    }

    override suspend fun exportReportPdf(
        summary: ReportSummary,
        donations: List<Donation>,
        expenses: List<Expense>,
        organizationName: String,
    ): File = withContext(Dispatchers.IO) {
        pdfReportWriter.write(summary, donations, expenses, organizationName)
    }

    private fun donationCells(d: Donation): List<String> = listOf(
        d.receiptNo,
        d.name,
        d.mobile,
        d.email,
        d.address,
        d.city,
        d.pincode,
        "%.2f".format(d.amount),
        d.paymentMode.name,
        d.collector,
        DateTimeUtils.formatDate(d.dateEpochMillis),
        DateTimeUtils.formatTime(d.timeEpochMillis),
        d.notes,
        if (d.isReceiptPrinted) "Yes" else "No",
    )

    private fun csvEscape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    /**
     * Writes a valid OOXML .xlsx that Excel / Google Sheets / LibreOffice can open.
     * Includes every donor row plus a TOTAL amount line.
     */
    private fun writeXlsx(file: File, donations: List<Donation>) {
        val total = donations.sumOf { it.amount }
        val rowsXml = buildString {
            append(rowXml(1, HEADER, asText = true))
            donations.forEachIndexed { index, donation ->
                append(donationRowXml(index + 2, donation))
            }
            val totalRowIndex = donations.size + 2
            append(totalRowXml(totalRowIndex, total))
        }

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            put(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
                    """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
                    """<Default Extension="xml" ContentType="application/xml"/>""" +
                    """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
                    """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
                    """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
                    """</Types>""",
            )
            put(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                    """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
                    """</Relationships>""",
            )
            put(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
                    """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
                    """<sheets><sheet name="Donations" sheetId="1" r:id="rId1"/></sheets>""" +
                    """</workbook>""",
            )
            put(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                    """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
                    """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
                    """</Relationships>""",
            )
            put(
                "xl/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
                    """<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>""" +
                    """<fills count="1"><fill><patternFill patternType="none"/></fill></fills>""" +
                    """<borders count="1"><border/></borders>""" +
                    """<cellStyleXfs count="1"><xf/></cellStyleXfs>""" +
                    """<cellXfs count="1"><xf xfId="0"/></cellXfs>""" +
                    """</styleSheet>""",
            )
            put(
                "xl/worksheets/sheet1.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
                    """<sheetData>""" + rowsXml + """</sheetData>""" +
                    """</worksheet>""",
            )
        }
    }

    private fun donationRowXml(rowIndex: Int, d: Donation): String = buildString {
        append("""<row r="$rowIndex">""")
        textCell(this, 0, rowIndex, d.receiptNo)
        textCell(this, 1, rowIndex, d.name)
        textCell(this, 2, rowIndex, d.mobile)
        textCell(this, 3, rowIndex, d.email)
        textCell(this, 4, rowIndex, d.address)
        textCell(this, 5, rowIndex, d.city)
        textCell(this, 6, rowIndex, d.pincode)
        numberCell(this, 7, rowIndex, d.amount)
        textCell(this, 8, rowIndex, d.paymentMode.name)
        textCell(this, 9, rowIndex, d.collector)
        textCell(this, 10, rowIndex, DateTimeUtils.formatDate(d.dateEpochMillis))
        textCell(this, 11, rowIndex, DateTimeUtils.formatTime(d.timeEpochMillis))
        textCell(this, 12, rowIndex, d.notes)
        textCell(this, 13, rowIndex, if (d.isReceiptPrinted) "Yes" else "No")
        append("</row>")
    }

    private fun totalRowXml(rowIndex: Int, total: Double): String = buildString {
        append("""<row r="$rowIndex">""")
        textCell(this, 0, rowIndex, "")
        textCell(this, 1, rowIndex, "TOTAL")
        textCell(this, 2, rowIndex, "")
        textCell(this, 3, rowIndex, "")
        textCell(this, 4, rowIndex, "")
        textCell(this, 5, rowIndex, "")
        textCell(this, 6, rowIndex, "")
        numberCell(this, 7, rowIndex, total)
        textCell(this, 8, rowIndex, "")
        textCell(this, 9, rowIndex, "")
        textCell(this, 10, rowIndex, "")
        textCell(this, 11, rowIndex, "")
        textCell(this, 12, rowIndex, "")
        textCell(this, 13, rowIndex, "")
        append("</row>")
    }

    private fun rowXml(rowIndex: Int, cells: List<String>, asText: Boolean): String = buildString {
        append("""<row r="$rowIndex">""")
        cells.forEachIndexed { col, value ->
            if (asText) textCell(this, col, rowIndex, value)
            else textCell(this, col, rowIndex, value)
        }
        append("</row>")
    }

    private fun textCell(sb: StringBuilder, col: Int, row: Int, value: String) {
        val ref = columnName(col) + row
        val escaped = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        sb.append("""<c r="$ref" t="inlineStr"><is><t xml:space="preserve">$escaped</t></is></c>""")
    }

    private fun numberCell(sb: StringBuilder, col: Int, row: Int, value: Double) {
        val ref = columnName(col) + row
        sb.append("""<c r="$ref"><v>${"%.2f".format(value)}</v></c>""")
    }

    private fun columnName(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + i % 26))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    companion object {
        private val HEADER = listOf(
            "Receipt No",
            "Name",
            "Mobile",
            "Email",
            "Address",
            "City",
            "Pincode",
            "Amount",
            "Payment Mode",
            "Collector",
            "Date",
            "Time",
            "Notes",
            "Receipt Printed",
        )
    }
}
