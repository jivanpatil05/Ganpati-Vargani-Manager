package com.ganpati.vargani.presentation.donation

import com.ganpati.vargani.domain.model.PaymentMode

/**
 * User actions on the donation form.
 */
sealed interface DonationFormEvent {

    data class ReceiptNoChanged(val value: String) : DonationFormEvent
    data class NameChanged(val value: String) : DonationFormEvent
    data class MobileChanged(val value: String) : DonationFormEvent
    data class AddressChanged(val value: String) : DonationFormEvent
    data class AmountChanged(val value: String) : DonationFormEvent
    data class PaymentModeChanged(val mode: PaymentMode) : DonationFormEvent
    data class CollectorChanged(val value: String) : DonationFormEvent
    data class DateChanged(val epochMillis: Long) : DonationFormEvent
    data class TimeChanged(val epochMillis: Long) : DonationFormEvent
    data class NotesChanged(val value: String) : DonationFormEvent

    data object Save : DonationFormEvent
}
