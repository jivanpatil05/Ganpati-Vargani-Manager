package com.ganpati.vargani.presentation.expense

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.EmptyStateView
import com.ganpati.vargani.core.components.ExpenseCard
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.SearchBarField
import com.ganpati.vargani.core.components.SectionHeader
import com.ganpati.vargani.core.components.StatCard
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.ErrorRed
import com.ganpati.vargani.core.utils.CurrencyUtils
import com.ganpati.vargani.domain.model.ExpenseStats

@Composable
fun ExpenseListRoute(
    onBack: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenExpense: (Long) -> Unit,
    showTopBar: Boolean = true,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseListScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onAddExpense = onAddExpense,
        onOpenExpense = onOpenExpense,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        showTopBar = showTopBar,
        embedded = embedded,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    uiState: ExpenseListUiState,
    onOpenSettings: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenExpense: (Long) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = true,
    embedded: Boolean = false,
) {
    if (embedded) {
        ExpenseListBody(
            uiState = uiState,
            onSearchQueryChanged = onSearchQueryChanged,
            onOpenExpense = onOpenExpense,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showTopBar) {
                VarganiTopAppBar(
                    title = stringResource(R.string.section_outgoing),
                    onNavigateBack = onBack,
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                            )
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (uiState.canWrite) {
                ExtendedFloatingActionButton(
                    onClick = onAddExpense,
                    shape = CardShape,
                    containerColor = ErrorRed,
                    contentColor = MaterialTheme.colorScheme.onError,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_add_expense),
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.action_add_expense),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LoadingView(showShimmerPlaceholder = true)
            }
            return@Scaffold
        }

        ExpenseListBody(
            uiState = uiState,
            onSearchQueryChanged = onSearchQueryChanged,
            onOpenExpense = onOpenExpense,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ExpenseListBody(
    uiState: ExpenseListUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOpenExpense: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (uiState.isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            LoadingView(showShimmerPlaceholder = true)
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ExpenseStatsRow(stats = uiState.stats) }

        item {
            SearchBarField(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                placeholder = stringResource(R.string.search_expenses),
            )
        }

        item {
            SectionHeader(title = stringResource(R.string.recent_expenses))
        }

        if (uiState.expenses.isEmpty()) {
            item {
                EmptyStateView(
                    title = if (uiState.searchQuery.isBlank()) {
                        stringResource(R.string.no_expenses)
                    } else {
                        stringResource(R.string.no_search_results)
                    },
                    subtitle = if (uiState.searchQuery.isBlank()) {
                        stringResource(R.string.no_expenses_hint)
                    } else {
                        stringResource(R.string.clear_filters)
                    },
                    icon = if (uiState.searchQuery.isBlank()) {
                        Icons.Outlined.Inbox
                    } else {
                        Icons.Outlined.SearchOff
                    },
                )
            }
        } else {
            items(uiState.expenses, key = { it.id }) { expense ->
                ExpenseCard(
                    title = expense.title,
                    category = expense.category,
                    amount = expense.amount,
                    paymentMode = expense.paymentMode,
                    paidBy = expense.paidBy,
                    dateEpochMillis = expense.dateEpochMillis,
                    onClick = { onOpenExpense(expense.id) },
                )
            }
        }
    }
}

@Composable
private fun ExpenseStatsRow(
    stats: ExpenseStats,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                title = stringResource(R.string.total_expenses),
                value = CurrencyUtils.format(stats.totalExpenses),
                modifier = Modifier.weight(1f),
                iconTint = ErrorRed,
                iconBackground = ErrorRed.copy(alpha = 0.12f),
            )
            StatCard(
                title = stringResource(R.string.today_expenses),
                value = CurrencyUtils.format(stats.todayExpenses),
                modifier = Modifier.weight(1f),
                iconTint = ErrorRed,
                iconBackground = ErrorRed.copy(alpha = 0.12f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                title = stringResource(R.string.cash_collection),
                value = CurrencyUtils.format(stats.cashTotal),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = stringResource(R.string.upi_collection),
                value = CurrencyUtils.format(stats.upiTotal),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
