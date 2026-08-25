package com.ganpati.vargani.presentation.donation

import com.ganpati.vargani.domain.model.Donation

/**
 * Immutable UI state for the donation details screen.
 */
data class DonationDetailsUiState(
    val donation: Donation? = null,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val isGeneratingReceipt: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val canWrite: Boolean = false,
    val errorMessage: String? = null,
)
