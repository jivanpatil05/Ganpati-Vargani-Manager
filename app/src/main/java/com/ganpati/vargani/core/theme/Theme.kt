package com.ganpati.vargani.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = LightWarning,
    onSecondary = Color.White,
    secondaryContainer = SoftGold,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = LightSuccess,
    onTertiary = Color.White,
    tertiaryContainer = SoftGreen,
    onTertiaryContainer = Color(0xFF14532D),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightPrimaryContainer,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = Color(0xFFE5E7EB),
    error = LightError,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkWarning,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = SoftGold,
    tertiary = DarkSuccess,
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = SoftGreen,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF334155),
    error = DarkError,
    onError = DarkBackground,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

val LocalVarganiColors = staticCompositionLocalOf { VarganiExtendedColors() }

data class VarganiExtendedColors(
    val gold: Color = LightWarning,
    val cash: Color = LightSuccess,
    val upi: Color = LightPrimary,
    val success: Color = LightSuccess,
    val warning: Color = LightWarning,
    val terracotta: Color = LightError,
    val peacock: Color = LightPrimary,
    val softOrange: Color = SoftOrange,
    val softGold: Color = SoftGold,
    val softGreen: Color = SoftGreen,
    val softBlue: Color = SoftBlue,
    val chartColors: List<Color> = ChartColors,
    val brandGradient: List<Color> = listOf(BrandGradientStart, BrandGradientMid, BrandGradientEnd),
)

@Composable
fun VarganiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val extendedColors = if (darkTheme) {
        VarganiExtendedColors(
            gold = DarkWarning,
            cash = DarkSuccess,
            upi = DarkPrimary,
            success = DarkSuccess,
            warning = DarkWarning,
            terracotta = DarkError,
            peacock = DarkPrimary,
            softOrange = DarkPrimaryContainer,
            softGold = DarkWarning.copy(alpha = 0.22f),
            softGreen = DarkSuccess.copy(alpha = 0.22f),
            softBlue = DarkPrimaryContainer,
            brandGradient = listOf(DarkPrimaryContainer, Color(0xFF236089), DarkPrimary),
        )
    } else {
        VarganiExtendedColors()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Keep system bars transparent; draw page background behind them (edge-to-edge).
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalVarganiColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VarganiTypography,
            shapes = VarganiShapes,
        ) {
            ProvideTextStyle(
                value = VarganiTypography.bodyLarge,
                content = content,
            )
        }
    }
}

object VarganiThemeExtras {
    val extendedColors: VarganiExtendedColors
        @Composable
        get() = LocalVarganiColors.current
}
