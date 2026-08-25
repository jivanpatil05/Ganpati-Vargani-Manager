package com.ganpati.vargani.presentation.donation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.core.utils.ValidationUtils
import com.ganpati.vargani.core.utils.WhatsAppGroupNotifyHelper
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.usecase.donation.GetDonationUseCase
import com.ganpati.vargani.domain.usecase.donation.SaveDonationUseCase
import com.ganpati.vargani.domain.usecase.settings.AllocateReceiptNumberUseCase
import com.ganpati.vargani.domain.usecase.settings.GetSettingsUseCase
import com.ganpati.vargani.domain.usecase.settings.PeekNextReceiptNumberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DonationFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val getDonation: GetDonationUseCase,
    private val saveDonation: SaveDonationUseCase,
    private val peekNextReceiptNumber: PeekNextReceiptNumberUseCase,
    private val allocateReceiptNumber: AllocateReceiptNumberUseCase,
    private val getSettings: GetSettingsUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val navDonationId: Long? =
        savedStateHandle.get<Long>("donationId")?.takeIf { it > 0L }

    private val _uiState = MutableStateFlow(DonationFormUiState(isLoading = navDonationId != null))
    val uiState: StateFlow<DonationFormUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DonationFormEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var existingDonation: Donation? = null
    private var initialized = false

    init {
        // Prefer nav arg when present (edit route); add route initializes via [initialize].
        if (navDonationId != null) {
            initialize(navDonationId)
        }
    }

    /**
     * Called from the composable when the route supplies an explicit id
     * (add route has no nav arg; edit route also works via SavedStateHandle).
     */
    fun initialize(donationId: Long?) {
        if (initialized) return
        initialized = true
        if (donationId == null || donationId <= 0L) {
            initNewDonation()
        } else {
            loadDonation(donationId)
        }
    }

    fun onEvent(event: DonationFormEvent) {
        when (event) {
            is DonationFormEvent.ReceiptNoChanged -> updateField { copy(receiptNo = event.value) }
            is DonationFormEvent.NameChanged -> updateField {
                copy(name = event.value, errors = errors - DonationFormFieldKey.NAME)
            }
            is DonationFormEvent.MobileChanged -> updateField {
                copy(
                    mobile = event.value.filter { it.isDigit() }.take(10),
                    errors = errors - DonationFormFieldKey.MOBILE,
                )
            }
            is DonationFormEvent.AddressChanged -> updateField { copy(address = event.value) }
            is DonationFormEvent.AmountChanged -> updateField {
                copy(amountText = event.value, errors = errors - DonationFormFieldKey.AMOUNT)
            }
            is DonationFormEvent.PaymentModeChanged -> updateField { copy(paymentMode = event.mode) }
            is DonationFormEvent.CollectorChanged -> Unit // Collector is fixed from signup name
            is DonationFormEvent.DateChanged -> updateField { copy(dateMillis = normalizeDate(event.epochMillis)) }
            is DonationFormEvent.TimeChanged -> updateField { copy(timeMillis = normalizeTime(event.epochMillis)) }
            is DonationFormEvent.NotesChanged -> updateField { copy(notes = event.value) }
            DonationFormEvent.Save -> save()
        }
    }

    private fun initNewDonation() {
        viewModelScope.launch {
            val receipt = runCatching { peekNextReceiptNumber() }.getOrDefault("GV-0001")
            val collectorName = authRepository.observeSession().first().name.trim()
            val now = System.currentTimeMillis()
            _uiState.value = DonationFormUiState(
                receiptNo = receipt,
                collector = collectorName,
                dateMillis = DateTimeUtils.startOfDay(now),
                timeMillis = now,
                isEditMode = false,
                isLoading = false,
            )
        }
    }

    private fun loadDonation(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val donation = getDonation(id)
            if (donation == null) {
                _uiState.update { it.copy(isLoading = false) }
                _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                _uiEvents.send(UiEvent.NavigateBack)
                return@launch
            }
            existingDonation = donation
            _uiState.value = DonationFormUiState(
                receiptNo = donation.receiptNo,
                name = donation.name,
                mobile = donation.mobile,
                email = donation.email,
                address = donation.address,
                city = donation.city,
                pincode = donation.pincode,
                amountText = formatAmount(donation.amount),
                paymentMode = donation.paymentMode,
                collector = donation.collector,
                dateMillis = donation.dateEpochMillis,
                timeMillis = donation.timeEpochMillis,
                notes = donation.notes,
                isEditMode = true,
                isLoading = false,
            )
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val errors = validate(state)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            val first = DonationFormFieldKey.entries.firstOrNull { it in errors }
            if (first != null) {
                viewModelScope.launch {
                    _effects.send(DonationFormEffect.FirstInvalidField(first))
                }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val receiptNo = if (state.isEditMode) {
                    state.receiptNo
                } else {
                    allocateReceiptNumber()
                }
                val base = existingDonation
                val collectorName = if (state.isEditMode) {
                    state.collector.trim().ifBlank {
                        authRepository.observeSession().first().name.trim()
                    }
                } else {
                    authRepository.observeSession().first().name.trim()
                        .ifBlank { state.collector.trim() }
                }
                val donation = Donation(
                    id = base?.id ?: 0L,
                    receiptNo = receiptNo,
                    name = state.name.trim(),
                    mobile = state.mobile.trim(),
                    email = "",
                    address = state.address.trim(),
                    city = "",
                    pincode = "",
                    amount = state.amountText.toDouble(),
                    paymentMode = state.paymentMode,
                    collector = collectorName,
                    dateEpochMillis = state.dateMillis,
                    timeEpochMillis = state.timeMillis,
                    notes = state.notes.trim(),
                    isReceiptPrinted = base?.isReceiptPrinted ?: false,
                    createdAt = base?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
                val id = saveDonation(donation)
                Triple(id, state.isEditMode, donation)
            }.onSuccess { (id, isEdit, donation) ->
                _uiState.update { it.copy(isSaving = false, receiptNo = it.receiptNo) }
                _uiEvents.send(
                    UiEvent.ShowSnackbar(
                        context.getString(
                            if (isEdit) R.string.donation_updated else R.string.donation_saved,
                        ),
                    ),
                )
                if (!isEdit) {
                    notifyWhatsAppGroup(donation)
                }
                _effects.send(DonationFormEffect.Saved(id))
            }.onFailure {
                _uiState.update { it.copy(isSaving = false) }
                val message = when {
                    it.message?.contains("unique", ignoreCase = true) == true ||
                        it.message?.contains("Receipt", ignoreCase = true) == true ->
                        context.getString(R.string.error_receipt_unique)
                    else -> context.getString(R.string.error_generic)
                }
                _uiEvents.send(UiEvent.ShowSnackbar(message))
            }
        }
    }

    private suspend fun notifyWhatsAppGroup(donation: Donation) {
        val settings = runCatching { getSettings() }.getOrNull() ?: return
        if (!settings.whatsappGroupNotifyEnabled) return
        WhatsAppGroupNotifyHelper.shareDonation(
            context = context,
            donation = donation,
            orgName = settings.organizationName,
        )
    }

    private fun validate(state: DonationFormUiState): Map<DonationFormFieldKey, Int> {
        val errors = linkedMapOf<DonationFormFieldKey, Int>()
        if (state.name.isBlank()) {
            errors[DonationFormFieldKey.NAME] = R.string.error_name_required
        }
        if (state.mobile.isNotBlank() && !ValidationUtils.isValidIndianMobile(state.mobile)) {
            errors[DonationFormFieldKey.MOBILE] = R.string.error_mobile_invalid
        }
        if (state.amountText.isBlank()) {
            errors[DonationFormFieldKey.AMOUNT] = R.string.error_amount_required
        } else if (!ValidationUtils.isValidAmount(state.amountText)) {
            errors[DonationFormFieldKey.AMOUNT] = R.string.error_amount_positive
        }
        if (state.collector.isBlank()) {
            errors[DonationFormFieldKey.COLLECTOR] = R.string.error_collector_required
        }
        return errors
    }

    private fun normalizeDate(epochMillis: Long): Long = DateTimeUtils.startOfDay(epochMillis)

    private fun normalizeTime(epochMillis: Long): Long {
        val cal = Calendar.getInstance(Locale("en", "IN")).apply { timeInMillis = epochMillis }
        return cal.timeInMillis
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            "%.2f".format(Locale.US, amount)
        }
    }

    private inline fun updateField(block: DonationFormUiState.() -> DonationFormUiState) {
        _uiState.update(block)
    }
}
