package com.ganpati.vargani.core.utils

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Prints PDF files through the Android system print framework.
 *
 * A custom [PrintDocumentAdapter] streams the on-disk PDF to the print service via
 * [ParcelFileDescriptor], which is the recommended approach for pre-generated PDFs.
 * [android.print.pdf.PrintedPdfDocument] is intended for programmatic PDF creation,
 * not replaying an existing file.
 */
object PrintUtils {

    private const val TAG = "PrintUtils"

    /**
     * Sends [file] to the default print dialog.
     *
     * @param context Context with access to [Context.PRINT_SERVICE] (typically an Activity).
     * @param file Existing PDF on disk.
     * @param jobName Human-readable name shown in the print queue and spooler UI.
     */
    fun printPdf(context: Context, file: File, jobName: String) {
        require(file.exists() && file.canRead()) {
            "Cannot print missing or unreadable PDF: ${file.absolutePath}"
        }

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: run {
                Log.e(TAG, "PrintManager unavailable on this device")
                return
            }

        val adapter = PdfFilePrintDocumentAdapter(pdfFile = file, documentName = jobName)
        val attributes = PrintAttributes.Builder().build()

        printManager.print(jobName, adapter, attributes)
    }

    /**
     * [PrintDocumentAdapter] that copies a PDF file into the print pipeline.
     *
     * Layout reports a single unknown-length document; write streams bytes from disk
     * to the destination descriptor supplied by the print service.
     */
    private class PdfFilePrintDocumentAdapter(
        private val pdfFile: File,
        private val documentName: String,
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }

            val info = PrintDocumentInfo.Builder(documentName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                // Page count is not parsed from the file; the print service renders the PDF.
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()

            // Document content changed if print attributes changed (e.g. color vs. mono).
            val changed = oldAttributes != newAttributes
            callback.onLayoutFinished(info, changed)
        }

        override fun onWrite(
            pages: Array<out android.print.PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            var input: FileInputStream? = null
            var output: FileOutputStream? = null

            try {
                input = FileInputStream(pdfFile)
                output = FileOutputStream(destination.fileDescriptor)

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } >= 0) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }
                    output.write(buffer, 0, bytesRead)
                }

                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write PDF to print destination", e)
                callback.onWriteFailed(e.message)
            } finally {
                try {
                    input?.close()
                } catch (ignored: IOException) {
                    // Best-effort cleanup.
                }
                try {
                    output?.close()
                } catch (ignored: IOException) {
                    // Best-effort cleanup.
                }
                try {
                    destination.close()
                } catch (ignored: IOException) {
                    // Caller owns the descriptor; closing is required per platform contract.
                }
            }
        }
    }
}
