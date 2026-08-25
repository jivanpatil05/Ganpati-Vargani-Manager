package com.ganpati.vargani.data.repository

import com.ganpati.vargani.data.remote.FirestoreIdGenerator
import com.ganpati.vargani.data.remote.FirestoreMappers
import com.ganpati.vargani.data.remote.FirestoreMappers.toDonation
import com.ganpati.vargani.data.remote.FirestorePaths
import com.ganpati.vargani.data.remote.UserSessionStore
import com.ganpati.vargani.domain.model.CollectorStat
import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.DonationFilter
import com.ganpati.vargani.domain.model.DonationSort
import com.ganpati.vargani.domain.model.Member
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.AuthSessionState
import com.ganpati.vargani.domain.repository.DonationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-backed donations stored in the [payments] collection.
 */
@Singleton
class FirestoreDonationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionStore: UserSessionStore,
) : DonationRepository {

    private fun payments() = firestore.collection(FirestorePaths.PAYMENTS)

    private fun observeCommitteeDonations(): Flow<List<Donation>> = callbackFlow {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var registration: ListenerRegistration? = null
        var collectJob: Job? = null

        fun bind(committeeId: String) {
            registration?.remove()
            if (committeeId.isBlank()) {
                trySend(emptyList())
                return
            }
            registration = payments()
                .whereEqualTo("committeeId", committeeId)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snap?.documents
                        ?.mapNotNull { it.toDonation() }
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

    override fun observeAll(): Flow<List<Donation>> = observeCommitteeDonations()

    override fun observeRecent(limit: Int): Flow<List<Donation>> =
        observeAll().map { it.take(limit) }

    override fun observeFiltered(filter: DonationFilter): Flow<List<Donation>> =
        observeAll().map { list -> applyFilter(list, filter) }

    override fun observeById(id: Long): Flow<Donation?> =
        observeAll().map { list -> list.find { it.id == id } }

    override fun observeTopCollectors(limit: Int): Flow<List<CollectorStat>> =
        observeAll().map { list ->
            list.groupBy { it.collector.ifBlank { "—" } }
                .map { (collector, donations) ->
                    CollectorStat(
                        collector = collector,
                        totalAmount = donations.sumOf { it.amount },
                        donationCount = donations.size,
                    )
                }
                .sortedByDescending { it.totalAmount }
                .take(limit)
        }

    override fun observeTopDonors(limit: Int): Flow<List<Donation>> =
        observeAll().map { list -> list.sortedByDescending { it.amount }.take(limit) }

    override fun observeCollectors(): Flow<List<String>> =
        observeAll().map { list ->
            list.map { it.collector.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        }

    override fun observeTotalCollection(): Flow<Double> =
        observeAll().map { it.sumOf { d -> d.amount } }

    override fun observeCollectionBetween(start: Long, end: Long): Flow<Double> =
        observeAll().map { list ->
            list.filter { it.dateEpochMillis in start..end }.sumOf { it.amount }
        }

    override fun observeDonorCount(): Flow<Int> =
        observeAll().map { list ->
            list.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().size
        }

    override fun observeAverageDonation(): Flow<Double> =
        observeAll().map { list -> if (list.isEmpty()) 0.0 else list.map { it.amount }.average() }

    override fun observeHighestDonation(): Flow<Double> =
        observeAll().map { list -> list.maxOfOrNull { it.amount } ?: 0.0 }

    override fun observeCashCollection(): Flow<Double> =
        observeAll().map { list ->
            list.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.amount }
        }

    override fun observeUpiCollection(): Flow<Double> =
        observeAll().map { list ->
            list.filter { it.paymentMode == PaymentMode.UPI }.sumOf { it.amount }
        }

    override fun observePendingReceipts(): Flow<Int> =
        observeAll().map { list -> list.count { !it.isReceiptPrinted } }

    override suspend fun getById(id: Long): Donation? {
        val snap = payments().document(id.toString()).get().await()
        return snap.toDonation()
    }

    override suspend fun getLowestDonation(): Double {
        val list = observeAll().first()
        return list.minOfOrNull { it.amount } ?: 0.0
    }

    override suspend fun insert(donation: Donation): Long {
        sessionStore.requireWriteAccess()
        val committeeId = sessionStore.requireCommitteeId()
        val id = if (donation.id > 0L) donation.id else FirestoreIdGenerator.nextLongId()
        val toSave = donation.copy(id = id, updatedAt = System.currentTimeMillis())
        payments().document(id.toString())
            .set(FirestoreMappers.donationToMap(toSave, committeeId))
            .await()
        upsertMemberFromDonation(toSave, committeeId)
        return id
    }

    override suspend fun update(donation: Donation) {
        sessionStore.requireWriteAccess()
        val committeeId = sessionStore.requireCommitteeId()
        require(donation.id > 0L) { "Invalid donation id" }
        payments().document(donation.id.toString())
            .set(
                FirestoreMappers.donationToMap(
                    donation.copy(updatedAt = System.currentTimeMillis()),
                    committeeId,
                ),
            )
            .await()
    }

    override suspend fun delete(id: Long) {
        sessionStore.requireWriteAccess()
        payments().document(id.toString()).delete().await()
    }

    override suspend fun deleteAll() {
        sessionStore.requireWriteAccess()
        val committeeId = sessionStore.requireCommitteeId()
        val snap = payments().whereEqualTo("committeeId", committeeId).get().await()
        val batch = firestore.batch()
        snap.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    override suspend fun isReceiptUnique(receiptNo: String, excludeId: Long): Boolean {
        val committeeId = sessionStore.requireCommitteeId()
        val snap = payments()
            .whereEqualTo("committeeId", committeeId)
            .whereEqualTo("receiptNo", receiptNo)
            .get()
            .await()
        return snap.documents.none { doc ->
            val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: -1L
            id != excludeId
        }
    }

    override suspend fun markReceiptPrinted(id: Long) {
        sessionStore.requireWriteAccess()
        payments().document(id.toString())
            .update(
                mapOf(
                    "isReceiptPrinted" to true,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            )
            .await()
    }

    private suspend fun upsertMemberFromDonation(donation: Donation, committeeId: String) {
        if (donation.name.isBlank()) return
        val members = firestore.collection(FirestorePaths.MEMBERS)
        val existing = members
            .whereEqualTo("committeeId", committeeId)
            .whereEqualTo("name", donation.name.trim())
            .limit(1)
            .get()
            .await()
        if (!existing.isEmpty) return
        val ref = members.document()
        ref.set(
            FirestoreMappers.memberToMap(
                Member(
                    id = ref.id,
                    committeeId = committeeId,
                    name = donation.name.trim(),
                    mobile = donation.mobile,
                    address = donation.address,
                ),
            ),
        ).await()
    }

    private fun applyFilter(list: List<Donation>, filter: DonationFilter): List<Donation> {
        var result = list
        val q = filter.query.trim()
        if (q.isNotEmpty()) {
            result = result.filter {
                it.name.contains(q, true) ||
                    it.receiptNo.contains(q, true) ||
                    it.mobile.contains(q, true) ||
                    it.collector.contains(q, true)
            }
        }
        filter.collector?.takeIf { it.isNotBlank() }?.let { c ->
            result = result.filter { it.collector.equals(c, true) }
        }
        filter.paymentMode?.let { mode -> result = result.filter { it.paymentMode == mode } }
        filter.startDateMillis?.let { start -> result = result.filter { it.dateEpochMillis >= start } }
        filter.endDateMillis?.let { end -> result = result.filter { it.dateEpochMillis <= end } }
        filter.minAmount?.let { min -> result = result.filter { it.amount >= min } }
        filter.maxAmount?.let { max -> result = result.filter { it.amount <= max } }
        return when (filter.sort) {
            DonationSort.LATEST -> result.sortedByDescending { it.dateEpochMillis }
            DonationSort.OLDEST -> result.sortedBy { it.dateEpochMillis }
            DonationSort.HIGHEST_AMOUNT -> result.sortedByDescending { it.amount }
            DonationSort.LOWEST_AMOUNT -> result.sortedBy { it.amount }
            DonationSort.ALPHABETICAL -> result.sortedBy { it.name.lowercase() }
        }
    }
}
