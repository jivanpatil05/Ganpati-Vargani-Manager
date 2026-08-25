package com.ganpati.vargani.core.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.ganpati.vargani.core.constants.AppConstants
import java.io.File

/**
 * Shares app-generated files (receipts, CSV, Excel, backups) via the system share sheet.
 *
 * Uses [FileProvider] so content URIs can be granted read permission to target apps
 * without exposing internal storage paths. Requires matching authority in
 * [AndroidManifest.xml] and paths declared in `res/xml/file_paths.xml`.
 */
object FileShareUtils {

    private const val TAG = "FileShareUtils"

    /**
     * Opens the platform share chooser for [file].
     *
     * @param context Activity or application context. An application context automatically
     *   receives [Intent.FLAG_ACTIVITY_NEW_TASK] so the chooser can be launched.
     * @param file Existing, readable file under a FileProvider-configured directory.
     * @param mimeType MIME type understood by receiving apps (e.g. `application/pdf`).
     * @param chooserTitle Title shown on the share sheet.
     */
    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String,
    ) {
        require(file.exists() && file.canRead()) {
            "Cannot share missing or unreadable file: ${file.absolutePath}"
        }

        val authority = "${context.packageName}${AppConstants.FILE_PROVIDER_AUTHORITY_SUFFIX}"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            // Allow the chosen app to read the stream for the lifetime of this intent.
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "No app available to handle share intent for $mimeType", e)
            throw e
        }
    }
}
