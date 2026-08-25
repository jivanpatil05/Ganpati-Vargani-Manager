package com.ganpati.vargani.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.ChartCard
import com.ganpati.vargani.core.components.DonationCard
import com.ganpati.vargani.core.components.EmptyStateView
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.SectionHeader
import com.ganpati.vargani.core.components.SecondaryButton
import com.ganpati.vargani.core.components.StatCard
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.components.charts.BarChart
import com.ganpati.vargani.core.components.charts.ChartEntry
import com.ganpati.vargani.core.components.charts.PieChart
import com.ganpati.vargani.core.utils.CurrencyUtils
import com.ganpati.vargani.core.utils.FileShareUtils
import com.ganpati.vargani.core.utils.UiEvent

@Composable
fun ReportsRoute(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.ShareFile -> {
                    try {
                        FileShareUtils.shareFile(
                            context = context,
                            file = event.file,
                            mimeType = event.mimeType,
                            chooserTitle = context.getString(R.string.action_export),
                        )
                        snackbarHostState.showSnackbar(context.getString(R.string.export_success))
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar(context.getString(R.string.error_generic))
                    } finally {
                        viewModel.onShareIntentOpened()
                    }
                }
                is UiEvent.PrintPdf -> Unit
                is UiEvent.SaveToDevice -> Unit
                UiEvent.NavigateBack -> onBack()
                UiEvent.LoggedOut -> Unit
            }
        }
    }

    ReportsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onExportExcel = viewModel::exportExcel,
        onExportPdf = viewModel::exportPdf,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReportsScreen(
    uiState: ReportsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.reports_title),
                onNavigateBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                LoadingView(showShimmerPlaceholder = true)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ReportStatsRow(
                    average = uiState.summary?.averageDonation ?: 0.0,
                    highest = uiState.summary?.highestDonation ?: 0.0,
                    lowest = uiState.summary?.lowestDonation ?: 0.0,
                )
            }

            item {
                CollectionChartSection(
                    title = stringResource(R.string.daily_collection),
                    entries = uiState.daily,
                )
            }

            item {
                CollectionChartSection(
                    title = stringResource(R.string.weekly_report),
                    entries = uiState.weekly,
                )
            }

            item {
                ChartCard(title = stringResource(R.string.cash_vs_upi)) {
                    if (uiState.paymentModes.isEmpty() || uiState.paymentModes.all { it.value == 0.0 }) {
                        EmptyStateView(
                            title = stringResource(R.string.no_donations),
                            subtitle = stringResource(R.string.no_donations_hint),
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        PieChart(entries = uiState.paymentModes)
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.top_donors))
            }

            if (uiState.topDonors.isEmpty()) {
                item {
                    EmptyStateView(
                        title = stringResource(R.string.no_donations),
                        subtitle = stringResource(R.string.no_donations_hint),
                        icon = Icons.Outlined.Inbox,
                    )
                }
            } else {
                items(
                    items = uiState.topDonors,
                    key = { it.id },
                ) { donation ->
                    DonationCard(
                        receiptNo = donation.receiptNo,
                        name = donation.name,
                        amount = donation.amount,
                        paymentMode = donation.paymentMode,
                        collector = donation.collector,
                        dateEpochMillis = donation.dateEpochMillis,
                        mobile = donation.mobile,
                    )
                }
            }

            item {
                ExportActionsSection(
                    isExportingExcel = uiState.isExportingExcel,
                    isExportingPdf = uiState.isExportingPdf,
                    onExportExcel = onExportExcel,
                    onExportPdf = onExportPdf,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportStatsRow(
    average: Double,
    highest: Double,
    lowest: Double,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 2,
    ) {
        val cardModifier = Modifier.fillMaxWidth(0.48f)
        StatCard(
            title = stringResource(R.string.average_donation),
            value = CurrencyUtils.format(average),
            icon = Icons.Default.Assessment,
            modifier = cardModifier,
        )
        StatCard(
            title = stringResource(R.string.highest_donation),
            value = CurrencyUtils.format(highest),
            icon = Icons.Default.TrendingUp,
            modifier = cardModifier,
        )
        StatCard(
            title = stringResource(R.string.lowest_donation),
            value = CurrencyUtils.format(lowest),
            icon = Icons.Default.TrendingDown,
            modifier = cardModifier,
        )
    }
}

@Composable
private fun CollectionChartSection(
    title: String,
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
) {
    ChartCard(
        title = title,
        modifier = modifier,
    ) {
        if (entries.isEmpty() || entries.all { it.value == 0.0 }) {
            EmptyStateView(
                title = stringResource(R.string.no_donations),
                subtitle = stringResource(R.string.no_donations_hint),
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            BarChart(entries = entries)
        }
    }
}

@Composable
private fun ExportActionsSection(
    isExportingExcel: Boolean,
    isExportingPdf: Boolean,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExporting = isExportingExcel || isExportingPdf
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(title = stringResource(R.string.action_export))
        PrimaryButton(
            text = stringResource(R.string.export_excel),
            onClick = onExportExcel,
            enabled = !isExporting,
            loading = isExportingExcel,
        )
        SecondaryButton(
            text = stringResource(R.string.export_pdf),
            onClick = onExportPdf,
            enabled = !isExporting,
            loading = isExportingPdf,
        )
    }
}
