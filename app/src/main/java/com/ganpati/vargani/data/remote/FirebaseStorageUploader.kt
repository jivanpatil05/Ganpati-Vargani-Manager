package com.ganpati.vargani.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads payment QR images to Firebase Storage.
 * Path: committees/{committeeId}/qr/qr_code.jpg
 */
@Singleton
class FirebaseStorageUploader @Inject constructor(
    private val storage: FirebaseStorage,
    private val sessionStore: UserSessionStore,
) {
    suspend fun uploadQrImage(localPath: String): String {
        sessionStore.requireAdminAccess()
        val committeeId = sessionStore.requireCommitteeId()
        val file = File(localPath)
        require(file.exists()) { "QR image file not found" }
        val ref = storage.reference
            .child("committees")
            .child(committeeId)
            .child("qr")
            .child("qr_code.jpg")
        ref.putFile(Uri.fromFile(file)).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteQrImage() {
        sessionStore.requireAdminAccess()
        val committeeId = sessionStore.requireCommitteeId()
        runCatching {
            storage.reference
                .child("committees")
                .child(committeeId)
                .child("qr")
                .child("qr_code.jpg")
                .delete()
                .await()
        }
    }
}
