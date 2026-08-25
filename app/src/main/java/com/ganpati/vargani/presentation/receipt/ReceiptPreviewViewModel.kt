package com.ganpati.vargani.presentation.receipt

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.usecase.backup.GenerateReceiptUseCase
import com.ganpati.vargani.domain.usecase.donation.MarkReceiptPrintedUseCase
import com.ganpati.vargani.domain.usecase.donation.ObserveDonationUseCase
import com.ganpati.vargani.domain.usecase.settings.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ReceiptPreviewUiState(
    val donation: Donation? = null,
    val settings: AppSettings = AppSettings(),
    val receiptFile: File? = null,
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ReceiptPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val observeDonation: ObserveDonationUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    private val generateReceipt: GenerateReceiptUseCase,
    private val markReceiptPrinted: MarkReceiptPrintedUseCase,
) : ViewModel() {

    private val donationId: Long = checkNotNull(savedStateHandle.get<Long>("donationId"))

    private val _uiState = MutableStateFlow(ReceiptPreviewUiState())
    val uiState: StateFlow<ReceiptPreviewUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        viewModelScope.launch {
            observeDonation(donationId).collect { donation ->
                _uiState.update {
                    it.copy(
                        donation = donation,
                        isLoading = donation == null && it.isLoading,
                        errorMessage = if (donation == null && !it.isLoading) {
                            context.getString(R.string.error_generic)
                        } else {
                            null
                        },
                    )
                }
                if (donation != null) {
                    _uiState.update { it.copy(isLoading = false) }
                    if (_uiState.value.receiptFile == null && !_uiState.value.isGenerating) {
                        generateReceiptPdf(donation, showSnackbar = false)
                    }
                }
            }
        }
    }

    fun onShare() {
        val donation = _uiState.value.donation ?: return
        if (_uiState.value.isGenerating) return

        val existingFile = _uiState.value.receiptFile
        if (existingFile != null) {
            viewModelScope.launch {
                markReceiptPrinted(donationId)
                _uiEvents.send(
                    UiEvent.ShareFile(
                        file = existingFile,
                        mimeType = "application/pdf",
                    ),
                )
            }
            return
        }

        generateReceiptPdf(
            donation = donation,
            showSnackbar = true,
            onComplete = { file ->
                viewModelScope.launch {
                    markReceiptPrinted(donationId)
                    _uiEvents.send(
                        UiEvent.ShareFile(
                            file = file,
                            mimeType = "application/pdf",
                        ),
                    )
                }
            },
        )
    }

    private fun generateReceiptPdf(
        donation: Donation,
        force: Boolean = false,
        showSnackbar: Boolean = true,
        onComplete: ((File) -> Unit)? = null,
    ) {
        if (_uiState.value.isGenerating) return
        if (!force && _uiState.value.receiptFile != null && onComplete == null) return

        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching { generateReceipt(donation) }
                .onSuccess { file ->
                    _uiState.update {
                        it.copy(
                            receiptFile = file,
                            isGenerating = false,
                        )
                    }
                    if (showSnackbar) {
                        _uiEvents.send(
                            UiEvent.ShowSnackbar(context.getString(R.string.receipt_generated)),
                        )
                    }
                    onComplete?.invoke(file)
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = context.getString(R.string.error_generic),
                        )
                    }
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
        }
    }
}
