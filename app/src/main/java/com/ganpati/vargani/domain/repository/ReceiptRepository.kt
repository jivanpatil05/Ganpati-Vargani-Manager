package com.ganpati.vargani.domain.repository

import com.ganpati.vargani.domain.model.AppSettings
import com.ganpati.vargani.domain.model.Donation
import java.io.File

interface ReceiptRepository {
    suspend fun generateReceiptPdf(donation: Donation, settings: AppSettings): File
}
