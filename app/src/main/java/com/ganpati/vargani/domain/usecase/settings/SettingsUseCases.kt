package com.ganpati.vargani.domain.usecase.settings

import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = repository.observeSettings()
}

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): AppSettings = repository.getSettings()
}

class SaveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(settings: AppSettings) = repository.saveSettings(settings)
}

class PeekNextReceiptNumberUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): String = repository.peekNextReceiptNumber()
}

class AllocateReceiptNumberUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): String = repository.nextReceiptNumber()
}
