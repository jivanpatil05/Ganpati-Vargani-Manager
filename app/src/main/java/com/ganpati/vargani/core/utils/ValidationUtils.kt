package com.ganpati.vargani.core.utils

import com.ganpati.vargani.core.constants.AppConstants

/**
 * Form validation helpers for donation inputs.
 * Pure Kotlin (no Android framework APIs) so unit tests run on the JVM.
 */
object ValidationUtils {

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    fun isValidIndianMobile(mobile: String): Boolean {
        var digits = mobile.filter { it.isDigit() }
        // Allow +91 / 91 country-code prefixes commonly typed by users.
        if (digits.length == 12 && digits.startsWith("91")) {
            digits = digits.substring(2)
        } else if (digits.length == 11 && digits.startsWith("0")) {
            digits = digits.substring(1)
        }
        return digits.length == AppConstants.INDIAN_MOBILE_LENGTH && digits.first() in '6'..'9'
    }

    fun isValidAmount(amountText: String): Boolean {
        val value = amountText.toDoubleOrNull() ?: return false
        return value > 0.0
    }

    fun isValidPincode(pincode: String): Boolean {
        if (pincode.isBlank()) return true
        return pincode.length == AppConstants.PINCODE_LENGTH && pincode.all { it.isDigit() }
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return true
        return EMAIL_REGEX.matches(email.trim())
    }
}
