package com.ganpati.vargani.core.constants

/**
 * Application-wide constants.
 * Centralizing literals keeps screens free of magic numbers/strings.
 */
object AppConstants {
    const val DATABASE_NAME = "ganpati_vargani.db"
    const val DATASTORE_NAME = "vargani_settings"
    const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

    const val DEFAULT_RECEIPT_PREFIX = "GV"
    const val DEFAULT_RECEIPT_START = 1L
    const val DEFAULT_ORG_NAME = "Ganpati Festival Committee"
    const val DEFAULT_ORG_ADDRESS = ""

    const val DATE_PATTERN = "dd MMM yyyy"
    const val TIME_PATTERN = "hh:mm a"
    const val DATE_TIME_PATTERN = "dd MMM yyyy, hh:mm a"
    const val ISO_DATE_PATTERN = "yyyy-MM-dd"
    const val EXPORT_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss"

    const val INDIAN_MOBILE_LENGTH = 10
    const val PINCODE_LENGTH = 6

    const val RECENT_DONATIONS_LIMIT = 8
    const val TOP_COLLECTORS_LIMIT = 5
    const val TOP_DONORS_LIMIT = 10
    const val TREND_DAYS = 14

    const val CORNER_RADIUS_DP = 16
    const val CARD_ELEVATION_DP = 2
}
