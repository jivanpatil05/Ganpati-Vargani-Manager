package com.ganpati.vargani.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.ConfirmationDialog
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.SecondaryButton
import com.ganpati.vargani.core.components.SectionHeader
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.DialogShape
import com.ganpati.vargani.core.theme.GoldAccent
import com.ganpati.vargani.core.theme.OrangePrimary
import com.ganpati.vargani.core.theme.OrangePrimaryDark
import com.ganpati.vargani.core.theme.UpiBlue
import com.ganpati.vargani.core.utils.FileDownloadUtils
import com.ganpati.vargani.core.utils.FileShareUtils
import com.ganpati.vargani.core.utils.UiEvent
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BackupRoute(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingSaveFile by remember { mutableStateOf<File?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                viewModel.restoreBackup(stream)
            } ?: run {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.restore_failed))
                }
            }
        }.onFailure {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.restore_failed))
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val source = pendingSaveFile
        pendingSaveFile = null
        if (uri == null || source == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = FileDownloadUtils.copyToUri(context, source, uri)
            snackbarHostState.showSnackbar(
                message = context.getString(
                    if (ok) R.string.file_saved_success else R.string.error_generic,
                ),
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is UiEvent.ShareFile -> {
                    FileShareUtils.shareFile(
                        context = context,
                        file = event.file,
                        mimeType = event.mimeType,
                        chooserTitle = context.getString(R.string.share),
                    )
                }
                is UiEvent.SaveToDevice -> {
                    val downloaded = FileDownloadUtils.saveToDownloads(
                        context = context,
                        source = event.file,
                        displayName = event.suggestedFileName,
                        mimeType = event.mimeType,
                    )
                    if (downloaded != null) {
                        snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.file_saved_to_downloads,
                                event.suggestedFileName,
                            ),
                        )
                    } else {
                        pendingSaveFile = event.file
                        saveLauncher.launch(event.suggestedFileName)
                    }
                }
                else -> Unit
            }
        }
    }

    BackupScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onBackup = viewModel::createBackup,
        onRestore = {
            restoreLauncher.launch(
                arrayOf(
                    "application/zip",
                    "application/json",
                    "application/octet-stream",
                    "*/*",
                ),
            )
        },
        onExportExcel = viewModel::exportExcelFile,
        onExportPdf = viewModel::exportPdfReport,
        onDismissExportOptions = viewModel::dismissExportOptions,
        onDownloadExport = { viewModel.confirmExport(DataExportDestination.DOWNLOAD) },
        onShareExport = { viewModel.confirmExport(DataExportDestination.SHARE) },
        onReset = viewModel::showResetConfirmation,
        onDismissReset = viewModel::dismissResetConfirmation,
        onConfirmReset = viewModel::confirmReset,
    )
}

@Composable
fun BackupScreen(
    uiState: BackupUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onDismissExportOptions: () -> Unit,
    onDownloadExport: () -> Unit,
    onShareExport: () -> Unit,
    onReset: () -> Unit,
    onDismissReset: () -> Unit,
    onConfirmReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.showResetConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_reset_title),
            message = stringResource(R.string.confirm_reset_message),
            onConfirm = onConfirmReset,
            onDismiss = onDismissReset,
        )
    }

    if (uiState.pendingExportKind != null) {
        ExportActionDialog(
            kind = uiState.pendingExportKind,
            onDownload = onDownloadExport,
            onShare = onShareExport,
            onDismiss = onDismissExportOptions,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.backup_page_title),
                onNavigateBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.backup_page_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DataSection(title = stringResource(R.string.backup_database)) {
                PrimaryButton(
                    text = stringResource(R.string.backup_database),
                    onClick = onBackup,
                    enabled = !uiState.isProcessing,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(R.string.restore_database),
                    onClick = onRestore,
                    enabled = !uiState.isProcessing,
                )
            }

            DataSection(title = stringResource(R.string.action_export)) {
                SecondaryButton(
                    text = stringResource(R.string.export_excel),
                    onClick = onExportExcel,
                    enabled = !uiState.isProcessing,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(R.string.export_pdf),
                    onClick = onExportPdf,
                    enabled = !uiState.isProcessing,
                )
            }

            DataSection(title = stringResource(R.string.reset_data)) {
                SecondaryButton(
                    text = stringResource(R.string.reset_data),
                    onClick = onReset,
                    enabled = !uiState.isProcessing,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DataSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        SectionHeader(title = title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ExportActionDialog(
    kind: DataExportKind,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val actionTitle = when (kind) {
        DataExportKind.BACKUP -> stringResource(R.string.backup_database)
        DataExportKind.EXCEL -> stringResource(R.string.export_excel)
        DataExportKind.PDF -> stringResource(R.string.export_pdf)
    }
    val headerIcon = when (kind) {
        DataExportKind.BACKUP -> Icons.Outlined.FolderZip
        DataExportKind.EXCEL -> Icons.Outlined.TableChart
        DataExportKind.PDF -> Icons.Outlined.PictureAsPdf
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = DialogShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(OrangePrimary, OrangePrimaryDark, GoldAccent),
                            ),
                        )
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White.copy(alpha = 0.92f),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.export_choose_action_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.export_choose_action_message, actionTitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExportActionOption(
                        icon = Icons.Outlined.Download,
                        iconTint = OrangePrimaryDark,
                        iconBackground = OrangePrimary.copy(alpha = 0.14f),
                        title = stringResource(R.string.action_download),
                        subtitle = stringResource(R.string.action_download_hint),
                        onClick = onDownload,
                    )
                    ExportActionOption(
                        icon = Icons.Outlined.Share,
                        iconTint = UpiBlue,
                        iconBackground = UpiBlue.copy(alpha = 0.12f),
                        title = stringResource(R.string.share),
                        subtitle = stringResource(R.string.action_share_hint),
                        onClick = onShare,
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportActionOption(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
