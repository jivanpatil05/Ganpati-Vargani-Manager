package com.ganpati.vargani.core.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Saves generated files into the device Downloads folder (or a user-picked URI).
 */
object FileDownloadUtils {

    private const val TAG = "FileDownloadUtils"

    /**
     * Copies [source] into the public Downloads collection.
     * @return display path/name for snackbar, or null on failure.
     */
    fun saveToDownloads(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
    ): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, source, displayName, mimeType)
            } else {
                saveLegacyDownloads(source, displayName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save $displayName to Downloads", e)
            null
        }
    }

    fun copyToUri(context: Context, source: File, destination: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destination)?.use { output ->
                FileInputStream(source).use { input -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to $destination", e)
            false
        }
    }

    private fun saveWithMediaStore(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)
            ?: error("MediaStore insert returned null")

        resolver.openOutputStream(uri)?.use { output: OutputStream ->
            FileInputStream(source).use { input -> input.copyTo(output) }
        } ?: error("Unable to open Downloads output stream")

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return displayName
    }

    @Suppress("DEPRECATION")
    private fun saveLegacyDownloads(source: File, displayName: String): String {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()
        val target = File(downloads, displayName)
        FileInputStream(source).use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        }
        return target.absolutePath
    }
}
