package com.ganpati.vargani.domain.repository

import com.ganpati.vargani.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)
    suspend fun nextReceiptNumber(): String
    suspend fun peekNextReceiptNumber(): String
}
