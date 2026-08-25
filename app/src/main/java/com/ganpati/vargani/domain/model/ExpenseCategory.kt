package com.ganpati.vargani.domain.model

/**
 * Festival expense categories for outgoing tracking.
 */
enum class ExpenseCategory {
    PUJA_ITEMS,
    DECORATION,
    PRASAD,
    SOUND_LIGHT,
    TRANSPORT,
    RENT,
    UTILITIES,
    MISC;

    companion object {
        fun fromStorage(value: String): ExpenseCategory =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: MISC
    }
}
