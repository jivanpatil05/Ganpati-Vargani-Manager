package com.ganpati.vargani.presentation.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.SectionHeader
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.components.bringIntoViewWhenFocused
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.AppUserProfile
import com.ganpati.vargani.domain.model.UserRole

@Composable
fun ManageUsersRoute(
    onBack: () -> Unit,
    viewModel: ManageUsersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            if (event is UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    ManageUsersScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onNameChanged = viewModel::onNameChanged,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onMobileChanged = viewModel::onMobileChanged,
        onCreateViewer = viewModel::createViewer,
    )
}

@Composable
fun ManageUsersScreen(
    uiState: ManageUsersUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onMobileChanged: (String) -> Unit,
    onCreateViewer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.manage_users_title),
                onNavigateBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.manage_users_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                SectionHeader(title = stringResource(R.string.manage_users_team))
            }

            if (uiState.users.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.manage_users_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.users, key = { it.uid }) { user ->
                    UserRow(
                        user = user,
                        isYou = user.uid == uiState.currentUid,
                    )
                }
            }

            if (uiState.isAdmin) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = stringResource(R.string.manage_users_create_viewer))
                    Text(
                        text = stringResource(
                            R.string.manage_users_slots,
                            uiState.viewerCount,
                            2,
                            uiState.remainingViewerSlots,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (uiState.canCreateViewer) {
                    item {
                        CreateViewerForm(
                            uiState = uiState,
                            onNameChanged = onNameChanged,
                            onEmailChanged = onEmailChanged,
                            onPasswordChanged = onPasswordChanged,
                            onMobileChanged = onMobileChanged,
                            onCreateViewer = onCreateViewer,
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.manage_users_limit_reached),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: AppUserProfile,
    isYou: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayName = user.name.ifBlank { user.email }
                val youLabel = stringResource(R.string.manage_users_you)
                Text(
                    text = if (isYou) "$displayName · $youLabel" else displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (user.email.isNotBlank()) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            RoleBadge(role = user.role)
        }
    }
}

@Composable
private fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier,
) {
    val label = when (role) {
        UserRole.ADMIN -> stringResource(R.string.manage_users_role_admin)
        UserRole.VIEWER -> stringResource(R.string.manage_users_role_viewer)
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = when (role) {
            UserRole.ADMIN -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            UserRole.VIEWER -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
        },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = when (role) {
                UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                UserRole.VIEWER -> MaterialTheme.colorScheme.tertiary
            },
        )
    }
}

@Composable
private fun CreateViewerForm(
    uiState: ManageUsersUiState,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onMobileChanged: (String) -> Unit,
    onCreateViewer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewWhenFocused(),
                label = { Text(stringResource(R.string.auth_full_name)) },
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                enabled = !uiState.isSubmitting,
            )
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewWhenFocused(),
                label = { Text(stringResource(R.string.auth_email)) },
                singleLine = true,
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                enabled = !uiState.isSubmitting,
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewWhenFocused(),
                label = { Text(stringResource(R.string.auth_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                enabled = !uiState.isSubmitting,
            )
            OutlinedTextField(
                value = uiState.mobile,
                onValueChange = onMobileChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewWhenFocused(),
                label = { Text(stringResource(R.string.auth_mobile_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                ),
                enabled = !uiState.isSubmitting,
            )

            uiState.formError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (uiState.isSubmitting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                PrimaryButton(
                    text = stringResource(R.string.manage_users_create_button),
                    onClick = onCreateViewer,
                )
            }

            HorizontalDivider()
            Text(
                text = stringResource(R.string.manage_users_create_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
