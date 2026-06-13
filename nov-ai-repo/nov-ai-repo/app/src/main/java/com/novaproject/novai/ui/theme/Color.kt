package com.novaproject.novai.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val NovDark        = Color(0xFF0D0D1A)
val NovSurface     = Color(0xFF161625)
val NovCard        = Color(0xFF1C1C2E)
val NovCardAlt     = Color(0xFF222238)

val NovCyan        = Color(0xFF00E5CC)
val NovCyanDark    = Color(0xFF00B3A0)
val NovPurple      = Color(0xFFA855F7)
val NovPurpleLight = Color(0xFFD8B4FE)
val NovBlue        = Color(0xFF3B82F6)
val NovOrange      = Color(0xFFFF7043)

val NovTextPrimary   = Color(0xFFF1F1F5)
val NovTextSecondary = Color(0xFF9090A8)
val NovDivider       = Color(0xFF2A2A40)

val ErrorRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF22C55E)

val LocalAccentColor = compositionLocalOf { NovCyan }
val LocalAccentColorDark = compositionLocalOf { NovCyanDark }

fun accentColorFromKey(key: String): Color = when (key) {
    "purple" -> NovPurple
    "orange" -> NovOrange
    else -> NovCyan
}

fun accentColorDarkFromKey(key: String): Color = when (key) {
    "purple" -> Color(0xFF7C3AED)
    "orange" -> Color(0xFFE64A19)
    else -> NovCyanDark
}
