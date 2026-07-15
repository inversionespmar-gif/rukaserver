package com.rukatv.iptv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme

// Cyan accent (IPTV Smarters style)
val Accent = Color(0xFF2ED3C6)
val Background = Color(0xFF0C0F16)
val Surface = Color(0xFF161C28)
val SurfaceAlt = Color(0xFF1B2230)
val OnSurface = Color(0xFFE8EEF7)

fun tvColorScheme() = darkColorScheme(
    primary = Accent,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    onBackground = OnSurface
)

fun phoneColorScheme() = darkColorScheme(
    primary = Accent,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    onBackground = OnSurface
)
