package com.ganpati.vargani.data.local.room

import com.ganpati.vargani.data.local.room.entity.DonationEntity
import com.ganpati.vargani.data.local.room.entity.ExpenseEntity
import com.ganpati.vargani.data.local.room.entity.SettingsEntity
import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseCategory
import com.ganpati.vargani.domain.model.PaymentMode

/** Maps between Room entities and pure domain models. */
object EntityMappers {

    fun DonationEntity.toDomain(): Donation = Donation(
        id = id,
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
        updatedAt = updatedAt
    )

    fun Donation.toEntity(): DonationEntity = DonationEntity(
        id = id,
        receiptNo = receiptNo,
        name = name.trim(),
        mobile = mobile.trim(),
        email = email.trim(),
        address = address.trim(),
        city = city.trim(),
        pincode = pincode.trim(),
        amount = amount,
        paymentMode = paymentMode.name,
        collector = collector.trim(),
        dateEpochMillis = dateEpochMillis,
        timeEpochMillis = timeEpochMillis,
        notes = notes.trim(),
        isReceiptPrinted = isReceiptPrinted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun SettingsEntity.toDomain(): AppSettings = AppSettings(
        darkMode = darkMode,
        dynamicColor = dynamicColor,
        languageCode = languageCode,
        receiptPrefix = receiptPrefix,
        receiptCounter = receiptCounter,
        organizationName = organizationName,
        organizationAddress = organizationAddress,
        organizationLogoUri = organizationLogo,
        upiId = upiId,
        bankName = bankName,
        accountNumber = accountNumber,
        ifsc = ifsc,
        accountHolder = accountHolder,
        qrImagePath = qrImagePath,
        whatsappGroupNotifyEnabled = whatsappGroupNotifyEnabled,
    )

    fun AppSettings.toEntity(): SettingsEntity = SettingsEntity(
        id = 1,
        darkMode = darkMode,
        dynamicColor = dynamicColor,
        languageCode = languageCode,
        receiptPrefix = receiptPrefix,
        receiptCounter = receiptCounter,
        organizationName = organizationName,
        organizationAddress = organizationAddress,
        organizationLogo = organizationLogoUri,
        upiId = upiId,
        bankName = bankName,
        accountNumber = accountNumber,
        ifsc = ifsc,
        accountHolder = accountHolder,
        qrImagePath = qrImagePath,
        whatsappGroupNotifyEnabled = whatsappGroupNotifyEnabled,
    )

    fun ExpenseEntity.toDomain(): Expense = Expense(
        id = id,
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

    fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
        id = id,
        title = title.trim(),
        category = category.name,
        amount = amount,
        paymentMode = paymentMode.name,
        paidBy = paidBy.trim(),
        dateEpochMillis = dateEpochMillis,
        timeEpochMillis = timeEpochMillis,
        notes = notes.trim(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}