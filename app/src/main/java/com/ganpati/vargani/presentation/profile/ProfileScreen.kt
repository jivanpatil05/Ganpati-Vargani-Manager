package com.ganpati.vargani.presentation.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.SecondaryButton
import com.ganpati.vargani.core.components.SectionHeader
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.components.bringIntoViewWhenFocused
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.OrangePrimary
import com.ganpati.vargani.core.theme.OrangePrimaryDark
import com.ganpati.vargani.core.theme.VarganiMotion
import com.ganpati.vargani.core.utils.PaymentQrStore
import com.ganpati.vargani.core.utils.UiEvent
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cropFailedMessage = stringResource(R.string.payment_qr_crop_failed)

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        when {
            result.isSuccessful -> {
                val croppedUri = result.uriContent ?: return@rememberLauncherForActivityResult
                viewModel.onQrImagePicked(croppedUri)
            }
            result is CropImage.CancelledResult -> Unit
            else -> {
                if (result.error != null) {
                    scope.launch {
                        snackbarHostState.showSnackbar(cropFailedMessage)
                    }
                }
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val outputUri = PaymentQrStore.createCropOutputUri(context)
        cropImageLauncher.launch(
            CropImageContractOptions(
                uri = uri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    fixAspectRatio = true,
                    outputCompressFormat = Bitmap.CompressFormat.JPEG,
                    outputCompressQuality = 95,
                    customOutputUri = outputUri,
                    activityTitle = context.getString(R.string.payment_qr_crop_title),
                    cropMenuCropButtonTitle = context.getString(R.string.payment_qr_crop_done),
                ),
            ),
        )
    }

    ProfileScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onOrganizationNameChanged = viewModel::onOrganizationNameChanged,
        onOrganizationAddressChanged = viewModel::onOrganizationAddressChanged,
        onSaveOrganizationDetails = { viewModel.saveOrganizationDetails() },
        onUpiIdChanged = viewModel::onUpiIdChanged,
        onBankNameChanged = viewModel::onBankNameChanged,
        onAccountNumberChanged = viewModel::onAccountNumberChanged,
        onIfscChanged = viewModel::onIfscChanged,
        onAccountHolderChanged = viewModel::onAccountHolderChanged,
        onUploadQr = { pickImageLauncher.launch("image/*") },
        onRemoveQr = viewModel::onRemoveQrImage,
        onSavePaymentDetails = { viewModel.savePaymentDetails() },
        onToggleViewersEnabled = viewModel::toggleViewersEnabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOrganizationNameChanged: (String) -> Unit,
    onOrganizationAddressChanged: (String) -> Unit,
    onSaveOrganizationDetails: () -> Unit,
    onUpiIdChanged: (String) -> Unit,
    onBankNameChanged: (String) -> Unit,
    onAccountNumberChanged: (String) -> Unit,
    onIfscChanged: (String) -> Unit,
    onAccountHolderChanged: (String) -> Unit,
    onUploadQr: () -> Unit,
    onRemoveQr: () -> Unit,
    onSavePaymentDetails: () -> Unit,
    onToggleViewersEnabled: () -> Unit,
) {
    Scaffold(
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.profile_title),
                onNavigateBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LoadingView()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeaderCard(
                name = uiState.name.ifBlank { stringResource(R.string.profile_guest) },
                mobile = uiState.mobile,
            )

            if (uiState.isAdmin) {
                ViewerAccessSection(
                    enabled = uiState.viewersEnabled,
                    isSaving = uiState.isSaving,
                    onToggle = onToggleViewersEnabled,
                )

                OrganizationSection(
                    organizationName = uiState.organizationName,
                    organizationAddress = uiState.organizationAddress,
                    isSaving = uiState.isSaving,
                    onOrganizationNameChanged = onOrganizationNameChanged,
                    onOrganizationAddressChanged = onOrganizationAddressChanged,
                    onSave = onSaveOrganizationDetails,
                )
            }

            PaymentDetailsSection(
                uiState = uiState,
                canWrite = uiState.isAdmin,
                onUpiIdChanged = onUpiIdChanged,
                onBankNameChanged = onBankNameChanged,
                onAccountNumberChanged = onAccountNumberChanged,
                onIfscChanged = onIfscChanged,
                onAccountHolderChanged = onAccountHolderChanged,
                onUploadQr = onUploadQr,
                onRemoveQr = onRemoveQr,
                onSavePaymentDetails = onSavePaymentDetails,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OrganizationSection(
    organizationName: String,
    organizationAddress: String,
    isSaving: Boolean,
    onOrganizationNameChanged: (String) -> Unit,
    onOrganizationAddressChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = stringResource(R.string.organization))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = VarganiMotion.CardElevation),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.organization_profile_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = organizationName,
                    onValueChange = onOrganizationNameChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewWhenFocused(),
                    label = { Text(stringResource(R.string.organization_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = organizationAddress,
                    onValueChange = onOrganizationAddressChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewWhenFocused(),
                    label = { Text(stringResource(R.string.organization_address)) },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                )
                PrimaryButton(
                    text = stringResource(R.string.organization_save),
                    onClick = onSave,
                    enabled = !isSaving,
                    loading = isSaving,
                )
            }
        }
    }
}

@Composable
private fun ViewerAccessSection(
    enabled: Boolean,
    isSaving: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = stringResource(R.string.viewer_access_section))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = VarganiMotion.CardElevation),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.viewer_access_toggle),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                if (enabled) {
                                    R.string.viewer_access_on_hint
                                } else {
                                    R.string.viewer_access_off_hint
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onToggle() },
                        enabled = !isSaving,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentDetailsSection(
    uiState: ProfileUiState,
    canWrite: Boolean,
    onUpiIdChanged: (String) -> Unit,
    onBankNameChanged: (String) -> Unit,
    onAccountNumberChanged: (String) -> Unit,
    onIfscChanged: (String) -> Unit,
    onAccountHolderChanged: (String) -> Unit,
    onUploadQr: () -> Unit,
    onRemoveQr: () -> Unit,
    onSavePaymentDetails: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.payment_account_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = stringResource(
                if (canWrite) R.string.payment_account_hint else R.string.payment_account_view_hint,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = VarganiMotion.CardElevation),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QrUploadBlock(
                    qrImagePath = uiState.qrImagePath,
                    canWrite = canWrite,
                    onUploadQr = onUploadQr,
                    onRemoveQr = onRemoveQr,
                )

                OutlinedTextField(
                    value = uiState.upiId,
                    onValueChange = onUpiIdChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewWhenFocused(),
                    label = { Text(stringResource(R.string.payment_upi_id)) },
                    placeholder = { Text(stringResource(R.string.payment_upi_id_hint)) },
                    singleLine = true,
                    readOnly = !canWrite,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = uiState.accountHolder,
                    onValueChange = onAccountHolderChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewWhenFocused(),
                    label = { Text(stringResource(R.string.payment_account_holder)) },
                    singleLine = true,
                    readOnly = !canWrite,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = uiState.bankName,
                    onValueChange = onBankNameChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewWhenFocused(),
                    label = { Text(stringResource(R.string.payment_bank_name)) },
                    singleLine = true,
                    readOnly = !canWrite,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = uiState.accountNumber,
                    onValueChange = onAccountNumberChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewWhenFocused(),
                    label = { Text(stringResource(R.string.payment_account_number)) },
                    singleLine = true,
                    readOnly = !canWrite,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = uiState.ifsc,
                    onValueChange = onIfscChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewWhenFocused(),
                    label = { Text(stringResource(R.string.payment_ifsc)) },
                    singleLine = true,
                    readOnly = !canWrite,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                )

                if (canWrite) {
                    PrimaryButton(
                        text = stringResource(R.string.payment_save_details),
                        onClick = onSavePaymentDetails,
                        enabled = !uiState.isSaving,
                        loading = uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun QrUploadBlock(
    qrImagePath: String,
    canWrite: Boolean,
    onUploadQr: () -> Unit,
    onRemoveQr: () -> Unit,
) {
    val qrBitmap = remember(qrImagePath) {
        if (qrImagePath.isBlank()) {
            null
        } else {
            runCatching {
                BitmapFactory.decodeFile(File(qrImagePath).absolutePath)
            }.getOrNull()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.payment_qr_code),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.payment_qr_code),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = stringResource(R.string.payment_qr_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (canWrite) {
                PrimaryButton(
                    text = stringResource(
                        if (qrBitmap == null) R.string.payment_upload_qr else R.string.payment_change_qr,
                    ),
                    onClick = onUploadQr,
                    modifier = Modifier.weight(1f),
                )
                if (qrBitmap != null) {
                    SecondaryButton(
                        text = stringResource(R.string.payment_remove_qr),
                        onClick = onRemoveQr,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    name: String,
    mobile: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = VarganiMotion.CardElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(OrangePrimary, OrangePrimaryDark),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (mobile.isNotBlank()) {
                    Text(
                        text = mobile,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.profile_account_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
