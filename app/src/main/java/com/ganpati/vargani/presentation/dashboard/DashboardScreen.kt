package com.ganpati.vargani.presentation.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.AnimatedCounter
import com.ganpati.vargani.core.components.DonationCard
import com.ganpati.vargani.core.components.EmptyStateView
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.SectionHeader
import com.ganpati.vargani.core.components.StatCard
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.CashGreen
import com.ganpati.vargani.core.theme.ErrorRed
import com.ganpati.vargani.core.theme.GoldAccent
import com.ganpati.vargani.core.theme.OrangePrimary
import com.ganpati.vargani.core.theme.OrangePrimaryDark
import com.ganpati.vargani.core.theme.SoftBlue
import com.ganpati.vargani.core.theme.SoftGreen
import com.ganpati.vargani.core.theme.UpiBlue
import com.ganpati.vargani.core.theme.VarganiTheme
import com.ganpati.vargani.domain.model.DashboardStats
import com.ganpati.vargani.presentation.expense.ExpenseListRoute

@Composable
fun DashboardRoute(
    onAddDonation: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenDonors: () -> Unit,
    onOpenManageUsers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDonation: (Long) -> Unit,
    onOpenExpense: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onExport: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val section by viewModel.section.collectAsStateWithLifecycle()

    BackHandler(enabled = section != HomeSection.Incoming) {
        viewModel.selectSection(HomeSection.Incoming)
    }

    DashboardScreen(
        uiState = uiState,
        section = section,
        onSectionChange = viewModel::selectSection,
        onAddDonation = onAddDonation,
        onAddExpense = onAddExpense,
        onOpenDonors = onOpenDonors,
        onOpenManageUsers = onOpenManageUsers,
        onOpenSettings = onOpenSettings,
        onOpenProfile = onOpenProfile,
        onOpenDonation = onOpenDonation,
        onOpenExpense = onOpenExpense,
        onOpenHistory = onOpenHistory,
        onExport = onExport,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DashboardScreen(
    uiState: DashboardUiState,
    section: HomeSection,
    onSectionChange: (HomeSection) -> Unit,
    onAddDonation: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenDonors: () -> Unit,
    onOpenManageUsers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDonation: (Long) -> Unit,
    onOpenExpense: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val fabScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fab_scale",
    )
    val isOutgoing = section == HomeSection.Outgoing
    val showFab = uiState.canWrite

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.dashboard_title),
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.nav_profile),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = if (isOutgoing) onAddExpense else onAddDonation,
                    modifier = Modifier.scale(fabScale),
                    shape = CardShape,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(
                                if (isOutgoing) R.string.cd_add_expense else R.string.cd_add_donation,
                            ),
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(
                                if (isOutgoing) R.string.action_add_expense else R.string.action_add_donation,
                            ),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    containerColor = if (isOutgoing) {
                        ErrorRed
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isOutgoing) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HomeSectionSwitcher(
                section = section,
                onSectionChange = onSectionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when (section) {
                HomeSection.Incoming -> IncomingDashboardContent(
                    uiState = uiState,
                    onOpenDonors = onOpenDonors,
                    onOpenManageUsers = onOpenManageUsers,
                    onOpenDonation = onOpenDonation,
                    onOpenHistory = onOpenHistory,
                    onExport = onExport,
                )
                HomeSection.Outgoing -> ExpenseListRoute(
                    onOpenSettings = onOpenSettings,
                    onAddExpense = onAddExpense,
                    onOpenExpense = onOpenExpense,
                    showTopBar = false,
                    embedded = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSectionSwitcher(
    section: HomeSection,
    onSectionChange: (HomeSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(HomeSection.Incoming, HomeSection.Outgoing)
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = section == option,
                onClick = { onSectionChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(
                    text = stringResource(
                        when (option) {
                            HomeSection.Incoming -> R.string.section_incoming
                            HomeSection.Outgoing -> R.string.section_outgoing
                        },
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncomingDashboardContent(
    uiState: DashboardUiState,
    onOpenDonors: () -> Unit,
    onOpenManageUsers: () -> Unit,
    onOpenDonation: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onExport: () -> Unit,
) {
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LoadingView(showShimmerPlaceholder = true)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardStatsGrid(stats = uiState.stats)
        }

        item {
            QuickActionsRow(
                isAdmin = uiState.isAdmin,
                onOpenManageUsers = onOpenManageUsers,
                onOpenHistory = onOpenHistory,
                onExport = onExport,
                onOpenDonors = onOpenDonors,
            )
        }

        item {
            SectionHeader(
                title = stringResource(R.string.recent_donations),
                actionLabel = stringResource(R.string.see_all),
                onActionClick = onOpenDonors,
            )
        }

        if (uiState.recent.isEmpty()) {
            item {
                EmptyStateView(
                    title = stringResource(R.string.no_donations),
                    subtitle = stringResource(R.string.no_donations_hint),
                    icon = Icons.Outlined.Inbox,
                )
            }
        } else {
            items(
                items = uiState.recent,
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
                    onClick = { onOpenDonation(donation.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardStatsGrid(
    stats: DashboardStats,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 2,
    ) {
        val cardModifier = Modifier.fillMaxWidth(0.48f)

        AnimatedAmountStatCard(
            title = stringResource(R.string.total_collection),
            amount = stats.totalCollection,
            icon = Icons.Default.TrendingUp,
            modifier = cardModifier,
        )
        AnimatedAmountStatCard(
            title = stringResource(R.string.today_collection),
            amount = stats.todayCollection,
            icon = Icons.Default.CalendarToday,
            modifier = cardModifier,
        )
        StatCard(
            title = stringResource(R.string.total_donors),
            value = stats.totalDonors.toString(),
            icon = Icons.Default.People,
            modifier = cardModifier,
        )
        AnimatedAmountStatCard(
            title = stringResource(R.string.cash_collection),
            amount = stats.cashCollection,
            icon = Icons.Default.Money,
            iconTint = CashGreen,
            iconBackground = SoftGreen.copy(alpha = 0.7f),
            modifier = cardModifier,
        )
        AnimatedAmountStatCard(
            title = stringResource(R.string.upi_collection),
            amount = stats.upiCollection,
            icon = Icons.Default.Payments,
            iconTint = UpiBlue,
            iconBackground = SoftBlue.copy(alpha = 0.7f),
            modifier = cardModifier,
        )
    }
}

@Composable
private fun AnimatedAmountStatCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
) {
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconTint,
                    )
                }
            }
            AnimatedCounter(
                targetValue = amount,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    isAdmin: Boolean,
    onOpenManageUsers: () -> Unit,
    onOpenHistory: () -> Unit,
    onExport: () -> Unit,
    onOpenDonors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(title = stringResource(R.string.quick_actions))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isAdmin) {
                QuickActionTile(
                    title = stringResource(R.string.action_add_viewer),
                    subtitle = stringResource(R.string.quick_action_add_viewer_hint),
                    icon = Icons.Default.PersonAdd,
                    accent = OrangePrimary,
                    accentEnd = OrangePrimaryDark,
                    onClick = onOpenManageUsers,
                    modifier = Modifier.weight(1f),
                )
            }
            QuickActionTile(
                title = stringResource(R.string.nav_donors),
                subtitle = stringResource(R.string.quick_action_donors_hint),
                icon = Icons.Default.Groups,
                accent = UpiBlue,
                accentEnd = Color(0xFF0D47A1),
                onClick = onOpenDonors,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionTile(
                title = stringResource(R.string.action_history),
                subtitle = stringResource(R.string.quick_action_history_hint),
                icon = Icons.Default.History,
                accent = CashGreen,
                accentEnd = Color(0xFF1B5E20),
                onClick = onOpenHistory,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                title = stringResource(R.string.action_export),
                subtitle = stringResource(R.string.quick_action_export_hint),
                icon = Icons.Default.FileDownload,
                accent = GoldAccent,
                accentEnd = Color(0xFFF57C00),
                onClick = onExport,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    accentEnd: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "quick_action_press",
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .height(124.dp),
        shape = CardShape,
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = 0.18f),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Soft corner glow
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(72.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(accent, accentEnd),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    VarganiTheme {
        DashboardScreen(
            uiState = DashboardUiState(isLoading = false),
            section = HomeSection.Incoming,
            onSectionChange = {},
            onAddDonation = {},
            onAddExpense = {},
            onOpenDonors = {},
            onOpenManageUsers = {},
            onOpenSettings = {},
            onOpenProfile = {},
            onOpenDonation = {},
            onOpenExpense = {},
            onOpenHistory = {},
            onExport = {},
        )
    }
}
