package com.ganpati.vargani.domain.usecase.backup

import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.repository.BackupRepository
import com.ganpati.vargani.domain.repository.DonationRepository
import com.ganpati.vargani.domain.repository.ExpenseRepository
import com.ganpati.vargani.domain.repository.ExportRepository
import com.ganpati.vargani.domain.repository.ReceiptRepository
import com.ganpati.vargani.domain.repository.SettingsRepository
import com.ganpati.vargani.domain.usecase.report.ObserveReportSummaryUseCase
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): File = backupRepository.createBackup()
}

class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(input: InputStream) = backupRepository.restoreBackup(input)
}

class ResetDataUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke() = backupRepository.resetAllData()
}

class ExportCsvUseCase @Inject constructor(
    private val exportRepository: ExportRepository,
    private val donationRepository: DonationRepository
) {
    suspend operator fun invoke(): File {
        val donations = donationRepository.observeAll().first()
        return exportRepository.exportCsv(donations)
    }
}

class ExportExcelUseCase @Inject constructor(
    private val exportRepository: ExportRepository,
    private val donationRepository: DonationRepository
) {
    suspend operator fun invoke(): File {
        val donations = donationRepository.observeAll().first()
        return exportRepository.exportExcel(donations)
    }
}

class ExportReportPdfUseCase @Inject constructor(
    private val exportRepository: ExportRepository,
    private val donationRepository: DonationRepository,
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val reportUseCase: ObserveReportSummaryUseCase,
) {
    suspend operator fun invoke(): File {
        val donations = donationRepository.observeAll().first()
        val expenses = expenseRepository.observeAll().first()
        val settings = settingsRepository.getSettings()
        val summary = reportUseCase.once()
        return exportRepository.exportReportPdf(
            summary = summary,
            donations = donations,
            expenses = expenses,
            organizationName = settings.organizationName,
        )
    }
}

class GenerateReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(donation: Donation): File {
        val settings = settingsRepository.getSettings()
        return receiptRepository.generateReceiptPdf(donation, settings)
    }
}
