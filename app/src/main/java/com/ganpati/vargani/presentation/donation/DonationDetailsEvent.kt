package com.ganpati.vargani.presentation.donation

/**
 * User actions on the donation details screen.
 */
sealed interface DonationDetailsEvent {

    data object Edit : DonationDetailsEvent
    data object ShowDeleteDialog : DonationDetailsEvent
    data object DismissDeleteDialog : DonationDetailsEvent
    data object ConfirmDelete : DonationDetailsEvent
    data object GenerateReceipt : DonationDetailsEvent
    data object ShareReceipt : DonationDetailsEvent
    data object PrintReceipt : DonationDetailsEvent
}
