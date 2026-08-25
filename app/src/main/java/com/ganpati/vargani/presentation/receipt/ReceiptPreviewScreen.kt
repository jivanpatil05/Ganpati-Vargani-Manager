package com.ganpati.vargani.presentation.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.theme.BadgeShape
import com.ganpati.vargani.core.theme.BrandGradientMid
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.GoldAccent
import com.ganpati.vargani.core.theme.OrangePrimary
import com.ganpati.vargani.core.theme.OrangePrimaryDark
import com.ganpati.vargani.core.theme.VarganiThemeExtras
import com.ganpati.vargani.core.utils.CurrencyUtils
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.core.utils.FileShareUtils
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.PaymentMode

@Composable
fun ReceiptPreviewRoute(
    donationId: Long,
    onBack: () -> Unit,
    viewModel: ReceiptPreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.ShareFile -> {
                    FileShareUtils.shareFile(
                        context = context,
                        file = event.file,
                        mimeType = event.mimeType,
                        chooserTitle = context.getString(R.string.share_receipt),
                    )
                }
                is UiEvent.PrintPdf,
                is UiEvent.SaveToDevice,
                UiEvent.NavigateBack,
                UiEvent.LoggedOut -> Unit
            }
        }
    }

    ReceiptPreviewScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onShare = viewModel::onShare,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptPreviewScreen(
    uiState: ReceiptPreviewUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.receipt_preview),
                onNavigateBack = onBack,
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
                ReceiptPreviewContent(
                    donation = uiState.donation,
                    organizationName = uiState.settings.organizationName,
                    organizationAddress = uiState.settings.organizationAddress,
                    isGenerating = uiState.isGenerating,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onShare = onShare,
                )
            }
        }
    }
}

@Composable
private fun ReceiptPreviewContent(
    donation: Donation,
    organizationName: String,
    organizationAddress: String,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
    onShare: () -> Unit,
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
    val orgName = organizationName.ifBlank {
        stringResource(R.string.organization_name)
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(OrangePrimaryDark, OrangePrimary, BrandGradientMid),
                            ),
                        )
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "ॐ",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = orgName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.receipt_preview),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f),
                        )
                        if (organizationAddress.isNotBlank()) {
                            Text(
                                text = organizationAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(GoldAccent),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ReceiptMetaRow(
                        label = stringResource(R.string.receipt_number),
                        value = donation.receiptNo,
                    )
                    ReceiptMetaRow(
                        label = stringResource(R.string.donor_name),
                        value = donation.name,
                    )
                    if (donation.mobile.isNotBlank()) {
                        ReceiptMetaRow(
                            label = stringResource(R.string.mobile_number),
                            value = donation.mobile,
                        )
                    }
                    if (donation.address.isNotBlank()) {
                        ReceiptMetaRow(
                            label = stringResource(R.string.address),
                            value = donation.address,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        color = OrangePrimary.copy(alpha = 0.08f),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.amount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = CurrencyUtils.format(donation.amount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimaryDark,
                            )
                            Surface(
                                shape = BadgeShape,
                                color = paymentColor.copy(alpha = 0.14f),
                            ) {
                                Text(
                                    text = paymentLabel,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = paymentColor,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ReceiptMetaRow(
                        label = stringResource(R.string.collector_name),
                        value = donation.collector,
                    )
                    ReceiptMetaRow(
                        label = stringResource(R.string.date),
                        value = "${DateTimeUtils.formatDate(donation.dateEpochMillis)}  " +
                            DateTimeUtils.formatTime(donation.timeEpochMillis),
                    )
                    if (donation.notes.isNotBlank()) {
                        ReceiptMetaRow(
                            label = stringResource(R.string.notes),
                            value = donation.notes,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Thank you for your generous contribution",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Generated offline by ${stringResource(R.string.app_name)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        PrimaryButton(
            text = stringResource(R.string.share_receipt),
            onClick = onShare,
            enabled = !isGenerating,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ReceiptMetaRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(0.42f)
                .padding(end = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
        )
    }
}
