package com.ganpati.vargani.data.repository

import android.content.Context
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.core.utils.LocaleHelper
import com.ganpati.vargani.data.local.datastore.SettingsDataStore
import com.ganpati.vargani.data.remote.UserSessionStore
import com.ganpati.vargani.domain.model.AppLanguage
import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseCategory
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.BackupRepository
import com.ganpati.vargani.domain.repository.DonationRepository
import com.ganpati.vargani.domain.repository.ExpenseRepository
import com.ganpati.vargani.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON+ZIP backup/restore against Firestore-backed repositories.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val donationRepository: DonationRepository,
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val settingsDataStore: SettingsDataStore,
    private val sessionStore: UserSessionStore,
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun createBackup(): File = withContext(Dispatchers.IO) {
        val donations = donationRepository.observeAll().first()
        val expenses = expenseRepository.observeAll().first()
        val settings = settingsRepository.getSettings()
        val payload = BackupPayload(
            version = BACKUP_VERSION,
            createdAt = System.currentTimeMillis(),
            settings = BackupSettings.from(settings),
            donations = donations.map { BackupDonation.from(it) },
            expenses = expenses.map { BackupExpense.from(it) },
        )
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val outFile = File(backupDir, "vargani_backup_${DateTimeUtils.exportTimestamp()}.zip")
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
            zip.write(json.encodeToString(payload).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        outFile
    }

    override suspend fun restoreBackup(input: InputStream) = withContext(Dispatchers.IO) {
        sessionStore.requireAdminAccess()
        val bytes = input.readBytes()
        require(bytes.isNotEmpty()) { "Backup file is empty" }

        val jsonText = extractBackupJson(bytes)
        val payload = runCatching {
            json.decodeFromString<BackupPayload>(jsonText)
        }.getOrElse { error ->
            throw IllegalArgumentException(
                "Invalid backup format. Use a file created by Backup Database.",
                error,
            )
        }

        donationRepository.deleteAll()
        expenseRepository.observeAll().first().forEach { expenseRepository.delete(it.id) }

        settingsRepository.saveSettings(payload.settings.toDomain())
        payload.donations.forEach { dto ->
            donationRepository.insert(dto.toDomain().copy(id = 0L))
        }
        payload.expenses.forEach { dto ->
            expenseRepository.save(dto.toDomain().copy(id = 0L))
        }

        val restoredSettings = payload.settings.toDomain()
        settingsDataStore.save(restoredSettings)
        LocaleHelper.persist(context, AppLanguage.fromCode(restoredSettings.languageCode))
    }

    override suspend fun resetAllData() {
        sessionStore.requireAdminAccess()
        donationRepository.deleteAll()
        expenseRepository.observeAll().first().forEach { expenseRepository.delete(it.id) }
        settingsRepository.saveSettings(AppSettings())
        settingsDataStore.save(AppSettings())
        LocaleHelper.persist(context, AppLanguage.ENGLISH)
    }

    private fun extractBackupJson(bytes: ByteArray): String {
        val fromZip = runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    if (!entry.isDirectory && name.equals(BACKUP_JSON_NAME, ignoreCase = true)) {
                        return@use zip.readBytes().toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                null
            }
        }.getOrNull()

        if (!fromZip.isNullOrBlank()) return fromZip

        val asText = bytes.toString(Charsets.UTF_8).trim()
        if (asText.startsWith("{") && asText.contains("\"donations\"")) {
            return asText
        }

        throw IllegalArgumentException(
            "Invalid backup file. Select a .zip created by Backup Database.",
        )
    }

    companion object {
        private const val BACKUP_VERSION = 2
        private const val BACKUP_JSON_NAME = "backup.json"
    }
}

@Serializable
private data class BackupPayload(
    val version: Int = 2,
    val createdAt: Long = 0L,
    val settings: BackupSettings = BackupSettings(),
    val donations: List<BackupDonation> = emptyList(),
    val expenses: List<BackupExpense> = emptyList(),
)

@Serializable
private data class BackupSettings(
    val darkMode: Boolean = false,
    val dynamicColor: Boolean = false,
    val languageCode: String = "en",
    val receiptPrefix: String = "GV",
    val receiptCounter: Long = 1L,
    val organizationName: String = "Ganpati Festival Committee",
    val organizationAddress: String = "",
    val organizationLogoUri: String = "",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val accountHolder: String = "",
    val qrImagePath: String = "",
    val whatsappGroupNotifyEnabled: Boolean = true,
    val viewersEnabled: Boolean = true,
) {
    fun toDomain() = AppSettings(
        darkMode = darkMode,
        dynamicColor = dynamicColor,
        languageCode = languageCode,
        receiptPrefix = receiptPrefix,
        receiptCounter = receiptCounter,
        organizationName = organizationName,
        organizationAddress = organizationAddress,
        organizationLogoUri = organizationLogoUri,
        upiId = upiId,
        bankName = bankName,
        accountNumber = accountNumber,
        ifsc = ifsc,
        accountHolder = accountHolder,
        qrImagePath = qrImagePath,
        whatsappGroupNotifyEnabled = whatsappGroupNotifyEnabled,
        viewersEnabled = viewersEnabled,
    )

    companion object {
        fun from(s: AppSettings) = BackupSettings(
            darkMode = s.darkMode,
            dynamicColor = s.dynamicColor,
            languageCode = s.languageCode,
            receiptPrefix = s.receiptPrefix,
            receiptCounter = s.receiptCounter,
            organizationName = s.organizationName,
            organizationAddress = s.organizationAddress,
            organizationLogoUri = s.organizationLogoUri,
            upiId = s.upiId,
            bankName = s.bankName,
            accountNumber = s.accountNumber,
            ifsc = s.ifsc,
            accountHolder = s.accountHolder,
            qrImagePath = s.qrImagePath,
            whatsappGroupNotifyEnabled = s.whatsappGroupNotifyEnabled,
            viewersEnabled = s.viewersEnabled,
        )
    }
}

@Serializable
private data class BackupDonation(
    val receiptNo: String = "",
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val pincode: String = "",
    val amount: Double = 0.0,
    val paymentMode: String = "CASH",
    val collector: String = "",
    val dateEpochMillis: Long = 0L,
    val timeEpochMillis: Long = 0L,
    val notes: String = "",
    val isReceiptPrinted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun toDomain() = Donation(
        id = 0L,
        receiptNo = receiptNo,
        name = name,
        mobile = mobile,
        email = email,
        address = address,
        city = city,
        pincode = pincode,
        amount = amount,
        paymentMode = PaymentMode.fromStorage(paymentMode),
        collector = collector,
        dateEpochMillis = dateEpochMillis,
        timeEpochMillis = timeEpochMillis,
        notes = notes,
        isReceiptPrinted = isReceiptPrinted,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun from(d: Donation) = BackupDonation(
            receiptNo = d.receiptNo,
            name = d.name,
            mobile = d.mobile,
            email = d.email,
            address = d.address,
            city = d.city,
            pincode = d.pincode,
            amount = d.amount,
            paymentMode = d.paymentMode.name,
            collector = d.collector,
            dateEpochMillis = d.dateEpochMillis,
            timeEpochMillis = d.timeEpochMillis,
            notes = d.notes,
            isReceiptPrinted = d.isReceiptPrinted,
            createdAt = d.createdAt,
            updatedAt = d.updatedAt,
        )
    }
}

@Serializable
private data class BackupExpense(
    val title: String = "",
    val category: String = "MISC",
    val amount: Double = 0.0,
    val paymentMode: String = "CASH",
    val paidBy: String = "",
    val dateEpochMillis: Long = 0L,
    val timeEpochMillis: Long = 0L,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun toDomain() = Expense(
        id = 0L,
        title = title,
        category = ExpenseCategory.fromStorage(category),
        amount = amount,
        paymentMode = PaymentMode.fromStorage(paymentMode),
        paidBy = paidBy,
        dateEpochMillis = dateEpochMillis,
        timeEpochMillis = timeEpochMillis,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun from(e: Expense) = BackupExpense(
            title = e.title,
            category = e.category.name,
            amount = e.amount,
            paymentMode = e.paymentMode.name,
            paidBy = e.paidBy,
            dateEpochMillis = e.dateEpochMillis,
            timeEpochMillis = e.timeEpochMillis,
            notes = e.notes,
            createdAt = e.createdAt,
            updatedAt = e.updatedAt,
        )
    }
}
