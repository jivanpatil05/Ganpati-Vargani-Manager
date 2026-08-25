package com.ganpati.vargani.data.remote

import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.model.AppUserProfile
import com.ganpati.vargani.domain.model.Committee
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseCategory
import com.ganpati.vargani.domain.model.FestivalEvent
import com.ganpati.vargani.domain.model.Member
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot

object FirestoreMappers {

    fun donationToMap(donation: Donation, committeeId: String): Map<String, Any?> = mapOf(
        "id" to donation.id,
        "committeeId" to committeeId,
        "receiptNo" to donation.receiptNo,
        "name" to donation.name,
        "mobile" to donation.mobile,
        "email" to donation.email,
        "address" to donation.address,
        "city" to donation.city,
        "pincode" to donation.pincode,
        "amount" to donation.amount,
        "paymentMode" to donation.paymentMode.name,
        "collector" to donation.collector,
        "dateEpochMillis" to donation.dateEpochMillis,
        "timeEpochMillis" to donation.timeEpochMillis,
        "notes" to donation.notes,
        "isReceiptPrinted" to donation.isReceiptPrinted,
        "createdAt" to donation.createdAt,
        "updatedAt" to donation.updatedAt,
    )

    fun DocumentSnapshot.toDonation(): Donation? {
        if (!exists()) return null
        val id = getLong("id") ?: id.toLongOrNull() ?: return null
        return Donation(
            id = id,
            receiptNo = getString("receiptNo").orEmpty(),
            name = getString("name").orEmpty(),
            mobile = getString("mobile").orEmpty(),
            email = getString("email").orEmpty(),
            address = getString("address").orEmpty(),
            city = getString("city").orEmpty(),
            pincode = getString("pincode").orEmpty(),
            amount = getDouble("amount") ?: 0.0,
            paymentMode = PaymentMode.fromStorage(getString("paymentMode").orEmpty()),
            collector = getString("collector").orEmpty(),
            dateEpochMillis = getLong("dateEpochMillis") ?: 0L,
            timeEpochMillis = getLong("timeEpochMillis") ?: 0L,
            notes = getString("notes").orEmpty(),
            isReceiptPrinted = getBoolean("isReceiptPrinted") ?: false,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
        )
    }

    fun expenseToMap(expense: Expense, committeeId: String): Map<String, Any?> = mapOf(
        "id" to expense.id,
        "committeeId" to committeeId,
        "title" to expense.title,
        "category" to expense.category.name,
        "amount" to expense.amount,
        "paymentMode" to expense.paymentMode.name,
        "paidBy" to expense.paidBy,
        "dateEpochMillis" to expense.dateEpochMillis,
        "timeEpochMillis" to expense.timeEpochMillis,
        "notes" to expense.notes,
        "createdAt" to expense.createdAt,
        "updatedAt" to expense.updatedAt,
    )

    fun DocumentSnapshot.toExpense(): Expense? {
        if (!exists()) return null
        val id = getLong("id") ?: id.toLongOrNull() ?: return null
        return Expense(
            id = id,
            title = getString("title").orEmpty(),
            category = ExpenseCategory.fromStorage(getString("category").orEmpty()),
            amount = getDouble("amount") ?: 0.0,
            paymentMode = PaymentMode.fromStorage(getString("paymentMode").orEmpty()),
            paidBy = getString("paidBy").orEmpty(),
            dateEpochMillis = getLong("dateEpochMillis") ?: 0L,
            timeEpochMillis = getLong("timeEpochMillis") ?: 0L,
            notes = getString("notes").orEmpty(),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
        )
    }

    fun settingsToMap(settings: AppSettings, committeeId: String): Map<String, Any?> = mapOf(
        "committeeId" to committeeId,
        "darkMode" to settings.darkMode,
        "dynamicColor" to settings.dynamicColor,
        "languageCode" to settings.languageCode,
        "receiptPrefix" to settings.receiptPrefix,
        "receiptCounter" to settings.receiptCounter,
        "organizationName" to settings.organizationName,
        "organizationAddress" to settings.organizationAddress,
        "organizationLogoUri" to settings.organizationLogoUri,
        "upiId" to settings.upiId,
        "bankName" to settings.bankName,
        "accountNumber" to settings.accountNumber,
        "ifsc" to settings.ifsc,
        "accountHolder" to settings.accountHolder,
        "qrImagePath" to settings.qrImagePath,
        "whatsappGroupNotifyEnabled" to settings.whatsappGroupNotifyEnabled,
        "viewersEnabled" to settings.viewersEnabled,
    )

    fun DocumentSnapshot.toAppSettings(): AppSettings {
        if (!exists()) return AppSettings()
        return AppSettings(
            darkMode = getBoolean("darkMode") ?: false,
            dynamicColor = getBoolean("dynamicColor") ?: false,
            languageCode = getString("languageCode") ?: "en",
            receiptPrefix = getString("receiptPrefix") ?: "GV",
            receiptCounter = getLong("receiptCounter") ?: 1L,
            organizationName = getString("organizationName") ?: "Ganpati Festival Committee",
            organizationAddress = getString("organizationAddress").orEmpty(),
            organizationLogoUri = getString("organizationLogoUri").orEmpty(),
            upiId = getString("upiId").orEmpty(),
            bankName = getString("bankName").orEmpty(),
            accountNumber = getString("accountNumber").orEmpty(),
            ifsc = getString("ifsc").orEmpty(),
            accountHolder = getString("accountHolder").orEmpty(),
            qrImagePath = getString("qrImagePath").orEmpty(),
            whatsappGroupNotifyEnabled = getBoolean("whatsappGroupNotifyEnabled") ?: true,
            viewersEnabled = getBoolean("viewersEnabled") ?: true,
        )
    }

    fun DocumentSnapshot.toUserProfile(): AppUserProfile? {
        if (!exists()) return null
        return AppUserProfile(
            uid = id,
            name = getString("name").orEmpty(),
            email = getString("email").orEmpty(),
            mobile = getString("mobile").orEmpty(),
            role = UserRole.fromStorage(getString("role")),
            committeeId = getString("committeeId").orEmpty(),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        )
    }

    fun userProfileToMap(profile: AppUserProfile): Map<String, Any?> = mapOf(
        "name" to profile.name,
        "email" to profile.email,
        "mobile" to profile.mobile,
        "role" to profile.role.name,
        "committeeId" to profile.committeeId,
        "createdAt" to profile.createdAt,
    )

    fun committeeToMap(committee: Committee): Map<String, Any?> = mapOf(
        "name" to committee.name,
        "address" to committee.address,
        "createdBy" to committee.createdBy,
        "createdAt" to committee.createdAt,
    )

    fun DocumentSnapshot.toCommittee(): Committee? {
        if (!exists()) return null
        return Committee(
            id = id,
            name = getString("name").orEmpty(),
            address = getString("address").orEmpty(),
            createdBy = getString("createdBy").orEmpty(),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        )
    }

    fun memberToMap(member: Member): Map<String, Any?> = mapOf(
        "committeeId" to member.committeeId,
        "name" to member.name,
        "mobile" to member.mobile,
        "address" to member.address,
        "createdAt" to member.createdAt,
    )

    fun DocumentSnapshot.toMember(): Member? {
        if (!exists()) return null
        return Member(
            id = id,
            committeeId = getString("committeeId").orEmpty(),
            name = getString("name").orEmpty(),
            mobile = getString("mobile").orEmpty(),
            address = getString("address").orEmpty(),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        )
    }

    fun eventToMap(event: FestivalEvent): Map<String, Any?> = mapOf(
        "committeeId" to event.committeeId,
        "title" to event.title,
        "description" to event.description,
        "startDateMillis" to event.startDateMillis,
        "endDateMillis" to event.endDateMillis,
        "createdAt" to event.createdAt,
    )

    fun DocumentSnapshot.toFestivalEvent(): FestivalEvent? {
        if (!exists()) return null
        return FestivalEvent(
            id = id,
            committeeId = getString("committeeId").orEmpty(),
            title = getString("title").orEmpty(),
            description = getString("description").orEmpty(),
            startDateMillis = getLong("startDateMillis") ?: 0L,
            endDateMillis = getLong("endDateMillis") ?: 0L,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        )
    }
}
