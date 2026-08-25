package com.ganpati.vargani

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Hilt generates the dependency graph from here.
 */
@HiltAndroidApp
class GanpatiVarganiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        // Must set Firestore settings BEFORE any other Firestore API calls.
        val firestore = FirebaseFirestore.getInstance()
        runCatching {
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
        }
        runCatching { firestore.enableNetwork() }
    }
}
