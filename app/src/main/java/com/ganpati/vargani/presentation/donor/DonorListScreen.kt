package com.ganpati.vargani.presentation.donor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.ConfirmationDialog
import com.ganpati.vargani.core.components.DonationCard
import com.ganpati.vargani.core.components.EmptyStateView
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.SearchBarField
import com.ganpati.vargani.core.components.SecondaryButton
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.TextFieldShape
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.DonationFilter
import com.ganpati.vargani.domain.model.DonationSort
import com.ganpati.vargani.domain.model.PaymentMode

@Composable
fun DonorListRoute(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: DonorListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.action?.invoke()
                    }
                }
                is UiEvent.ShareFile,
                is UiEvent.PrintPdf,
                is UiEvent.SaveToDevice,
                UiEvent.NavigateBack,
                UiEvent.LoggedOut -> Unit
            }
        }
    }

    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    if (pendingDeleteId != null) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            onConfirm = {
                val id = pendingDeleteId
                pendingDeleteId = null
                if (id != null) {
                    viewModel.onEvent(DonorListEvent.DeleteDonation(id))
                }
            },
            onDismiss = { pendingDeleteId = null },
            confirmText = stringResource(R.string.delete),
        )
    }

    DonorListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAdd = onAdd,
        onOpen = onOpen,
        onEdit = onEdit,
        onEvent = { event ->
            when (event) {
                is DonorListEvent.DeleteDonation -> pendingDeleteId = event.id
                else -> viewModel.onEvent(event)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorListScreen(
    uiState: DonorListUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onEvent: (DonorListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.nav_donors),
                onNavigateBack = onBack,
                actions = {
                    IconButton(onClick = { onEvent(DonorListEvent.OpenFilterSheet) }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.cd_open_filters),
                        )
                    }
                    IconButton(onClick = { onEvent(DonorListEvent.OpenSortSheet) }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = stringResource(R.string.cd_open_sort),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (uiState.canWrite) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_donation),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchBarField(
                query = uiState.searchQuery,
                onQueryChange = { onEvent(DonorListEvent.SearchQueryChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingView(showShimmerPlaceholder = true)
                    }
                }
                uiState.donations.isEmpty() -> {
                    EmptyDonorListState(
                        filter = uiState.filter,
                        hasSearchQuery = uiState.searchQuery.isNotBlank(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.donations,
                            key = { it.id },
                        ) { donation ->
                            SwipeableDonationItem(
                                donation = donation,
                                canWrite = uiState.canWrite,
                                onOpen = { onOpen(donation.id) },
                                onEdit = { onEdit(donation.id) },
                                onDelete = { onEvent(DonorListEvent.DeleteDonation(donation.id)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showFilterSheet) {
        FilterBottomSheet(
            filter = uiState.filter,
            collectors = uiState.collectors,
            onEvent = onEvent,
            onDismiss = { onEvent(DonorListEvent.CloseFilterSheet) },
        )
    }

    if (uiState.showSortSheet) {
        SortBottomSheet(
            selectedSort = uiState.filter.sort,
            onSortSelected = { onEvent(DonorListEvent.SortChanged(it)) },
            onDismiss = { onEvent(DonorListEvent.CloseSortSheet) },
        )
    }
}

@Composable
private fun EmptyDonorListState(
    filter: DonationFilter,
    hasSearchQuery: Boolean,
    modifier: Modifier = Modifier,
) {
    val isFiltered = filter.hasActiveConstraints() || hasSearchQuery
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (isFiltered) {
            EmptyStateView(
                title = stringResource(R.string.no_search_results),
                subtitle = stringResource(R.string.clear_filters),
                icon = Icons.Outlined.SearchOff,
            )
        } else {
            EmptyStateView(
                title = stringResource(R.string.no_donations),
                subtitle = stringResource(R.string.no_donations_hint),
                icon = Icons.Outlined.Inbox,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDonationItem(
    donation: Donation,
    canWrite: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!canWrite) {
        DonationCard(
            receiptNo = donation.receiptNo,
            name = donation.name,
            amount = donation.amount,
            paymentMode = donation.paymentMode,
            collector = donation.collector,
            dateEpochMillis = donation.dateEpochMillis,
            mobile = donation.mobile,
            onClick = onOpen,
            modifier = modifier,
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
    )

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> stringResource(R.string.swipe_edit)
                SwipeToDismissBoxValue.EndToStart -> stringResource(R.string.swipe_delete)
                else -> ""
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Outlined.Delete
                else -> Icons.Outlined.Edit
            }
            val contentColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardShape)
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                        )
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                        )
                    }
                }
            }
        },
        content = {
            DonationCard(
                receiptNo = donation.receiptNo,
                name = donation.name,
                amount = donation.amount,
                paymentMode = donation.paymentMode,
                collector = donation.collector,
                dateEpochMillis = donation.dateEpochMillis,
                mobile = donation.mobile,
                onClick = onOpen,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filter: DonationFilter,
    collectors: List<String>,
    onEvent: (DonorListEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var collectorExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var minAmountText by remember(filter.minAmount) {
        mutableStateOf(filter.minAmount?.toString().orEmpty())
    }
    var maxAmountText by remember(filter.maxAmount) {
        mutableStateOf(filter.maxAmount?.toString().orEmpty())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.filter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            CollectorFilterDropdown(
                collectors = collectors,
                selectedCollector = filter.collector,
                expanded = collectorExpanded,
                onExpandedChange = { collectorExpanded = it },
                onCollectorSelected = {
                    onEvent(DonorListEvent.CollectorChanged(it))
                    collectorExpanded = false
                },
            )

            PaymentModeFilterDropdown(
                selectedMode = filter.paymentMode,
                expanded = paymentExpanded,
                onExpandedChange = { paymentExpanded = it },
                onModeSelected = {
                    onEvent(DonorListEvent.PaymentModeChanged(it))
                    paymentExpanded = false
                },
            )

            OutlinedTextField(
                value = filter.startDateMillis?.let { DateTimeUtils.formatDate(it) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartDatePicker = true },
                label = { Text(text = stringResource(R.string.date) + " (from)") },
                placeholder = { Text(text = stringResource(R.string.select_date)) },
                shape = TextFieldShape,
            )

            OutlinedTextField(
                value = filter.endDateMillis?.let { DateTimeUtils.formatDate(it) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndDatePicker = true },
                label = { Text(text = stringResource(R.string.date) + " (to)") },
                placeholder = { Text(text = stringResource(R.string.select_date)) },
                shape = TextFieldShape,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = minAmountText,
                    onValueChange = { text ->
                        minAmountText = text
                        onEvent(
                            DonorListEvent.MinAmountChanged(
                                text.toDoubleOrNull()?.takeIf { it >= 0 },
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = stringResource(R.string.amount) + " (min)") },
                    shape = TextFieldShape,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = { text ->
                        maxAmountText = text
                        onEvent(
                            DonorListEvent.MaxAmountChanged(
                                text.toDoubleOrNull()?.takeIf { it >= 0 },
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = stringResource(R.string.amount) + " (max)") },
                    shape = TextFieldShape,
                    singleLine = true,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton(
                    text = stringResource(R.string.clear_filters),
                    onClick = {
                        minAmountText = ""
                        maxAmountText = ""
                        onEvent(DonorListEvent.ClearFilters)
                    },
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(R.string.apply_filters),
                    onClick = { onEvent(DonorListEvent.ApplyFilters) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showStartDatePicker) {
        DateFilterDialog(
            initialMillis = filter.startDateMillis,
            onConfirm = { millis ->
                onEvent(DonorListEvent.StartDateChanged(DateTimeUtils.startOfDay(millis)))
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
        )
    }

    if (showEndDatePicker) {
        DateFilterDialog(
            initialMillis = filter.endDateMillis,
            onConfirm = { millis ->
                onEvent(DonorListEvent.EndDateChanged(DateTimeUtils.endOfDay(millis)))
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectorFilterDropdown(
    collectors: List<String>,
    selectedCollector: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCollectorSelected: (String?) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = selectedCollector.orEmpty(),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text(text = stringResource(R.string.collector_name)) },
            placeholder = { Text(text = stringResource(R.string.clear_filters)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = TextFieldShape,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.clear_filters)) },
                onClick = { onCollectorSelected(null) },
            )
            collectors.forEach { collector ->
                DropdownMenuItem(
                    text = { Text(text = collector) },
                    onClick = { onCollectorSelected(collector) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentModeFilterDropdown(
    selectedMode: PaymentMode?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModeSelected: (PaymentMode?) -> Unit,
) {
    val displayValue = when (selectedMode) {
        PaymentMode.CASH -> stringResource(R.string.payment_cash)
        PaymentMode.UPI -> stringResource(R.string.payment_upi)
        null -> ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text(text = stringResource(R.string.payment_mode)) },
            placeholder = { Text(text = stringResource(R.string.clear_filters)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = TextFieldShape,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.clear_filters)) },
                onClick = { onModeSelected(null) },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.payment_cash)) },
                onClick = { onModeSelected(PaymentMode.CASH) },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.payment_upi)) },
                onClick = { onModeSelected(PaymentMode.UPI) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterDialog(
    initialMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis ?: System.currentTimeMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let(onConfirm)
                },
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    selectedSort: DonationSort,
    onSortSelected: (DonationSort) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.sort),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            DonationSort.entries.forEach { sort ->
                SortOptionRow(
                    label = sortLabel(sort),
                    selected = selectedSort == sort,
                    onClick = { onSortSelected(sort) },
                )
                if (sort != DonationSort.entries.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SortOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun sortLabel(sort: DonationSort): String = when (sort) {
    DonationSort.LATEST -> stringResource(R.string.sort_latest)
    DonationSort.OLDEST -> stringResource(R.string.sort_oldest)
    DonationSort.HIGHEST_AMOUNT -> stringResource(R.string.sort_highest)
    DonationSort.LOWEST_AMOUNT -> stringResource(R.string.sort_lowest)
    DonationSort.ALPHABETICAL -> stringResource(R.string.sort_alphabetical)
}
