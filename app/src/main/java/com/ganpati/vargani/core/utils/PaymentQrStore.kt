package com.ganpati.vargani.core.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.ganpati.vargani.core.constants.AppConstants
import java.io.File
import java.io.FileOutputStream

/**
 * Copies a user-picked/cropped QR image into app-private storage for offline use.
 */
object PaymentQrStore {

    private const val TAG = "PaymentQrStore"
    private const val DIR_NAME = "payment"
    private const val FILE_NAME = "qr_code.jpg"
    private const val CROP_TEMP_NAME = "qr_crop_temp.jpg"

    fun saveFromUri(context: Context, uri: Uri): String? {
        return try {
            val dest = destinationFile(context)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            } ?: return null
            dest.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save QR image", e)
            null
        }
    }

    /**
     * Temporary FileProvider URI used as cropper output before final save.
     */
    fun createCropOutputUri(context: Context): Uri {
        val dir = File(context.filesDir, DIR_NAME).also { it.mkdirs() }
        val file = File(dir, CROP_TEMP_NAME)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}${AppConstants.FILE_PROVIDER_AUTHORITY_SUFFIX}",
            file,
        )
    }

    fun delete(path: String) {
        if (path.isBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun destinationFile(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME).also { it.mkdirs() }
        return File(dir, FILE_NAME)
    }
}
