package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VybDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = CyanNeon,
    secondary = VioletNeon,
    onSecondary = Color.White,
    secondaryContainer = Slate850,
    onSecondaryContainer = VioletNeon,
    tertiary = EmeraldNeon,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    outline = CardBorder,
    outlineVariant = Slate700,
    error = RoseNeon,
    onError = Color.White
)

@Composable
fun VybTheme(
    darkTheme: Boolean = true, // Default to VYB's premium dark signature theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = VybDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Slate950.toArgb()
            window.navigationBarColor = Slate950.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = VybTheme(darkTheme, dynamicColor, content)
