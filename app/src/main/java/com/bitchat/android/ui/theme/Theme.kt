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
// True OLED blacks layered with warm carbon grays.
// Primary = pure white for maximum contrast. Tertiary = accent red.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF1E1E1E),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFFB0B0B0),
    onSecondary = Color(0xFF0A0A0A),
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFD0D0D0),
    tertiary = Color(0xFFD93025),            // Accent red – sparingly
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3D0E0A),
    onTertiaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),          // True OLED Black
    onBackground = Color(0xFFEAEAEA),
    surface = Color(0xFF0E0E0E),             // Level 1 – cards
    onSurface = Color(0xFFEAEAEA),
    surfaceVariant = Color(0xFF1A1A1A),      // Level 2 – elevated
    onSurfaceVariant = Color(0xFF9E9E9E),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF252525),
    error = Color(0xFFD93025),
    onError = Color.White,
    inverseSurface = Color(0xFFE4E4E4),
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFF1A1A1A)
)

// ─── Light Palette ───────────────────────────────────────────────────
// Paper white with warm neutral grays: editorial, airy, museum-like.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0A0A0A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0F0F0),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF666666),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF333333),
    tertiary = Color(0xFFD93025),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDE9),
    onTertiaryContainer = Color(0xFF3D0E0A),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0E0E0E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E0E0E),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF666666),
    surfaceTint = Color.Transparent,
    outline = Color(0xFFDEDEDE),
    outlineVariant = Color(0xFFEEEEEE),
    error = Color(0xFFD93025),
    onError = Color.White,
    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = Color(0xFFE4E4E4),
    inversePrimary = Color(0xFFE4E4E4)
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
