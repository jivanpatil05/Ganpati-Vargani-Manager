package com.ganpati.vargani.presentation.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.usecase.backup.CreateBackupUseCase
import com.ganpati.vargani.domain.usecase.backup.ExportExcelUseCase
import com.ganpati.vargani.domain.usecase.backup.ExportReportPdfUseCase
import com.ganpati.vargani.domain.usecase.backup.ResetDataUseCase
import com.ganpati.vargani.domain.usecase.backup.RestoreBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val resetDataUseCase: ResetDataUseCase,
    private val exportExcelUseCase: ExportExcelUseCase,
    private val exportReportPdfUseCase: ExportReportPdfUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    fun createBackup() {
        showExportOptions(DataExportKind.BACKUP)
    }

    fun exportExcelFile() {
        showExportOptions(DataExportKind.EXCEL)
    }

    fun exportPdfReport() {
        showExportOptions(DataExportKind.PDF)
    }

    fun showExportOptions(kind: DataExportKind) {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(pendingExportKind = kind) }
    }

    fun dismissExportOptions() {
        _uiState.update { it.copy(pendingExportKind = null) }
    }

    fun confirmExport(destination: DataExportDestination) {
        val kind = _uiState.value.pendingExportKind ?: return
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(pendingExportKind = null, isProcessing = true) }
        viewModelScope.launch {
            val result = runCatching {
                when (kind) {
                    DataExportKind.BACKUP -> createBackupUseCase() to "application/zip"
                    DataExportKind.EXCEL -> exportExcelUseCase() to
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    DataExportKind.PDF -> exportReportPdfUseCase() to "application/pdf"
                }
            }
            result
                .onSuccess { (file, mimeType) ->
                    when (destination) {
                        DataExportDestination.DOWNLOAD -> {
                            _uiEvents.send(
                                UiEvent.SaveToDevice(
                                    file = file,
                                    mimeType = mimeType,
                                    suggestedFileName = file.name,
                                ),
                            )
                        }
                        DataExportDestination.SHARE -> {
                            _uiEvents.send(
                                UiEvent.ShareFile(
                                    file = file,
                                    mimeType = mimeType,
                                ),
                            )
                        }
                    }
                }
                .onFailure {
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    fun restoreBackup(input: InputStream) {
        if (_uiState.value.isProcessing) return
        val bytes = runCatching { input.readBytes() }.getOrElse {
            viewModelScope.launch {
                _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.restore_failed)))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            runCatching {
                restoreBackupUseCase(java.io.ByteArrayInputStream(bytes))
            }.onSuccess {
                _uiEvents.send(
                    UiEvent.ShowSnackbar(context.getString(R.string.restore_success)),
                )
            }.onFailure { error ->
                val message = error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.restore_failed)
                _uiEvents.send(UiEvent.ShowSnackbar(message))
            }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    fun showResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = true) }
    }

    fun dismissResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = false) }
    }

    fun confirmReset() {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, showResetConfirmation = false) }
            runCatching { resetDataUseCase() }
                .onSuccess {
                    _uiEvents.send(
                        UiEvent.ShowSnackbar(context.getString(R.string.settings_saved)),
                    )
                }
                .onFailure {
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }
}
