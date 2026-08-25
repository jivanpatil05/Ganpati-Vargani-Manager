package com.ganpati.vargani.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun isValidIndianMobile_validNumbers_returnsTrue() {
        assertThat(ValidationUtils.isValidIndianMobile("9876543210")).isTrue()
        assertThat(ValidationUtils.isValidIndianMobile("6123456789")).isTrue()
        assertThat(ValidationUtils.isValidIndianMobile("7890123456")).isTrue()
    }

    @Test
    fun isValidIndianMobile_ignoresNonDigits() {
        assertThat(ValidationUtils.isValidIndianMobile("+91 98765-43210")).isTrue()
    }

    @Test
    fun isValidIndianMobile_invalidNumbers_returnsFalse() {
        assertThat(ValidationUtils.isValidIndianMobile("1234567890")).isFalse()
        assertThat(ValidationUtils.isValidIndianMobile("987654321")).isFalse()
        assertThat(ValidationUtils.isValidIndianMobile("98765432101")).isFalse()
        assertThat(ValidationUtils.isValidIndianMobile("")).isFalse()
    }

    @Test
    fun isValidAmount_positiveValues_returnsTrue() {
        assertThat(ValidationUtils.isValidAmount("100")).isTrue()
        assertThat(ValidationUtils.isValidAmount("0.01")).isTrue()
        assertThat(ValidationUtils.isValidAmount("9999.99")).isTrue()
    }

    @Test
    fun isValidAmount_zeroOrNegative_returnsFalse() {
        assertThat(ValidationUtils.isValidAmount("0")).isFalse()
        assertThat(ValidationUtils.isValidAmount("-50")).isFalse()
        assertThat(ValidationUtils.isValidAmount("")).isFalse()
        assertThat(ValidationUtils.isValidAmount("abc")).isFalse()
    }

    @Test
    fun isValidPincode_blank_returnsTrue() {
        assertThat(ValidationUtils.isValidPincode("")).isTrue()
        assertThat(ValidationUtils.isValidPincode("   ")).isTrue()
    }

    @Test
    fun isValidPincode_validSixDigits_returnsTrue() {
        assertThat(ValidationUtils.isValidPincode("411001")).isTrue()
    }

    @Test
    fun isValidPincode_invalidLength_returnsFalse() {
        assertThat(ValidationUtils.isValidPincode("41100")).isFalse()
        assertThat(ValidationUtils.isValidPincode("4110011")).isFalse()
    }

    @Test
    fun isValidEmail_blank_returnsTrue() {
        assertThat(ValidationUtils.isValidEmail("")).isTrue()
    }

    @Test
    fun isValidEmail_validAddress_returnsTrue() {
        assertThat(ValidationUtils.isValidEmail("donor@example.com")).isTrue()
    }

    @Test
    fun isValidEmail_invalidAddress_returnsFalse() {
        assertThat(ValidationUtils.isValidEmail("not-an-email")).isFalse()
        assertThat(ValidationUtils.isValidEmail("@example.com")).isFalse()
    }
}
