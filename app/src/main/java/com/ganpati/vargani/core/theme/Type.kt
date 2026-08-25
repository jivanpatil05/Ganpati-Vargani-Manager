package com.ganpati.vargani.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ganpati.vargani.R

/**
 * Poppins typeface used across the entire app.
 * Bundled offline under res/font for reliable startup with no network dependency.
 */
val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private fun poppins(
    weight: FontWeight,
    size: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
) = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

val VarganiTypography = Typography(
    displayLarge = poppins(FontWeight.Bold, 57.sp, 64.sp, (-0.25).sp),
    displayMedium = poppins(FontWeight.Bold, 45.sp, 52.sp),
    displaySmall = poppins(FontWeight.Bold, 36.sp, 44.sp),
    headlineLarge = poppins(FontWeight.SemiBold, 32.sp, 40.sp),
    headlineMedium = poppins(FontWeight.SemiBold, 28.sp, 36.sp),
    headlineSmall = poppins(FontWeight.SemiBold, 24.sp, 32.sp),
    titleLarge = poppins(FontWeight.SemiBold, 22.sp, 28.sp),
    titleMedium = poppins(FontWeight.Medium, 16.sp, 24.sp, 0.15.sp),
    titleSmall = poppins(FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
    bodyLarge = poppins(FontWeight.Normal, 16.sp, 24.sp, 0.5.sp),
    bodyMedium = poppins(FontWeight.Normal, 14.sp, 20.sp, 0.25.sp),
    bodySmall = poppins(FontWeight.Normal, 12.sp, 16.sp, 0.4.sp),
    labelLarge = poppins(FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
    labelMedium = poppins(FontWeight.Medium, 12.sp, 16.sp, 0.5.sp),
    labelSmall = poppins(FontWeight.Medium, 11.sp, 16.sp, 0.5.sp),
)
