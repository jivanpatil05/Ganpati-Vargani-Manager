package com.ganpati.vargani.core.utils

import com.ganpati.vargani.core.constants.AppConstants
import kotlin.math.pow
import kotlin.random.Random

/**
 * Normalizes Indian mobile numbers to a 10-digit form when possible.
 */
object MobileUtils {
    fun digitsOnly(raw: String): String = raw.filter { it.isDigit() }

    fun normalizeIndianMobile(raw: String): String {
        var digits = digitsOnly(raw)
        if (digits.length == 12 && digits.startsWith("91")) {
            digits = digits.substring(2)
        } else if (digits.length == 11 && digits.startsWith("0")) {
            digits = digits.substring(1)
        }
        return digits
    }

    /** E.164-style without +, e.g. 9198XXXXXXXX for WhatsApp links. */
    fun toWhatsAppPhone(raw: String): String {
        val local = normalizeIndianMobile(raw)
        return if (local.length == AppConstants.INDIAN_MOBILE_LENGTH) {
            "91$local"
        } else {
            digitsOnly(raw)
        }
    }

    fun generateOtp(length: Int = 6): String {
        val max = 10.0.pow(length).toInt()
        val value = Random.nextInt(0, max)
        return value.toString().padStart(length, '0')
    }
}
