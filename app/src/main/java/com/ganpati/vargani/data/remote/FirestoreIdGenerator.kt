package com.ganpati.vargani.data.remote

import kotlin.random.Random

/**
 * Generates positive Long IDs compatible with existing navigation / domain models.
 * Stored as Firestore document IDs (string form of the Long).
 */
object FirestoreIdGenerator {
    fun nextLongId(): Long {
        val time = System.currentTimeMillis()
        val noise = Random.nextInt(100, 999)
        return time * 1000L + noise
    }
}
