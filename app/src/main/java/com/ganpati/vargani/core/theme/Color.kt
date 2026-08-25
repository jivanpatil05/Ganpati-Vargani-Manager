package com.ganpati.vargani.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette — Ganpati Vargani Manager
 * Cool sky-blue system with separate light / dark tokens.
 */

// —— Light mode ——
val LightPrimary = Color(0xFF3DA9FC)
val LightPrimaryContainer = Color(0xFFEAF6FF)
val LightBackground = Color(0xFFF8FBFD)
val LightSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF111827)
val LightTextSecondary = Color(0xFF4B5563)
val LightSuccess = Color(0xFF22C55E)
val LightWarning = Color(0xFFF59E0B)
val LightError = Color(0xFFEF4444)
val LightOutline = Color(0xFFD1D5DB)

// —— Dark mode ——
val DarkPrimary = Color(0xFF63B8FF)
val DarkPrimaryContainer = Color(0xFF1A4A72)
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF162032)
val DarkSurfaceVariant = Color(0xFF1C2738)
val DarkTextPrimary = Color(0xFFFBFAFC)
val DarkTextSecondary = Color(0xFFCBD5E1)
val DarkSuccess = Color(0xFF4ADE80)
val DarkWarning = Color(0xFFFBBF24)
val DarkError = Color(0xFFF87171)
val DarkOutline = Color(0xFF334155)

// Aliases used across existing screens (map to light tokens; Theme picks dark variants)
val OrangePrimary = LightPrimary
val OrangePrimaryDark = Color(0xFF1D8FE8)
val OrangePrimaryLight = Color(0xFF7CC5FD)
val GoldAccent = LightWarning
val GoldAccentDark = Color(0xFFD97706)
val GoldAccentLight = Color(0xFFFCD34D)

val LightSurfaceVariant = LightPrimaryContainer
val LightOnBackground = LightTextPrimary
val LightOnSurface = LightTextPrimary
val LightOnSurfaceVariant = LightTextSecondary

val DarkOnBackground = DarkTextPrimary
val DarkOnSurface = DarkTextPrimary
val DarkOnSurfaceVariant = DarkTextSecondary

// Semantic (light defaults; dark overrides in Theme)
val SuccessGreen = LightSuccess
val LeafGreen = Color(0xFF4ADE80)
val WarningAmber = LightWarning
val ErrorRed = LightError
val InfoSky = LightPrimary
val CashGreen = LightSuccess
val UpiBlue = LightPrimary

// Soft fills
val SoftOrange = LightPrimaryContainer
val SoftGold = Color(0xFFFEF3C7)
val SoftGreen = Color(0xFFDCFCE7)
val SoftBlue = LightPrimaryContainer

// Chart palette
val ChartColors = listOf(
    LightPrimary,
    LightSuccess,
    LightWarning,
    Color(0xFF8B5CF6),
    LightError,
    Color(0xFF06B6D4),
    Color(0xFFEC4899),
    Color(0xFF64748B),
)

// Brand gradient — primary blue glow for hero cards
val BrandGradientStart = LightPrimary
val BrandGradientMid = Color(0xFF2B9CF0)
val BrandGradientEnd = Color(0xFF1D8FE8)

// Legacy name aliases (screens may still reference these)
val Marigold = LightPrimary
val CopperOrange = OrangePrimaryDark
val GoldenSand = LightWarning
val DeepTerracotta = LightError
val PeacockBlue = LightPrimary
val IvoryWhite = LightBackground
val SoftCream = LightPrimaryContainer
val WarmStone = Color(0xFFE2E8F0)
val LightTaupe = LightOutline
val StoneGray = LightTextSecondary
val Charcoal = DarkBackground
