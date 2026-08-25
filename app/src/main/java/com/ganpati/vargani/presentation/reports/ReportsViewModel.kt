package com.ganpati.vargani.presentation.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.charts.ChartEntry
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.CollectionPoint
import com.ganpati.vargani.domain.usecase.backup.ExportExcelUseCase
import com.ganpati.vargani.domain.usecase.backup.ExportReportPdfUseCase
import com.ganpati.vargani.domain.usecase.report.ObserveReportSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    observeReportSummary: ObserveReportSummaryUseCase,
    private val exportExcel: ExportExcelUseCase,
    private val exportReportPdf: ExportReportPdfUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeReportSummary().collect { summary ->
                _uiState.update {
                    it.copy(
                        summary = summary,
                        daily = summary.daily.toChartEntries(),
                        weekly = summary.weekly.toChartEntries(),
                        paymentModes = listOf(
                            ChartEntry(
                                label = context.getString(R.string.payment_cash),
                                value = summary.cashTotal,
                            ),
                            ChartEntry(
                                label = context.getString(R.string.payment_upi),
                                value = summary.upiTotal,
                            ),
                        ).filter { entry -> entry.value > 0.0 },
                        topDonors = summary.topDonors,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun exportExcel() = export(ReportExportKind.Excel) { exportExcel.invoke() }

    fun exportPdf() = export(ReportExportKind.Pdf) { exportReportPdf.invoke() }

    /** Call after the system share sheet has been launched. */
    fun onShareIntentOpened() {
        _uiState.update { it.copy(exportingKind = null) }
    }

    private fun export(kind: ReportExportKind, block: suspend () -> java.io.File) {
        if (_uiState.value.isExporting) return

        _uiState.update { it.copy(exportingKind = kind) }
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { file ->
                    _uiEvents.send(
                        UiEvent.ShareFile(
                            file = file,
                            mimeType = mimeTypeFor(file),
                        ),
                    )
                }
                .onFailure {
                    _uiState.update { it.copy(exportingKind = null) }
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
        }
    }

    private fun mimeTypeFor(file: java.io.File): String {
        return when (file.extension.lowercase()) {
            "csv" -> "text/csv"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "pdf" -> "application/pdf"
            else -> "*/*"
        }
    }

    private fun List<CollectionPoint>.toChartEntries(): List<ChartEntry> =
        map { ChartEntry(label = it.label, value = it.amount) }
}
