package com.ganpati.vargani.domain.repository

import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ReportSummary
import java.io.File

interface ExportRepository {
    suspend fun exportCsv(donations: List<Donation>): File
    suspend fun exportExcel(donations: List<Donation>): File
    suspend fun exportReportPdf(
        summary: ReportSummary,
        donations: List<Donation>,
        expenses: List<Expense>,
        organizationName: String,
    ): File
}
