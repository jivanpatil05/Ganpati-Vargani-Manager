package com.ganpati.vargani.presentation.donation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.usecase.backup.GenerateReceiptUseCase
import com.ganpati.vargani.domain.usecase.donation.DeleteDonationUseCase
import com.ganpati.vargani.domain.usecase.donation.MarkReceiptPrintedUseCase
import com.ganpati.vargani.domain.usecase.donation.ObserveDonationUseCase
import com.ganpati.vargani.domain.usecase.donation.RestoreDonationUseCase
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

sealed interface DonationDetailsEffect {
    data object Deleted : DonationDetailsEffect
    data class OpenReceipt(val donationId: Long) : DonationDetailsEffect
}

@HiltViewModel
class DonationDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val observeDonation: ObserveDonationUseCase,
    private val deleteDonation: DeleteDonationUseCase,
    private val restoreDonation: RestoreDonationUseCase,
    private val generateReceipt: GenerateReceiptUseCase,
    private val markReceiptPrinted: MarkReceiptPrintedUseCase,
    authRepository: AuthRepository,
) : ViewModel() {

    private val donationId: Long = checkNotNull(savedStateHandle.get<Long>("donationId"))

    private val _uiState = MutableStateFlow(DonationDetailsUiState())
    val uiState: StateFlow<DonationDetailsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DonationDetailsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var deletedSnapshot: Donation? = null

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                _uiState.update { it.copy(canWrite = session.canWrite) }
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
                }
            }
        }
    }

    fun onEvent(event: DonationDetailsEvent) {
        when (event) {
            DonationDetailsEvent.Edit -> Unit
            DonationDetailsEvent.ShowDeleteDialog -> {
                if (!_uiState.value.canWrite) return
                _uiState.update { it.copy(showDeleteDialog = true) }
            }
            DonationDetailsEvent.DismissDeleteDialog -> {
                _uiState.update { it.copy(showDeleteDialog = false) }
            }
            DonationDetailsEvent.ConfirmDelete -> {
                if (_uiState.value.canWrite) confirmDelete()
            }
            DonationDetailsEvent.GenerateReceipt -> {
                viewModelScope.launch {
                    _effects.send(DonationDetailsEffect.OpenReceipt(donationId))
                }
            }
            DonationDetailsEvent.ShareReceipt -> shareOrPrint(share = true)
            DonationDetailsEvent.PrintReceipt -> shareOrPrint(share = false)
        }
    }

    private fun confirmDelete() {
        val donation = _uiState.value.donation ?: return
        if (_uiState.value.isDeleting) return

        _uiState.update { it.copy(isDeleting = true, showDeleteDialog = false) }
        deletedSnapshot = donation

        viewModelScope.launch {
            runCatching { deleteDonation(donationId) }
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false) }
                    _uiEvents.send(
                        UiEvent.ShowSnackbar(
                            message = context.getString(R.string.donation_deleted),
                            actionLabel = context.getString(R.string.undo),
                            action = { undoDelete() },
                        ),
                    )
                    _effects.send(DonationDetailsEffect.Deleted)
                }
                .onFailure {
                    deletedSnapshot = null
                    _uiState.update { it.copy(isDeleting = false) }
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
        }
    }

    private fun undoDelete() {
        val snapshot = deletedSnapshot ?: return
        deletedSnapshot = null
        viewModelScope.launch {
            runCatching { restoreDonation(snapshot) }
                .onFailure {
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
        }
    }

    private fun shareOrPrint(share: Boolean) {
        val donation = _uiState.value.donation ?: return
        if (_uiState.value.isGeneratingReceipt) return

        _uiState.update { it.copy(isGeneratingReceipt = true) }

        viewModelScope.launch {
            runCatching {
                val file = generateReceipt(donation)
                markReceiptPrinted(donationId)
                file
            }.onSuccess { file ->
                _uiState.update { it.copy(isGeneratingReceipt = false) }
                _uiEvents.send(
                    UiEvent.ShowSnackbar(context.getString(R.string.receipt_generated)),
                )
                if (share) {
                    _uiEvents.send(
                        UiEvent.ShareFile(
                            file = file,
                            mimeType = "application/pdf",
                        ),
                    )
                } else {
                    _uiEvents.send(
                        UiEvent.PrintPdf(
                            file = file,
                            jobName = context.getString(R.string.receipt_number) + " ${donation.receiptNo}",
                        ),
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isGeneratingReceipt = false) }
                _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
            }
        }
    }
}
