package com.bitchat.android.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// Colors that match Nothing / minimalist aesthetic
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE5E5E5),        // Stark off-white for primary accents
    onPrimary = Color.Black,
    secondary = Color(0xFFFFFFFF),      // Pure white
    onSecondary = Color.Black,
    background = Color(0xFF000000),     // True deep OLED Black
    onBackground = Color(0xFFE5E5E5),   // White on black
    surface = Color(0xFF141414),        // Very dark gray for components
    onSurface = Color(0xFFE5E5E5),      // White text
    surfaceVariant = Color(0xFF1C1C1C), // Slightly lighter elevated background
    onSurfaceVariant = Color(0xFFAFAFAF), // Gray muted text
    error = Color(0xFFE51025),          // Nothing Red
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF111111),        // Near black
    onPrimary = Color.White,
    secondary = Color(0xFF000000),      // True black
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF),     // Pure white
    onBackground = Color(0xFF111111),   // Black on white
    surface = Color(0xFFF5F5F5),        // Clean light gray for cards
    onSurface = Color(0xFF111111),      // Black text
    surfaceVariant = Color(0xFFEBEBEB), // Slightly darker gray elevation
    onSurfaceVariant = Color(0xFF555555), // Muted dark text
    error = Color(0xFFE51025),          // Nothing Red
    onError = Color.White
)

@Composable
fun BitchatTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    // App-level override from ThemePreferenceManager
    val themePref by ThemePreferenceManager.themeFlow.collectAsState(initial = ThemePreference.System)
    val shouldUseDark = when (darkTheme) {
        true -> true
        false -> false
        null -> when (themePref) {
            ThemePreference.Dark -> true
            ThemePreference.Light -> false
            ThemePreference.System -> isSystemInDarkTheme()
        }
    }

    val colorScheme = if (shouldUseDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    if (!shouldUseDark) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = if (!shouldUseDark) {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else 0
            }
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
