package com.ganpati.vargani.presentation.reports

import com.ganpati.vargani.core.components.charts.ChartEntry
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.ReportSummary

enum class ReportExportKind {
    Excel,
    Pdf,
}

/**
 * Immutable reports screen state derived from [ReportSummary].
 */
data class ReportsUiState(
    val summary: ReportSummary? = null,
    val daily: List<ChartEntry> = emptyList(),
    val weekly: List<ChartEntry> = emptyList(),
    val paymentModes: List<ChartEntry> = emptyList(),
    val topDonors: List<Donation> = emptyList(),
    val isLoading: Boolean = true,
    val exportingKind: ReportExportKind? = null,
) {
    val isExporting: Boolean get() = exportingKind != null
    val isExportingExcel: Boolean get() = exportingKind == ReportExportKind.Excel
    val isExportingPdf: Boolean get() = exportingKind == ReportExportKind.Pdf
}
