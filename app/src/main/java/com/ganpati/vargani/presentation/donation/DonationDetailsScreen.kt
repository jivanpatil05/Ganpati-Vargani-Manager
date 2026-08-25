package com.ganpati.vargani.presentation.donation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.ConfirmationDialog
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.SecondaryButton
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.VarganiThemeExtras
import com.ganpati.vargani.core.utils.CurrencyUtils
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.core.utils.FileShareUtils
import com.ganpati.vargani.core.utils.PrintUtils
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.PaymentMode

@Composable
fun DonationDetailsRoute(
    donationId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onReceipt: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: DonationDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        event.action?.invoke()
                    }
                }
                is UiEvent.ShareFile -> {
                    FileShareUtils.shareFile(
                        context = context,
                        file = event.file,
                        mimeType = event.mimeType,
                        chooserTitle = context.getString(R.string.share_receipt),
                    )
                }
                is UiEvent.PrintPdf -> {
                    PrintUtils.printPdf(context, event.file, event.jobName)
                }
                is UiEvent.SaveToDevice -> Unit
                UiEvent.NavigateBack -> onBack()
                UiEvent.LoggedOut -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DonationDetailsEffect.Deleted -> onDeleted()
                is DonationDetailsEffect.OpenReceipt -> onReceipt()
            }
        }
    }

    DonationDetailsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = onEdit,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationDetailsScreen(
    uiState: DonationDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEvent: (DonationDetailsEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.donation_details),
                onNavigateBack = onBack,
                actions = {
                    if (uiState.canWrite) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.edit),
                            )
                        }
                        IconButton(onClick = { onEvent(DonationDetailsEvent.ShowDeleteDialog) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingView()
                }
            }
            uiState.donation == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.errorMessage ?: stringResource(R.string.error_generic),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            else -> {
                DonationDetailsContent(
                    donation = uiState.donation,
                    isGeneratingReceipt = uiState.isGeneratingReceipt,
                    canWrite = uiState.canWrite,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onEdit = onEdit,
                    onEvent = onEvent,
                )
            }
        }
    }

    if (uiState.showDeleteDialog && uiState.canWrite) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            onConfirm = { onEvent(DonationDetailsEvent.ConfirmDelete) },
            onDismiss = { onEvent(DonationDetailsEvent.DismissDeleteDialog) },
            confirmText = stringResource(R.string.delete),
        )
    }
}

@Composable
private fun DonationDetailsContent(
    donation: Donation,
    isGeneratingReceipt: Boolean,
    canWrite: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onEvent: (DonationDetailsEvent) -> Unit,
) {
    val extendedColors = VarganiThemeExtras.extendedColors
    val paymentLabel = when (donation.paymentMode) {
        PaymentMode.CASH -> stringResource(R.string.payment_cash)
        PaymentMode.UPI -> stringResource(R.string.payment_upi)
    }
    val paymentColor = when (donation.paymentMode) {
        PaymentMode.CASH -> extendedColors.cash
        PaymentMode.UPI -> extendedColors.upi
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = donation.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = CurrencyUtils.format(donation.amount),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CardShape,
                        color = paymentColor.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = paymentLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = paymentColor,
                        )
                    }
                    Text(
                        text = donation.receiptNo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DetailRow(
                    label = stringResource(R.string.mobile_number),
                    value = donation.mobile,
                )
                if (donation.address.isNotBlank()) {
                    DetailRow(
                        label = stringResource(R.string.address),
                        value = donation.address,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(
                    label = stringResource(R.string.collector_name),
                    value = donation.collector,
                )
                DetailRow(
                    label = stringResource(R.string.date),
                    value = DateTimeUtils.formatDate(donation.dateEpochMillis),
                )
                DetailRow(
                    label = stringResource(R.string.time),
                    value = DateTimeUtils.formatTime(donation.timeEpochMillis),
                )
                if (donation.notes.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow(
                        label = stringResource(R.string.notes),
                        value = donation.notes,
                    )
                }
            }
        }

        PrimaryButton(
            text = stringResource(R.string.generate_receipt),
            onClick = { onEvent(DonationDetailsEvent.GenerateReceipt) },
            enabled = !isGeneratingReceipt,
        )

        SecondaryButton(
            text = stringResource(R.string.share_receipt),
            onClick = { onEvent(DonationDetailsEvent.ShareReceipt) },
            enabled = !isGeneratingReceipt,
        )

        SecondaryButton(
            text = stringResource(R.string.print_receipt),
            onClick = { onEvent(DonationDetailsEvent.PrintReceipt) },
            enabled = !isGeneratingReceipt,
        )

        if (canWrite) {
            SecondaryButton(
                text = stringResource(R.string.edit),
                onClick = onEdit,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
