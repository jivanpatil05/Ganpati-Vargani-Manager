package com.ganpati.vargani.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri

/**
 * Opens WhatsApp with a pre-filled OTP message for the given mobile number.
 *
 * True "number not registered on WhatsApp" checks require WhatsApp Business API.
 * Locally we detect: WhatsApp not installed, or the chat intent cannot be resolved.
 */
object WhatsAppOtpHelper {

    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    sealed class SendResult {
        data object Success : SendResult()
        data object WhatsAppNotInstalled : SendResult()
        data object NoWhatsAppAccountOrSendFailed : SendResult()
    }

    fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return isPackageInstalled(pm, WHATSAPP) || isPackageInstalled(pm, WHATSAPP_BUSINESS)
    }

    fun sendOtp(
        context: Context,
        mobile: String,
        otp: String,
        appName: String,
    ): SendResult {
        if (!isWhatsAppInstalled(context)) {
            return SendResult.WhatsAppNotInstalled
        }

        val phone = MobileUtils.toWhatsAppPhone(mobile)
        if (phone.length < 10) {
            return SendResult.NoWhatsAppAccountOrSendFailed
        }

        val body = "$appName OTP: $otp\n\nDo not share this code with anyone. Valid for 5 minutes."
        val uri = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(body)}".toUri()

        val packaged = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            when {
                isPackageInstalled(context.packageManager, WHATSAPP) -> setPackage(WHATSAPP)
                isPackageInstalled(context.packageManager, WHATSAPP_BUSINESS) ->
                    setPackage(WHATSAPP_BUSINESS)
            }
        }

        return try {
            val intent = if (packaged.resolveActivity(context.packageManager) != null) {
                packaged
            } else {
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) == null) {
                return SendResult.WhatsAppNotInstalled
            }
            context.startActivity(intent)
            SendResult.Success
        } catch (_: ActivityNotFoundException) {
            SendResult.WhatsAppNotInstalled
        } catch (_: Exception) {
            SendResult.NoWhatsAppAccountOrSendFailed
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean =
        try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}
