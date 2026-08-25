package com.ganpati.vargani.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.ConfirmationDialog
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.SecondaryButton
import com.ganpati.vargani.core.components.SectionHeader
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.components.bringIntoViewWhenFocused
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.utils.LocaleHelper
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.AppLanguage

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is UiEvent.LoggedOut -> onLoggedOut()
                else -> Unit
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onToggleDarkMode = viewModel::toggleDarkMode,
        onToggleWhatsAppGroupNotify = viewModel::toggleWhatsAppGroupNotify,
        onLanguageSelected = { language ->
            viewModel.setLanguage(language)
            LocaleHelper.changeLanguage(context, language)
        },
        onReceiptPrefixChanged = viewModel::onReceiptPrefixChanged,
        onReceiptCounterChanged = viewModel::onReceiptCounterChanged,
        onShowPrivacyPolicy = viewModel::showPrivacyPolicy,
        onDismissPrivacyPolicy = viewModel::dismissPrivacyPolicy,
        onShowAbout = viewModel::showAbout,
        onDismissAbout = viewModel::dismissAbout,
        onLogout = viewModel::logout,
        onDismissLogout = viewModel::dismissLogoutConfirm,
        onConfirmLogout = viewModel::confirmLogout,
        onOpenProfile = onOpenProfile,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleWhatsAppGroupNotify: () -> Unit = {},
    onLanguageSelected: (AppLanguage) -> Unit,
    onReceiptPrefixChanged: (String) -> Unit,
    onReceiptCounterChanged: (String) -> Unit,
    onShowPrivacyPolicy: () -> Unit,
    onDismissPrivacyPolicy: () -> Unit,
    onShowAbout: () -> Unit,
    onDismissAbout: () -> Unit,
    onLogout: () -> Unit = {},
    onDismissLogout: () -> Unit = {},
    onConfirmLogout: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (uiState.showLogoutConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_logout_title),
            message = stringResource(R.string.confirm_logout_message),
            onConfirm = onConfirmLogout,
            onDismiss = onDismissLogout,
            confirmText = stringResource(R.string.auth_logout),
        )
    }

    if (uiState.showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = onDismissPrivacyPolicy,
            title = { Text(stringResource(R.string.privacy_policy)) },
            text = { Text(stringResource(R.string.privacy_policy_body)) },
            confirmButton = {
                TextButton(onClick = onDismissPrivacyPolicy) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    if (uiState.showAbout) {
        AlertDialog(
            onDismissRequest = onDismissAbout,
            title = { Text(stringResource(R.string.about)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.app_version) + ": ${uiState.appVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.developer_info) + ": " +
                            stringResource(R.string.developer_name),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissAbout) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.settings_title),
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
            SettingsSection(title = stringResource(R.string.language)) {
                LanguageSelector(
                    selected = uiState.language,
                    onSelected = onLanguageSelected,
                )
            }

            SettingsSection(title = stringResource(R.string.appearance)) {
                SettingsSwitchRow(
                    label = stringResource(R.string.dark_mode),
                    checked = uiState.darkMode,
                    onCheckedChange = { onToggleDarkMode() },
                )
            }

            if (uiState.isAdmin) {
                SettingsSection(title = stringResource(R.string.receipt_settings)) {
                    OutlinedTextField(
                        value = uiState.receiptPrefix,
                        onValueChange = onReceiptPrefixChanged,
                        label = { Text(stringResource(R.string.receipt_prefix)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewWhenFocused(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.receiptCounter.toString(),
                        onValueChange = onReceiptCounterChanged,
                        label = { Text(stringResource(R.string.receipt_starting_number)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewWhenFocused(),
                        singleLine = true,
                    )
                }

                SettingsSection(title = stringResource(R.string.whatsapp_notify_section)) {
                    SettingsSwitchRow(
                        label = stringResource(R.string.whatsapp_notify_on_save),
                        checked = uiState.whatsappGroupNotifyEnabled,
                        onCheckedChange = { onToggleWhatsAppGroupNotify() },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.whatsapp_notify_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.about)) {
                SettingsInfoRow(
                    label = stringResource(R.string.app_version),
                    value = uiState.appVersion,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsInfoRow(
                    label = stringResource(R.string.developer_info),
                    value = stringResource(R.string.developer_name),
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(R.string.privacy_policy),
                    onClick = onShowPrivacyPolicy,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(R.string.about),
                    onClick = onShowAbout,
                )
            }

            SettingsSection(title = stringResource(R.string.auth_account)) {
                PrimaryButton(
                    text = stringResource(R.string.profile_title),
                    onClick = onOpenProfile,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(R.string.auth_logout),
                    onClick = onLogout,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(AppLanguage.ENGLISH, AppLanguage.MARATHI)
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, language ->
            SegmentedButton(
                selected = selected == language,
                onClick = { onSelected(language) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                ),
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                        AppLanguage.MARATHI -> stringResource(R.string.language_marathi)
                    },
                    fontWeight = if (selected == language) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
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
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
