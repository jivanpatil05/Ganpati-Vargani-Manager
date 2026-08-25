package com.ganpati.vargani.presentation.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.components.ConfirmationDialog
import com.ganpati.vargani.core.components.LoadingView
import com.ganpati.vargani.core.components.PrimaryButton
import com.ganpati.vargani.core.components.SecondaryButton
import com.ganpati.vargani.core.components.VarganiTopAppBar
import com.ganpati.vargani.core.components.categoryLabel
import com.ganpati.vargani.core.theme.CardShape
import com.ganpati.vargani.core.theme.ErrorRed
import com.ganpati.vargani.core.utils.CurrencyUtils
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.usecase.expense.DeleteExpenseUseCase
import com.ganpati.vargani.domain.usecase.expense.ObserveExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseDetailsUiState(
    val expense: Expense? = null,
    val isLoading: Boolean = true,
    val canWrite: Boolean = false,
)

@HiltViewModel
class ExpenseDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeExpense: ObserveExpenseUseCase,
    authRepository: AuthRepository,
    private val deleteExpense: DeleteExpenseUseCase,
) : ViewModel() {

    private val expenseId: Long = checkNotNull(savedStateHandle.get<Long>("expenseId"))

    val uiState: StateFlow<ExpenseDetailsUiState> = combine(
        observeExpense(expenseId),
        authRepository.observeSession(),
    ) { expense, session ->
        ExpenseDetailsUiState(
            expense = expense,
            isLoading = false,
            canWrite = session.canWrite,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpenseDetailsUiState(),
    )

    fun delete(onDeleted: () -> Unit) {
        if (!uiState.value.canWrite) return
        viewModelScope.launch {
            deleteExpense(expenseId)
            onDeleted()
        }
    }
}

@Composable
fun ExpenseDetailsRoute(
    expenseId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: ExpenseDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ExpenseDetailsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = { showDeleteConfirm = true },
    )

    if (showDeleteConfirm && uiState.canWrite) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_delete_expense_title),
            message = stringResource(R.string.confirm_delete_expense_message),
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete(onDeleted)
            },
            onDismiss = { showDeleteConfirm = false },
            confirmText = stringResource(R.string.delete),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDetailsScreen(
    uiState: ExpenseDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Scaffold(
        topBar = {
            VarganiTopAppBar(
                title = stringResource(R.string.expense_details),
                onNavigateBack = onBack,
                actions = {
                    if (uiState.canWrite) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val expense = uiState.expense
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) { LoadingView() }
            }
            expense == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.error_generic))
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = expense.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = categoryLabel(expense.category),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.format(expense.amount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed,
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            DetailRow(
                                stringResource(R.string.payment_mode),
                                when (expense.paymentMode) {
                                    PaymentMode.CASH -> stringResource(R.string.payment_cash)
                                    PaymentMode.UPI -> stringResource(R.string.payment_upi)
                                },
                            )
                            DetailRow(stringResource(R.string.expense_paid_by), expense.paidBy)
                            DetailRow(
                                stringResource(R.string.date),
                                "${DateTimeUtils.formatDate(expense.dateEpochMillis)}  " +
                                    DateTimeUtils.formatTime(expense.timeEpochMillis),
                            )
                            if (expense.notes.isNotBlank()) {
                                DetailRow(stringResource(R.string.notes), expense.notes)
                            }
                        }
                    }

                    if (uiState.canWrite) {
                        PrimaryButton(text = stringResource(R.string.edit), onClick = onEdit)
                        SecondaryButton(text = stringResource(R.string.delete), onClick = onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
