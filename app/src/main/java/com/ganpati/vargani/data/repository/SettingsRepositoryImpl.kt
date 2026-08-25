package com.ganpati.vargani.data.repository

import androidx.room.withTransaction
import com.ganpati.vargani.core.constants.AppConstants
import com.ganpati.vargani.data.local.datastore.SettingsDataStore
import com.ganpati.vargani.data.local.room.EntityMappers.toDomain
import com.ganpati.vargani.data.local.room.EntityMappers.toEntity
import com.ganpati.vargani.data.local.room.VarganiDatabase
import com.ganpati.vargani.data.local.room.dao.SettingsDao
import com.ganpati.vargani.data.local.room.entity.SettingsEntity
import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val settingsDataStore: SettingsDataStore,
    private val database: VarganiDatabase
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        settingsDao.observeSettings().map { entity ->
            entity?.toDomain() ?: AppSettings()
        }

    override suspend fun getSettings(): AppSettings {
        ensureDefaults()
        return settingsDao.getSettings()?.toDomain() ?: AppSettings()
    }

    override suspend fun saveSettings(settings: AppSettings) {
        database.withTransaction {
            settingsDao.upsert(settings.toEntity())
            settingsDataStore.save(settings)
        }
    }

    override suspend fun nextReceiptNumber(): String {
        return database.withTransaction {
            ensureDefaults()
            val current = settingsDao.getSettings() ?: SettingsEntity()
            val number = current.receiptCounter
            val receipt = formatReceipt(current.receiptPrefix, number)
            settingsDao.updateReceiptCounter(number + 1)
            val updated = current.copy(receiptCounter = number + 1)
            settingsDataStore.save(updated.toDomain())
            receipt
        }
    }

    override suspend fun peekNextReceiptNumber(): String {
        ensureDefaults()
        val current = settingsDao.getSettings() ?: SettingsEntity()
        return formatReceipt(current.receiptPrefix, current.receiptCounter)
    }

    private suspend fun ensureDefaults() {
        if (settingsDao.getSettings() == null) {
            val defaults = SettingsEntity(
                receiptPrefix = AppConstants.DEFAULT_RECEIPT_PREFIX,
                receiptCounter = AppConstants.DEFAULT_RECEIPT_START,
                organizationName = AppConstants.DEFAULT_ORG_NAME
            )
            settingsDao.upsert(defaults)
            settingsDataStore.save(defaults.toDomain())
        }
    }

    private fun formatReceipt(prefix: String, number: Long): String =
        "%s-%04d".format(prefix.trim().ifEmpty { AppConstants.DEFAULT_RECEIPT_PREFIX }, number)
}
