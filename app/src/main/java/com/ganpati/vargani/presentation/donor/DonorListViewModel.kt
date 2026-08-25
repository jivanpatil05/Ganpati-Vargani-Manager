package com.ganpati.vargani.presentation.donor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.DonationFilter
import com.ganpati.vargani.domain.model.DonationSort
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.usecase.donation.DeleteDonationUseCase
import com.ganpati.vargani.domain.usecase.donation.ObserveCollectorsUseCase
import com.ganpati.vargani.domain.usecase.donation.ObserveDonationsUseCase
import com.ganpati.vargani.domain.usecase.donation.RestoreDonationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DonorListEvent {
    data class SearchQueryChanged(val query: String) : DonorListEvent
    data object OpenFilterSheet : DonorListEvent
    data object CloseFilterSheet : DonorListEvent
    data object OpenSortSheet : DonorListEvent
    data object CloseSortSheet : DonorListEvent
    data class CollectorChanged(val collector: String?) : DonorListEvent
    data class PaymentModeChanged(val mode: PaymentMode?) : DonorListEvent
    data class StartDateChanged(val millis: Long?) : DonorListEvent
    data class EndDateChanged(val millis: Long?) : DonorListEvent
    data class MinAmountChanged(val amount: Double?) : DonorListEvent
    data class MaxAmountChanged(val amount: Double?) : DonorListEvent
    data class SortChanged(val sort: DonationSort) : DonorListEvent
    data object ClearFilters : DonorListEvent
    data object ApplyFilters : DonorListEvent
    data class DeleteDonation(val id: Long) : DonorListEvent
}

@OptIn(FlowPreview::class)
@HiltViewModel
class DonorListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeDonations: ObserveDonationsUseCase,
    private val observeCollectors: ObserveCollectorsUseCase,
    private val deleteDonation: DeleteDonationUseCase,
    private val restoreDonation: RestoreDonationUseCase,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(DonationFilter())
    val filter: StateFlow<DonationFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _showFilterSheet = MutableStateFlow(false)
    private val _showSortSheet = MutableStateFlow(false)

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var deletedSnapshot: Donation? = null
    private val _deletingIds = MutableStateFlow(emptySet<Long>())

    val uiState: StateFlow<DonorListUiState> = combine(
        combine(
            _searchQuery,
            _filter,
            _showFilterSheet,
            _showSortSheet,
            _deletingIds,
        ) { searchQuery, filter, showFilterSheet, showSortSheet, deletingIds ->
            DonorListMeta(
                searchQuery = searchQuery,
                filter = filter,
                showFilterSheet = showFilterSheet,
                showSortSheet = showSortSheet,
                deletingIds = deletingIds,
            )
        },
        observeCollectors(),
        _filter.flatMapLatest { filter -> observeDonations(filter) },
        authRepository.observeSession(),
    ) { meta, collectors, donations, session ->
        DonorListUiState(
            donations = donations.filter { it.id !in meta.deletingIds },
            filter = meta.filter,
            collectors = collectors,
            showFilterSheet = meta.showFilterSheet,
            showSortSheet = meta.showSortSheet,
            isLoading = false,
            searchQuery = meta.searchQuery,
            canWrite = session.canWrite,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = DonorListUiState(isLoading = true),
    )

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { query ->
                    _filter.update { it.copy(query = query.trim()) }
                }
        }
    }

    fun onEvent(event: DonorListEvent) {
        when (event) {
            is DonorListEvent.SearchQueryChanged -> {
                _searchQuery.value = event.query
            }
            DonorListEvent.OpenFilterSheet -> {
                _showFilterSheet.value = true
            }
            DonorListEvent.CloseFilterSheet -> {
                _showFilterSheet.value = false
            }
            DonorListEvent.OpenSortSheet -> {
                _showSortSheet.value = true
            }
            DonorListEvent.CloseSortSheet -> {
                _showSortSheet.value = false
            }
            is DonorListEvent.CollectorChanged -> {
                _filter.update { it.copy(collector = event.collector?.takeIf { name -> name.isNotBlank() }) }
            }
            is DonorListEvent.PaymentModeChanged -> {
                _filter.update { it.copy(paymentMode = event.mode) }
            }
            is DonorListEvent.StartDateChanged -> {
                _filter.update { it.copy(startDateMillis = event.millis) }
            }
            is DonorListEvent.EndDateChanged -> {
                _filter.update { it.copy(endDateMillis = event.millis) }
            }
            is DonorListEvent.MinAmountChanged -> {
                _filter.update { it.copy(minAmount = event.amount) }
            }
            is DonorListEvent.MaxAmountChanged -> {
                _filter.update { it.copy(maxAmount = event.amount) }
            }
            is DonorListEvent.SortChanged -> {
                _filter.update { it.copy(sort = event.sort) }
                _showSortSheet.value = false
            }
            DonorListEvent.ClearFilters -> {
                val currentQuery = _searchQuery.value
                val currentSort = _filter.value.sort
                _filter.value = DonationFilter(query = currentQuery.trim(), sort = currentSort)
            }
            DonorListEvent.ApplyFilters -> {
                _showFilterSheet.value = false
            }
            is DonorListEvent.DeleteDonation -> {
                if (uiState.value.canWrite) deleteWithUndo(event.id)
            }
        }
    }

    private fun deleteWithUndo(id: Long) {
        val snapshot = uiState.value.donations.find { it.id == id } ?: return
        if (id in _deletingIds.value) return

        _deletingIds.update { it + id }
        deletedSnapshot = snapshot

        viewModelScope.launch {
            runCatching { deleteDonation(id) }
                .onSuccess {
                    _deletingIds.update { it - id }
                    _uiEvents.send(
                        UiEvent.ShowSnackbar(
                            message = context.getString(R.string.donation_deleted),
                            actionLabel = context.getString(R.string.undo),
                            action = { undoDelete() },
                        ),
                    )
                }
                .onFailure {
                    _deletingIds.update { it - id }
                    deletedSnapshot = null
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
        }
    }

    private fun undoDelete() {
        val snapshot = deletedSnapshot ?: return
        deletedSnapshot = null
        viewModelScope.launch {
            runCatching { restoreDonation(snapshot) }
                .onFailure {
                    _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                }
        }
    }
}

private data class DonorListMeta(
    val searchQuery: String,
    val filter: DonationFilter,
    val showFilterSheet: Boolean,
    val showSortSheet: Boolean,
    val deletingIds: Set<Long>,
)
