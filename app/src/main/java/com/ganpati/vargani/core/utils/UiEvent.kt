package com.ganpati.vargani.core.utils

import java.io.File

/**
 * One-shot UI side effects emitted from ViewModels and consumed by composables / activities.
 */
sealed interface UiEvent {

    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null,
    ) : UiEvent

    data class ShareFile(
        val file: File,
        val mimeType: String,
    ) : UiEvent

    /**
     * Opens the system "Save as" dialog so the user can download [file] to Downloads / storage.
     */
    data class SaveToDevice(
        val file: File,
        val mimeType: String,
        val suggestedFileName: String,
    ) : UiEvent

    data class PrintPdf(
        val file: File,
        val jobName: String,
    ) : UiEvent

    data object NavigateBack : UiEvent

    data object LoggedOut : UiEvent
}
