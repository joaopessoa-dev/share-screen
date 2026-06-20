package com.example.sharescreen.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ShareScreenColorScheme = darkColorScheme(
    // Primary colors
    primary = Primary,
    onPrimary = Fg1,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Fg1,

    // Secondary colors (using accent)
    secondary = Accent,
    onSecondary = Background,
    secondaryContainer = Surface2,
    onSecondaryContainer = Fg1,

    // Tertiary colors
    tertiary = PrimaryLight,
    onTertiary = Background,
    tertiaryContainer = Surface2,
    onTertiaryContainer = Fg1,

    // Error colors
    error = Error,
    onError = Fg1,
    errorContainer = Error.copy(alpha = 0.12f),
    onErrorContainer = Error,

    // Background and surface
    background = Background,
    onBackground = Fg1,
    surface = Surface,
    onSurface = Fg1,
    surfaceVariant = Surface2,
    onSurfaceVariant = Fg2,

    // Outline
    outline = Fg4,
    outlineVariant = Fg4,

    // Inverse
    inverseSurface = Fg1,
    inverseOnSurface = Background,
    inversePrimary = PrimaryDark,

    // Scrim
    scrim = Background.copy(alpha = 0.8f)
)

@Composable
fun ShareScreenTheme(
    darkTheme: Boolean = true, // Always dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = ShareScreenColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Background.toArgb()
            window.navigationBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShareScreenTypography,
        shapes = ShareScreenShapes,
        content = content
    )
}
