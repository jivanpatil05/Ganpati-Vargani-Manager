package com.ganpati.vargani.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.PaymentQrStore
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.data.remote.FirebaseStorageUploader
import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.usecase.settings.ObserveSettingsUseCase
import com.ganpati.vargani.domain.usecase.settings.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val organizationName: String = "",
    val organizationAddress: String = "",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val accountHolder: String = "",
    val qrImagePath: String = "",
    val isAdmin: Boolean = false,
    val viewersEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
    observeSettings: ObserveSettingsUseCase,
    private val saveSettings: SaveSettingsUseCase,
    private val storageUploader: FirebaseStorageUploader,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var latestSettings: AppSettings = AppSettings()
    private var paymentDirty = false
    private var organizationDirty = false

    init {
        viewModelScope.launch {
            combine(
                authRepository.observeSession(),
                observeSettings(),
            ) { session, settings ->
                session to settings
            }.collect { (session, settings) ->
                latestSettings = settings
                _uiState.update { current ->
                    val base = current.copy(
                        name = session.name,
                        mobile = session.mobile,
                        email = session.email,
                        isAdmin = session.isAdmin,
                        viewersEnabled = settings.viewersEnabled,
                        isLoading = false,
                    )
                    if (current.isSaving || paymentDirty || organizationDirty) {
                        base
                    } else {
                        base.copy(
                            organizationName = settings.organizationName,
                            organizationAddress = settings.organizationAddress,
                            upiId = settings.upiId,
                            bankName = settings.bankName,
                            accountNumber = settings.accountNumber,
                            ifsc = settings.ifsc,
                            accountHolder = settings.accountHolder,
                            qrImagePath = settings.qrImagePath,
                        )
                    }
                }
            }
        }
    }

    fun onOrganizationNameChanged(value: String) {
        organizationDirty = true
        _uiState.update { it.copy(organizationName = value) }
    }

    fun onOrganizationAddressChanged(value: String) {
        organizationDirty = true
        _uiState.update { it.copy(organizationAddress = value) }
    }

    fun onUpiIdChanged(value: String) {
        paymentDirty = true
        _uiState.update { it.copy(upiId = value) }
    }

    fun onBankNameChanged(value: String) {
        paymentDirty = true
        _uiState.update { it.copy(bankName = value) }
    }

    fun onAccountNumberChanged(value: String) {
        paymentDirty = true
        _uiState.update { it.copy(accountNumber = value) }
    }

    fun onIfscChanged(value: String) {
        paymentDirty = true
        _uiState.update { it.copy(ifsc = value.uppercase()) }
    }

    fun onAccountHolderChanged(value: String) {
        paymentDirty = true
        _uiState.update { it.copy(accountHolder = value) }
    }

    fun toggleViewersEnabled() {
        if (!_uiState.value.isAdmin || _uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = latestSettings.copy(viewersEnabled = !latestSettings.viewersEnabled)
            runCatching { saveSettings(updated) }
                .onSuccess {
                    latestSettings = updated
                    _uiState.update { it.copy(viewersEnabled = updated.viewersEnabled) }
                    _uiEvents.send(
                        UiEvent.ShowSnackbar(
                            context.getString(
                                if (updated.viewersEnabled) {
                                    R.string.viewers_access_enabled
                                } else {
                                    R.string.viewers_access_disabled
                                },
                            ),
                        ),
                    )
                }
                .onFailure {
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun onQrImagePicked(uri: Uri) {
        viewModelScope.launch {
            val path = PaymentQrStore.saveFromUri(context, uri)
            if (path == null) {
                _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.payment_qr_upload_failed)))
                return@launch
            }
            val oldPath = _uiState.value.qrImagePath
            if (oldPath.isNotBlank() && oldPath != path) {
                PaymentQrStore.delete(oldPath)
            }
            paymentDirty = true
            _uiState.update { it.copy(qrImagePath = path) }
            savePaymentDetails(showSuccess = false)
            runCatching { storageUploader.uploadQrImage(path) }
            _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.payment_qr_uploaded)))
        }
    }

    fun onRemoveQrImage() {
        viewModelScope.launch {
            val path = _uiState.value.qrImagePath
            PaymentQrStore.delete(path)
            runCatching { storageUploader.deleteQrImage() }
            paymentDirty = true
            _uiState.update { it.copy(qrImagePath = "") }
            savePaymentDetails(showSuccess = false)
            _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.payment_qr_removed)))
        }
    }

    fun saveOrganizationDetails(showSuccess: Boolean = true) {
        if (!_uiState.value.isAdmin) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val toSave = latestSettings.copy(
                organizationName = state.organizationName.trim()
                    .ifBlank { latestSettings.organizationName },
                organizationAddress = state.organizationAddress.trim(),
            )
            runCatching { saveSettings(toSave) }
                .onSuccess {
                    latestSettings = toSave
                    organizationDirty = false
                    if (showSuccess) {
                        _uiEvents.send(
                            UiEvent.ShowSnackbar(context.getString(R.string.organization_saved)),
                        )
                    }
                }
                .onFailure {
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun savePaymentDetails(showSuccess: Boolean = true) {
        if (!_uiState.value.isAdmin) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val toSave = latestSettings.copy(
                upiId = state.upiId.trim(),
                bankName = state.bankName.trim(),
                accountNumber = state.accountNumber.trim(),
                ifsc = state.ifsc.trim().uppercase(),
                accountHolder = state.accountHolder.trim(),
                qrImagePath = state.qrImagePath,
            )
            runCatching { saveSettings(toSave) }
                .onSuccess {
                    latestSettings = toSave
                    paymentDirty = false
                    if (showSuccess) {
                        _uiEvents.send(
                            UiEvent.ShowSnackbar(context.getString(R.string.payment_details_saved)),
                        )
                    }
                }
                .onFailure {
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}

internal fun buildBreakdownItems(
    donations: List<Donation>,
    expenses: List<Expense>,
): List<BreakdownItem> {
    val incoming = donations.map { donation ->
        BreakdownItem(
            id = donation.id,
            type = BreakdownType.Incoming,
            title = donation.name,
            subtitle = donation.receiptNo,
            amount = donation.amount,
            dateEpochMillis = donation.dateEpochMillis,
        )
    }
    val outgoing = expenses.map { expense ->
        BreakdownItem(
            id = expense.id,
            type = BreakdownType.Outgoing,
            title = expense.title,
            subtitle = expense.category.name.replace('_', ' ').lowercase()
                .replaceFirstChar { it.titlecase() },
            amount = expense.amount,
            dateEpochMillis = expense.dateEpochMillis,
        )
    }
    return (incoming + outgoing).sortedByDescending { it.dateEpochMillis }
}
