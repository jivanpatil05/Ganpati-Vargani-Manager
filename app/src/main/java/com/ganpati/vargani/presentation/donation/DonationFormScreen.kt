package com.ganpati.vargani.presentation.donation

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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.ganpati.vargani.core.theme.TextFieldShape
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.core.utils.FileShareUtils
import com.ganpati.vargani.core.utils.PrintUtils
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.domain.model.PaymentMode
import kotlinx.coroutines.android.awaitFrame
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DonationFormRoute(
    donationId: Long?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: DonationFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var scrollToField by remember { mutableStateOf<DonationFormFieldKey?>(null) }

    LaunchedEffect(donationId) {
        viewModel.initialize(donationId)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                    )
                    event.action?.invoke()
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
                is DonationFormEffect.Saved -> onSaved(effect.id)
                is DonationFormEffect.FirstInvalidField -> scrollToField = effect.key
            }
        }
    }

    DonationFormScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        scrollToField = scrollToField,
        onScrollToFieldHandled = { scrollToField = null },
        onBack = onBack,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DonationFormScreen(
    uiState: DonationFormUiState,
    snackbarHostState: SnackbarHostState,
    scrollToField: DonationFormFieldKey?,
    onScrollToFieldHandled: () -> Unit,
    onBack: () -> Unit,
    onEvent: (DonationFormEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val nameRequester = remember { BringIntoViewRequester() }
    val mobileRequester = remember { BringIntoViewRequester() }
    val amountRequester = remember { BringIntoViewRequester() }

    val nameFocus = remember { FocusRequester() }
    val mobileFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val title = if (uiState.isEditMode) {
        stringResource(R.string.edit_donation)
    } else {
        stringResource(R.string.add_donation)
    }

    val saveLabel = if (uiState.isEditMode) {
        stringResource(R.string.update_donation)
    } else {
        stringResource(R.string.save_donation)
    }

    // Only run after fields are composed (Column keeps all focus targets attached).
    LaunchedEffect(scrollToField, uiState.isLoading) {
        val key = scrollToField ?: return@LaunchedEffect
        if (uiState.isLoading) return@LaunchedEffect
        awaitFrame()
        val requester: BringIntoViewRequester
        val focus: FocusRequester
        when (key) {
            DonationFormFieldKey.NAME -> {
                requester = nameRequester
                focus = nameFocus
            }
            DonationFormFieldKey.MOBILE -> {
                requester = mobileRequester
                focus = mobileFocus
            }
            DonationFormFieldKey.AMOUNT -> {
                requester = amountRequester
                focus = amountFocus
            }
            DonationFormFieldKey.COLLECTOR -> {
                // Collector is locked to the signed-in user name.
                onScrollToFieldHandled()
                return@LaunchedEffect
            }
        }
        runCatching {
            requester.bringIntoView()
            awaitFrame()
            focus.requestFocus()
        }
        onScrollToFieldHandled()
    }

    Scaffold(
        topBar = {
            VarganiTopAppBar(
                title = title,
                onNavigateBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            FormTextField(
                value = uiState.receiptNo,
                onValueChange = {},
                label = stringResource(R.string.receipt_number),
                enabled = false,
                readOnly = true,
            )

            FormTextField(
                modifier = Modifier
                    .bringIntoViewRequester(nameRequester)
                    .focusRequester(nameFocus),
                value = uiState.name,
                onValueChange = { onEvent(DonationFormEvent.NameChanged(it)) },
                label = stringResource(R.string.donor_name),
                isError = DonationFormFieldKey.NAME in uiState.errors,
                errorMessage = uiState.errors[DonationFormFieldKey.NAME]?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            FormTextField(
                modifier = Modifier
                    .bringIntoViewRequester(mobileRequester)
                    .focusRequester(mobileFocus),
                value = uiState.mobile,
                onValueChange = { onEvent(DonationFormEvent.MobileChanged(it)) },
                label = stringResource(R.string.mobile_number),
                isError = DonationFormFieldKey.MOBILE in uiState.errors,
                errorMessage = uiState.errors[DonationFormFieldKey.MOBILE]?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
            )

            FormTextField(
                value = uiState.address,
                onValueChange = { onEvent(DonationFormEvent.AddressChanged(it)) },
                label = stringResource(R.string.address),
                singleLine = false,
                minLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            AmountField(
                modifier = Modifier
                    .bringIntoViewRequester(amountRequester)
                    .focusRequester(amountFocus),
                value = uiState.amountText,
                onValueChange = { onEvent(DonationFormEvent.AmountChanged(it)) },
                isError = DonationFormFieldKey.AMOUNT in uiState.errors,
                errorMessage = uiState.errors[DonationFormFieldKey.AMOUNT]?.let { stringResource(it) },
            )

            Text(
                text = stringResource(R.string.payment_mode),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PaymentModeSelector(
                selected = uiState.paymentMode,
                onSelected = { onEvent(DonationFormEvent.PaymentModeChanged(it)) },
            )

            FormTextField(
                value = uiState.collector,
                onValueChange = {},
                label = stringResource(R.string.collector_name),
                enabled = false,
                readOnly = true,
                isError = DonationFormFieldKey.COLLECTOR in uiState.errors,
                errorMessage = uiState.errors[DonationFormFieldKey.COLLECTOR]?.let { stringResource(it) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FormTextField(
                    modifier = Modifier.weight(1f),
                    value = DateTimeUtils.formatDate(uiState.dateMillis),
                    onValueChange = {},
                    label = stringResource(R.string.date),
                    readOnly = true,
                    onClick = { showDatePicker = true },
                )
                FormTextField(
                    modifier = Modifier.weight(1f),
                    value = DateTimeUtils.formatTime(uiState.timeMillis),
                    onValueChange = {},
                    label = stringResource(R.string.time),
                    readOnly = true,
                    onClick = { showTimePicker = true },
                )
            }

            FormTextField(
                value = uiState.notes,
                onValueChange = { onEvent(DonationFormEvent.NotesChanged(it)) },
                label = stringResource(R.string.notes),
                singleLine = false,
                minLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )

            PrimaryButton(
                text = saveLabel,
                onClick = { onEvent(DonationFormEvent.Save) },
                enabled = !uiState.isSaving,
                loading = uiState.isSaving,
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            onEvent(DonationFormEvent.DateChanged(selected))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance(Locale("en", "IN")).apply {
            timeInMillis = uiState.timeMillis
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false,
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = Calendar.getInstance(Locale("en", "IN")).apply {
                            timeInMillis = uiState.timeMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onEvent(DonationFormEvent.TimeChanged(updated.timeInMillis))
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentModeSelector(
    selected: PaymentMode,
    onSelected: (PaymentMode) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selected == PaymentMode.CASH,
            onClick = { onSelected(PaymentMode.CASH) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
            Text(stringResource(R.string.payment_cash))
        }
        SegmentedButton(
            selected = selected == PaymentMode.UPI,
            onClick = { onSelected(PaymentMode.UPI) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
            Text(stringResource(R.string.payment_upi))
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onClick: (() -> Unit)? = null,
) {
    val fieldModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else {
        modifier
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = fieldModifier
            .fillMaxWidth()
            .bringIntoViewWhenFocused(),
        label = { Text(label) },
        enabled = enabled && onClick == null,
        readOnly = readOnly || onClick != null,
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        supportingText = {
            if (isError && errorMessage != null) {
                Text(text = errorMessage)
            }
        },
        shape = TextFieldShape,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
