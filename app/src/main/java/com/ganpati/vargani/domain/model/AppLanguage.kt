package com.ganpati.vargani.domain.model

/**
 * Supported UI languages for the app.
 */
enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    MARATHI("mr");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}
