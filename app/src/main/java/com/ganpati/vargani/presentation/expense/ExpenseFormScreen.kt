package com.ganpati.vargani.presentation.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.AmountField
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.components.bringIntoViewWhenFocused
import com.ganpati.vargani.core.components.categoryLabel
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.ExpenseCategory
import com.ganpati.vargani.domain.model.PaymentMode
import java.util.Calendar

@Composable
fun ExpenseFormRoute(
    expenseId: Long?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: ExpenseFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(expenseId) {
        viewModel.initialize(expenseId)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                UiEvent.NavigateBack -> onBack()
                else -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExpenseFormEffect.Saved -> onSaved(effect.id)
            }
        }
    }

    ExpenseFormScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTitleChanged = viewModel::onTitleChanged,
        onCategoryChanged = viewModel::onCategoryChanged,
        onAmountChanged = viewModel::onAmountChanged,
        onPaymentModeChanged = viewModel::onPaymentModeChanged,
        onDateChanged = viewModel::onDateChanged,
        onTimeChanged = viewModel::onTimeChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormScreen(
    uiState: ExpenseFormUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onCategoryChanged: (ExpenseCategory) -> Unit,
    onAmountChanged: (String) -> Unit,
    onPaymentModeChanged: (PaymentMode) -> Unit,
    onDateChanged: (Long) -> Unit,
    onTimeChanged: (Long) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VarganiTopAppBar(
                title = stringResource(
                    if (uiState.isEditMode) R.string.edit_expense else R.string.add_expense,
                ),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewWhenFocused(),
                label = { Text(stringResource(R.string.expense_title)) },
                singleLine = true,
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedTextField(
                value = categoryLabel(uiState.category),
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { categoryExpanded = true },
                label = { Text(stringResource(R.string.expense_category)) },
                readOnly = true,
                enabled = false,
            )

            if (categoryExpanded) {
                CategoryPickerDialog(
                    selected = uiState.category,
                    onSelected = {
                        onCategoryChanged(it)
                        categoryExpanded = false
                    },
                    onDismiss = { categoryExpanded = false },
                )
            }

            AmountField(
                value = uiState.amountText,
                onValueChange = onAmountChanged,
                isError = uiState.amountError != null,
                errorMessage = uiState.amountError,
            )

            Text(
                text = stringResource(R.string.payment_mode),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.paymentMode == PaymentMode.CASH,
                    onClick = { onPaymentModeChanged(PaymentMode.CASH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.payment_cash)) }
                SegmentedButton(
                    selected = uiState.paymentMode == PaymentMode.UPI,
                    onClick = { onPaymentModeChanged(PaymentMode.UPI) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.payment_upi)) }
            }

            OutlinedTextField(
                value = uiState.paidBy,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.expense_paid_by)) },
                enabled = false,
                readOnly = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = DateTimeUtils.formatDate(uiState.dateMillis),
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showDatePicker = true },
                    label = { Text(stringResource(R.string.date)) },
                    readOnly = true,
                    enabled = false,
                )
                OutlinedTextField(
                    value = DateTimeUtils.formatTime(uiState.timeMillis),
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showTimePicker = true },
                    label = { Text(stringResource(R.string.time)) },
                    readOnly = true,
                    enabled = false,
                )
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewWhenFocused(),
                label = { Text(stringResource(R.string.notes)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                ),
            )

            Spacer(modifier = Modifier.height(48.dp))

            PrimaryButton(
                text = stringResource(
                    if (uiState.isEditMode) R.string.update_expense else R.string.save_expense,
                ),
                onClick = onSave,
                enabled = !uiState.isSaving,
                loading = uiState.isSaving,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = uiState.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let(onDateChanged)
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.timeMillis }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false,
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = uiState.timeMillis
                            set(Calendar.HOUR_OF_DAY, timeState.hour)
                            set(Calendar.MINUTE, timeState.minute)
                        }.timeInMillis
                        onTimeChanged(updated)
                        showTimePicker = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = { TimePicker(state = timeState) },
        )
    }
}

@Composable
private fun CategoryPickerDialog(
    selected: ExpenseCategory,
    onSelected: (ExpenseCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expense_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ExpenseCategory.entries.forEach { category ->
                    TextButton(
                        onClick = { onSelected(category) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = categoryLabel(category),
                            fontWeight = if (category == selected) {
                                androidx.compose.ui.text.font.FontWeight.Bold
                            } else {
                                androidx.compose.ui.text.font.FontWeight.Normal
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
