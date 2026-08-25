package com.ganpati.vargani.data.repository

import com.ganpati.vargani.data.remote.FirestoreIdGenerator
import com.ganpati.vargani.data.remote.FirestoreMappers
import com.ganpati.vargani.data.remote.FirestoreMappers.toExpense
import com.ganpati.vargani.data.remote.FirestorePaths
import com.ganpati.vargani.data.remote.UserSessionStore
import com.ganpati.vargani.domain.model.Expense
import com.ganpati.vargani.domain.model.ExpenseStats
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.AuthSessionState
import com.ganpati.vargani.domain.repository.ExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionStore: UserSessionStore,
) : ExpenseRepository {

    private fun expenses() = firestore.collection(FirestorePaths.EXPENSES)

    private fun observeCommitteeExpenses(): Flow<List<Expense>> = callbackFlow {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var registration: ListenerRegistration? = null
        var collectJob: Job? = null

        fun bind(committeeId: String) {
            registration?.remove()
            if (committeeId.isBlank()) {
                trySend(emptyList())
                return
            }
            registration = expenses()
                .whereEqualTo("committeeId", committeeId)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snap?.documents
                        ?.mapNotNull { it.toExpense() }
                        ?.sortedByDescending { it.dateEpochMillis }
                        .orEmpty()
                    trySend(list)
                }
        }

        collectJob = scope.launch {
            sessionStore.session.collect { state: AuthSessionState ->
                if (!state.isLoggedIn) {
                    registration?.remove()
                    trySend(emptyList())
                } else {
                    bind(state.committeeId)
                }
            }
        }

        awaitClose {
            registration?.remove()
            collectJob?.cancel()
            scope.coroutineContext[Job]?.cancel()
        }
    }

    override fun observeAll(): Flow<List<Expense>> = observeCommitteeExpenses()

    override fun observeFiltered(query: String): Flow<List<Expense>> =
        observeAll().map { list ->
            val q = query.trim()
            if (q.isEmpty()) list
            else list.filter {
                it.title.contains(q, true) ||
                    it.category.name.contains(q, true) ||
                    it.paidBy.contains(q, true) ||
                    it.notes.contains(q, true)
            }
        }

    override fun observeById(id: Long): Flow<Expense?> =
        observeAll().map { list -> list.find { it.id == id } }

    override fun observeStats(): Flow<ExpenseStats> =
        observeAll().map { list ->
            val startOfDay = com.ganpati.vargani.core.utils.DateTimeUtils.startOfDay()
            ExpenseStats(
                totalExpenses = list.sumOf { it.amount },
                todayExpenses = list.filter { it.dateEpochMillis >= startOfDay }.sumOf { it.amount },
                cashTotal = list.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.amount },
                upiTotal = list.filter { it.paymentMode == PaymentMode.UPI }.sumOf { it.amount },
                count = list.size,
            )
        }

    override suspend fun getById(id: Long): Expense? {
        val snap = expenses().document(id.toString()).get().await()
        return snap.toExpense()
    }

    override suspend fun save(expense: Expense): Long {
        sessionStore.requireWriteAccess()
        val committeeId = sessionStore.requireCommitteeId()
        val id = if (expense.id > 0L) expense.id else FirestoreIdGenerator.nextLongId()
        val toSave = expense.copy(id = id, updatedAt = System.currentTimeMillis())
        expenses().document(id.toString())
            .set(FirestoreMappers.expenseToMap(toSave, committeeId))
            .await()
        return id
    }

    override suspend fun delete(id: Long) {
        sessionStore.requireWriteAccess()
        expenses().document(id.toString()).delete().await()
    }
}
