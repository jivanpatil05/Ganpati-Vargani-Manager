package com.ganpati.vargani.presentation.donation

/**
 * One-shot effects consumed by [DonationFormScreen] (scroll-to-field, navigation).
 */
sealed interface DonationFormEffect {

    data class FirstInvalidField(val key: DonationFormFieldKey) : DonationFormEffect

    data class Saved(val id: Long) : DonationFormEffect
}
