package com.ganpati.vargani.presentation.donor

import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.DonationFilter

data class DonorListUiState(
    val donations: List<Donation> = emptyList(),
    val filter: DonationFilter = DonationFilter(),
    val collectors: List<String> = emptyList(),
    val showFilterSheet: Boolean = false,
    val showSortSheet: Boolean = false,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val canWrite: Boolean = false,
)

fun DonationFilter.hasActiveConstraints(): Boolean =
    query.isNotBlank() ||
        collector != null ||
        paymentMode != null ||
        startDateMillis != null ||
        endDateMillis != null ||
        minAmount != null ||
        maxAmount != null
