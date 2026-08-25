package com.ganpati.vargani.presentation.expense

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.utils.DateTimeUtils
import com.ganpati.vargani.core.utils.UiEvent
import com.ganpati.vargani.core.utils.ValidationUtils
import com.ganpati.vargani.core.utils.WhatsAppGroupNotifyHelper
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseCategory
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.usecase.expense.GetExpenseUseCase
import com.ganpati.vargani.domain.usecase.expense.SaveExpenseUseCase
import com.ganpati.vargani.domain.usecase.settings.GetSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class ExpenseFormUiState(
    val title: String = "",
    val category: ExpenseCategory = ExpenseCategory.MISC,
    val amountText: String = "",
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val paidBy: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val timeMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val titleError: String? = null,
    val amountError: String? = null,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
)

sealed interface ExpenseFormEffect {
    data class Saved(val id: Long) : ExpenseFormEffect
}

@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val getExpense: GetExpenseUseCase,
    private val saveExpense: SaveExpenseUseCase,
    private val getSettings: GetSettingsUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val navExpenseId: Long? =
        savedStateHandle.get<Long>("expenseId")?.takeIf { it > 0L }

    private val _uiState = MutableStateFlow(ExpenseFormUiState(isLoading = navExpenseId != null))
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ExpenseFormEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var existing: Expense? = null
    private var initialized = false

    init {
        if (navExpenseId != null) initialize(navExpenseId)
    }

    fun initialize(expenseId: Long?) {
        if (initialized) return
        initialized = true
        if (expenseId == null || expenseId <= 0L) {
            initNew()
        } else {
            load(expenseId)
        }
    }

    fun onTitleChanged(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun onCategoryChanged(category: ExpenseCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onAmountChanged(value: String) {
        _uiState.update { it.copy(amountText = value, amountError = null) }
    }

    fun onPaymentModeChanged(mode: PaymentMode) {
        _uiState.update { it.copy(paymentMode = mode) }
    }

    fun onDateChanged(millis: Long) {
        _uiState.update { it.copy(dateMillis = DateTimeUtils.startOfDay(millis)) }
    }

    fun onTimeChanged(millis: Long) {
        _uiState.update { it.copy(timeMillis = millis) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        var titleError: String? = null
        var amountError: String? = null
        if (state.title.isBlank()) {
            titleError = context.getString(R.string.expense_error_title)
        }
        if (state.amountText.isBlank()) {
            amountError = context.getString(R.string.error_amount_required)
        } else if (!ValidationUtils.isValidAmount(state.amountText)) {
            amountError = context.getString(R.string.error_amount_positive)
        }
        if (titleError != null || amountError != null) {
            _uiState.update { it.copy(titleError = titleError, amountError = amountError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val paidBy = authRepository.observeSession().first().name.trim()
                .ifBlank { state.paidBy.trim() }
            runCatching {
                val base = existing
                val expense = Expense(
                    id = base?.id ?: 0L,
                    title = state.title.trim(),
                    category = state.category,
                    amount = state.amountText.toDouble(),
                    paymentMode = state.paymentMode,
                    paidBy = paidBy,
                    dateEpochMillis = state.dateMillis,
                    timeEpochMillis = state.timeMillis,
                    notes = state.notes.trim(),
                    createdAt = base?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
                val id = saveExpense(expense)
                Triple(id, state.isEditMode, expense)
            }.onSuccess { (id, isEdit, expense) ->
                _uiState.update { it.copy(isSaving = false) }
                _uiEvents.send(
                    UiEvent.ShowSnackbar(
                        context.getString(
                            if (isEdit) R.string.expense_updated else R.string.expense_saved,
                        ),
                    ),
                )
                if (!isEdit) {
                    notifyWhatsAppGroup(expense)
                }
                _effects.send(ExpenseFormEffect.Saved(id))
            }.onFailure {
                _uiState.update { it.copy(isSaving = false) }
                _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
            }
        }
    }

    private suspend fun notifyWhatsAppGroup(expense: Expense) {
        val settings = runCatching { getSettings() }.getOrNull() ?: return
        if (!settings.whatsappGroupNotifyEnabled) return
        WhatsAppGroupNotifyHelper.shareExpense(
            context = context,
            expense = expense,
            orgName = settings.organizationName,
        )
    }

    private fun initNew() {
        viewModelScope.launch {
            val paidBy = authRepository.observeSession().first().name.trim()
            val now = System.currentTimeMillis()
            _uiState.value = ExpenseFormUiState(
                paidBy = paidBy,
                dateMillis = DateTimeUtils.startOfDay(now),
                timeMillis = now,
                isEditMode = false,
                isLoading = false,
            )
        }
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val expense = getExpense(id)
            if (expense == null) {
                _uiState.update { it.copy(isLoading = false) }
                _uiEvents.send(UiEvent.ShowSnackbar(context.getString(R.string.error_generic)))
                _uiEvents.send(UiEvent.NavigateBack)
                return@launch
            }
            existing = expense
            _uiState.value = ExpenseFormUiState(
                title = expense.title,
                category = expense.category,
                amountText = formatAmount(expense.amount),
                paymentMode = expense.paymentMode,
                paidBy = expense.paidBy,
                dateMillis = expense.dateEpochMillis,
                timeMillis = expense.timeEpochMillis,
                notes = expense.notes,
                isEditMode = true,
                isLoading = false,
            )
        }
    }

    private fun formatAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toLong().toString()
        else "%.2f".format(Locale.US, amount)
}
