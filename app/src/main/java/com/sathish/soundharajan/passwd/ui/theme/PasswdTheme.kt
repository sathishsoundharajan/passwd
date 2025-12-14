package com.sathish.soundharajan.passwd.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary500,
    onPrimary = Color.White,
    secondary = AccentPurple,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = BackgroundDarkStart, // We will use Box with gradient, but this is fallback
    surface = GlassDark,
    onSurface = TextWhite,
    onBackground = TextWhite,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = Primary500,
    onPrimary = Color.White,
    secondary = AccentPurple,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = BackgroundLightStart,
    surface = GlassLight,
    onSurface = TextBlack,
    onBackground = TextBlack,
    error = ErrorRed
)

@Composable
fun PasswdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Turn OFF dynamic color to ensure our Glass Theme shines
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb() // Transparent for edge-to-edge
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
