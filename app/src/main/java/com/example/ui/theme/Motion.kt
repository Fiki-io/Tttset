package com.example.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

object AppMotion {
    const val DurationFast = 135
    const val DurationStandard = 220
    const val DurationLong = 320

    val StandardEasing: Easing = FastOutSlowInEasing

    fun <T> fastTween(reducedMotion: Boolean = false) = tween<T>(
        durationMillis = if (reducedMotion) 0 else DurationFast,
        easing = StandardEasing
    )

    fun <T> standardTween(reducedMotion: Boolean = false) = tween<T>(
        durationMillis = if (reducedMotion) 0 else DurationStandard,
        easing = StandardEasing
    )

    fun <T> sheetSpring(reducedMotion: Boolean = false) = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = if (reducedMotion) Spring.StiffnessHigh else Spring.StiffnessMediumLow
    )
}

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: SecurityException) {
        false
    }
}
