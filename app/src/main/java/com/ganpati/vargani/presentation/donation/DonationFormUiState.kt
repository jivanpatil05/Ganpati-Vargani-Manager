package com.ganpati.vargani.presentation.donation

import com.ganpati.vargani.domain.model.PaymentMode

/**
 * Immutable UI state for the add / edit donation form.
 * [errors] maps field keys to string resource ids.
 */
data class DonationFormUiState(
    val receiptNo: String = "",
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val pincode: String = "",
    val amountText: String = "",
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val collector: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val timeMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val errors: Map<DonationFormFieldKey, Int> = emptyMap(),
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
)

enum class DonationFormFieldKey {
    NAME,
    MOBILE,
    AMOUNT,
    COLLECTOR,
}
