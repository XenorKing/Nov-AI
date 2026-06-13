package com.novaproject.novai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun NovAITheme(
    accentColorKey: String = "cyan",
    content: @Composable () -> Unit
) {
    val accent = accentColorFromKey(accentColorKey)
    val accentDark = accentColorDarkFromKey(accentColorKey)

    val colorScheme = darkColorScheme(
        primary = accent,
        onPrimary = NovDark,
        primaryContainer = accentDark,
        secondary = NovPurple,
        onSecondary = Color.White,
        background = NovDark,
        surface = NovSurface,
        surfaceVariant = NovCard,
        onBackground = NovTextPrimary,
        onSurface = NovTextPrimary,
        onSurfaceVariant = NovTextSecondary,
        error = ErrorRed,
        outline = NovDivider
    )

    CompositionLocalProvider(
        LocalAccentColor provides accent,
        LocalAccentColorDark provides accentDark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NovAITypography,
            content = content
        )
    }
}
