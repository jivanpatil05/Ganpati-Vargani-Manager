package com.ganpati.vargani.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.EmptyStateView
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.RemainingBalanceCard
import com.ganpati.vargani.core.components.StatCard
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.CashGreen
import com.ganpati.vargani.core.theme.ErrorRed
import com.ganpati.vargani.core.theme.VarganiMotion
import com.ganpati.vargani.core.utils.CurrencyUtils
import com.ganpati.vargani.core.utils.DateTimeUtils

@Composable
fun AmountBreakdownRoute(
    onBack: () -> Unit = {},
    onOpenDonation: (Long) -> Unit,
    onOpenExpense: (Long) -> Unit,
    showTopBar: Boolean = true,
    embedded: Boolean = false,
    showRemainingStat: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: AmountBreakdownViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AmountBreakdownScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenItem = { item ->
            when (item.type) {
                BreakdownType.Incoming -> onOpenDonation(item.id)
                BreakdownType.Outgoing -> onOpenExpense(item.id)
            }
        },
        showTopBar = showTopBar,
        embedded = embedded,
        showRemainingStat = showRemainingStat,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountBreakdownScreen(
    uiState: AmountBreakdownUiState,
    onBack: () -> Unit,
    onOpenItem: (BreakdownItem) -> Unit,
    showTopBar: Boolean = true,
    embedded: Boolean = false,
    showRemainingStat: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (embedded) {
        AmountBreakdownBody(
            uiState = uiState,
            onOpenItem = onOpenItem,
            showRemainingStat = showRemainingStat,
            contentPadding = PaddingValues(0.dp),
            modifier = modifier,
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                if (showTopBar) {
                    VarganiTopAppBar(
                        title = stringResource(R.string.section_history),
                        onNavigateBack = onBack,
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            AmountBreakdownBody(
                uiState = uiState,
                onOpenItem = onOpenItem,
                showRemainingStat = showRemainingStat,
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun AmountBreakdownBody(
    uiState: AmountBreakdownUiState,
    onOpenItem: (BreakdownItem) -> Unit,
    showRemainingStat: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            LoadingView()
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showRemainingStat) {
                item {
                    RemainingBalanceCard(
                        amount = uiState.remaining,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        title = stringResource(R.string.total_incoming),
                        value = CurrencyUtils.format(uiState.totalIncoming),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.SouthWest,
                        iconTint = CashGreen,
                        iconBackground = CashGreen.copy(alpha = 0.12f),
                    )
                    StatCard(
                        title = stringResource(R.string.total_outgoing),
                        value = CurrencyUtils.format(uiState.totalOutgoing),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.NorthEast,
                        iconTint = ErrorRed,
                        iconBackground = ErrorRed.copy(alpha = 0.12f),
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.profile_breakdown_all),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (uiState.items.isEmpty()) {
                item {
                    EmptyStateView(
                        title = stringResource(R.string.profile_breakdown_empty),
                        subtitle = stringResource(R.string.profile_breakdown_empty_hint),
                        icon = Icons.Outlined.Inbox,
                    )
                }
            } else {
                item {
                    BreakdownItemsCard(
                        items = uiState.items,
                        onOpenItem = onOpenItem,
                    )
                }
            }
        }
    }
}

@Composable
fun BreakdownItemsCard(
    items: List<BreakdownItem>,
    onOpenItem: (BreakdownItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = VarganiMotion.CardElevation),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                BreakdownItemRow(
                    item = item,
                    onClick = { onOpenItem(item) },
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
fun BreakdownItemRow(
    item: BreakdownItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIncoming = item.type == BreakdownType.Incoming
    val tint = if (isIncoming) CashGreen else ErrorRed
    val amountPrefix = if (isIncoming) "+" else "-"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (isIncoming) Icons.Outlined.SouthWest else Icons.Outlined.NorthEast,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = buildString {
                    append(
                        if (isIncoming) {
                            stringResource(R.string.section_incoming)
                        } else {
                            stringResource(R.string.section_outgoing)
                        },
                    )
                    if (item.subtitle.isNotBlank()) {
                        append(" · ")
                        append(item.subtitle)
                    }
                    append(" · ")
                    append(DateTimeUtils.formatDate(item.dateEpochMillis))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "$amountPrefix${CurrencyUtils.format(item.amount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}
