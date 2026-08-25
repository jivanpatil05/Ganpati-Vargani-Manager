package com.ganpati.vargani.core.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared motion tokens for consistent, modern transitions.
 */
object VarganiMotion {
    const val SHORT_MS = 180
    const val MEDIUM_MS = 320
    const val LONG_MS = 480
    const val EMPHASIS_MS = 700

    fun <T> tweenShort(): FiniteAnimationSpec<T> =
        tween(durationMillis = SHORT_MS, easing = FastOutSlowInEasing)

    fun <T> tweenMedium(): FiniteAnimationSpec<T> =
        tween(durationMillis = MEDIUM_MS, easing = FastOutSlowInEasing)

    fun <T> tweenLong(): FiniteAnimationSpec<T> =
        tween(durationMillis = LONG_MS, easing = FastOutSlowInEasing)

    fun <T> springSnappy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    fun <T> springSoft(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    val PressScale = 0.97f
    val CardElevation: Dp = 2.dp
    val CardElevationPressed: Dp = 4.dp
}
