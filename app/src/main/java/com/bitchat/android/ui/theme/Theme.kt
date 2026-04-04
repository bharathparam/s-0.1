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

// ─── Dark Palette ────────────────────────────────────────────────────
// WhatsApp Dark Mode style
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00A884), // Accent green (Floating buttons, checks)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF005C4B), // Outgoing message bubble
    onPrimaryContainer = Color(0xFFE9EDEF), // Text on outgoing
    secondary = Color(0xFF202C33),
    onSecondary = Color(0xFFE9EDEF),
    secondaryContainer = Color(0xFF182229),
    onSecondaryContainer = Color(0xFF8696A0),
    tertiary = Color(0xFF53BDEB), // Links or blue text
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF027EB5),
    onTertiaryContainer = Color.White,
    background = Color(0xFF0B141A), // Chat background
    onBackground = Color(0xFFE9EDEF),
    surface = Color(0xFF202C33), // App bar, incoming messages
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = Color(0xFF2A3942), // Dialogs, secondary surface
    onSurfaceVariant = Color(0xFF8696A0), // Muted text, timestamps
    surfaceTint = Color.Transparent,
    outline = Color(0xFF3A3A3A),
    error = Color(0xFFEF9A9A),
    onError = Color.Black
)

// ─── Light Palette ───────────────────────────────────────────────────
// WhatsApp Light Mode style
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008069), // App bar mostly, and accents
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9FDD3), // Outgoing message bubble
    onPrimaryContainer = Color(0xFF111B21), // Text on outgoing
    secondary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF111B21),
    secondaryContainer = Color(0xFFF0F2F5),
    onSecondaryContainer = Color(0xFF667781),
    tertiary = Color(0xFF027EB5), // Links
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF53BDEB),
    onTertiaryContainer = Color.Black,
    background = Color(0xFFEFE7DE), // Chat background
    onBackground = Color(0xFF111B21),
    surface = Color(0xFFFFFFFF), // App bar (sometimes), incoming messages
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFF0F2F5), // Secondary surfaces
    onSurfaceVariant = Color(0xFF667781), // Muted text
    surfaceTint = Color.Transparent,
    outline = Color(0xFFD1D7DB),
    error = Color(0xFFD32F2F),
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
            @Suppress("DEPRECATION")
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
