package com.ganpati.vargani.presentation.backup

enum class DataExportKind {
    BACKUP,
    EXCEL,
    PDF,
}

enum class DataExportDestination {
    DOWNLOAD,
    SHARE,
}

data class BackupUiState(
    val isProcessing: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val pendingExportKind: DataExportKind? = null,
)
