package com.ganpati.vargani.domain.repository

import java.io.File
import java.io.InputStream

/**
 * Offline backup / restore contract.
 * Designed so a future cloud provider can wrap the same APIs.
 */
interface BackupRepository {
    suspend fun createBackup(): File
    suspend fun restoreBackup(input: InputStream)
    suspend fun resetAllData()
}
