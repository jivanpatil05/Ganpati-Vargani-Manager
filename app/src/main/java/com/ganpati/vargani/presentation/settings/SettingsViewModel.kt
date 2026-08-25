package com.ganpati.vargani.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.BuildConfig
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.LocaleHelper
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.AppLanguage
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.usecase.settings.ObserveSettingsUseCase
import com.ganpati.vargani.domain.usecase.settings.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val saveSettings: SaveSettingsUseCase,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(appVersion = BuildConfig.VERSION_NAME),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var textSaveJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                _uiState.update { it.copy(isAdmin = session.isAdmin) }
            }
        }
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { current ->
                    if (current.isSaving) {
                        current
                    } else {
                        current.copy(
                            darkMode = settings.darkMode,
                            dynamicColor = settings.dynamicColor,
                            languageCode = settings.languageCode,
                            receiptPrefix = settings.receiptPrefix,
                            receiptCounter = settings.receiptCounter,
                            organizationName = settings.organizationName,
                            organizationAddress = settings.organizationAddress,
                            organizationLogoUri = settings.organizationLogoUri,
                            upiId = settings.upiId,
                            bankName = settings.bankName,
                            accountNumber = settings.accountNumber,
                            ifsc = settings.ifsc,
                            accountHolder = settings.accountHolder,
                            qrImagePath = settings.qrImagePath,
                            whatsappGroupNotifyEnabled = settings.whatsappGroupNotifyEnabled,
                            viewersEnabled = settings.viewersEnabled,
                            appVersion = BuildConfig.VERSION_NAME,
                        )
                    }
                }
                LocaleHelper.persist(context, AppLanguage.fromCode(settings.languageCode))
            }
        }
    }

    fun toggleDarkMode() {
        updateSettings { copy(darkMode = !darkMode) }
    }

    fun toggleWhatsAppGroupNotify() {
        updateSettings { copy(whatsappGroupNotifyEnabled = !whatsappGroupNotifyEnabled) }
    }

    fun setLanguage(language: AppLanguage) {
        if (_uiState.value.languageCode == language.code) return
        updateSettings { copy(languageCode = language.code) }
    }

    fun onReceiptPrefixChanged(value: String) {
        _uiState.update { it.copy(receiptPrefix = value) }
        debouncedSave()
    }

    fun onReceiptCounterChanged(value: String) {
        val counter = value.filter { it.isDigit() }.toLongOrNull() ?: return
        _uiState.update { it.copy(receiptCounter = counter.coerceAtLeast(1L)) }
        debouncedSave()
    }

    fun showPrivacyPolicy() {
        _uiState.update { it.copy(showPrivacyPolicy = true) }
    }

    fun dismissPrivacyPolicy() {
        _uiState.update { it.copy(showPrivacyPolicy = false) }
    }

    fun showAbout() {
        _uiState.update { it.copy(showAbout = true) }
    }

    fun dismissAbout() {
        _uiState.update { it.copy(showAbout = false) }
    }

    fun logout() {
        _uiState.update { it.copy(showLogoutConfirm = true) }
    }

    fun dismissLogoutConfirm() {
        _uiState.update { it.copy(showLogoutConfirm = false) }
    }

    fun confirmLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(showLogoutConfirm = false) }
            authRepository.logout()
            _uiEvents.send(UiEvent.LoggedOut)
        }
    }

    private fun debouncedSave() {
        textSaveJob?.cancel()
        textSaveJob = viewModelScope.launch {
            delay(TEXT_SAVE_DEBOUNCE_MS)
            persistSettings(showSnackbar = false)
        }
    }

    private fun updateSettings(transform: SettingsUiState.() -> SettingsUiState) {
        _uiState.update(transform)
        viewModelScope.launch { persistSettings(showSnackbar = false) }
    }

    private suspend fun persistSettings(showSnackbar: Boolean) {
        val snapshot = _uiState.value
        _uiState.update { it.copy(isSaving = true) }
        runCatching { saveSettings(snapshot.toAppSettings()) }
            .onSuccess {
                if (showSnackbar) {
                    _uiEvents.send(
                        UiEvent.ShowSnackbar(context.getString(R.string.settings_saved)),
                    )
                }
            }
            .onFailure {
                _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
            }
        _uiState.update { it.copy(isSaving = false) }
    }

    companion object {
        private const val TEXT_SAVE_DEBOUNCE_MS = 400L
    }
}
