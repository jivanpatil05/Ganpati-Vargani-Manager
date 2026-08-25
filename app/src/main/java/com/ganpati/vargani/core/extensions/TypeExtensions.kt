package com.ganpati.vargani.core.extensions

import java.text.NumberFormat
import java.util.Locale

fun Double.toInr(): String =
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(this)

fun String?.orDash(): String = if (this.isNullOrBlank()) "—" else this

fun Long?.orZero(): Long = this ?: 0L
