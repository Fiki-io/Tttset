package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioDarkColorScheme = darkColorScheme(
    primary = AppColors.AccentBlue,
    onPrimary = AppColors.OnAccent,
    secondary = AppColors.SurfaceInteractive,
    onSecondary = AppColors.TextPrimary,
    background = AppColors.BackgroundBottom,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.SurfaceCard,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceInteractive,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.BorderSubtle,
    error = AppColors.StatusError,
    onError = AppColors.OnAccent
)

@Composable
fun TDStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
