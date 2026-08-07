package com.rukatv.iptv.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme

// ── Primary Accent (legacy cian – mantenido para compatibilidad) ─────────────
val Accent = Color(0xFF00D4FF)           // Azul eléctrico (nuevo acento principal)

// ── Backgrounds ──────────────────────────────────────────────────────────────
val Background    = Color(0xFF060A12)    // Fondo principal ultra oscuro
val BackgroundAlt = Color(0xFF0A0E18)   // Fondo secundario
val Surface       = Color(0xFF0E1420)   // Superficie de tarjetas/paneles
val SurfaceAlt    = Color(0xFF131926)   // Superficie alternativa (sidebar)
val SurfaceCard   = Color(0xFF111827)   // Superficie de cards con opacidad

// ── Glassmorphism ─────────────────────────────────────────────────────────────
val GlassBg       = Color(0x1AFFFFFF)   // Fondo glass muy sutil
val GlassBorder   = Color(0x26FFFFFF)   // Borde glass
val GlassBorderFocus = Color(0x55FFFFFF) // Borde glass en focus

// ── Text Colors ───────────────────────────────────────────────────────────────
val OnSurface   = Color(0xFFF0F4FF)     // Texto primario blanco azulado
val TextPrimary = Color(0xFFF0F4FF)
val TextSecondary = Color(0xFF8A95AA)
val TextMuted   = Color(0xFF4A5568)

// ── Logo gradient colors ──────────────────────────────────────────────────────
val LogoBlue    = Color(0xFF00D4FF)     // Azul eléctrico del logo
val LogoViolet  = Color(0xFF7B2FFF)     // Violeta del logo
val LogoPink    = Color(0xFFFF3CAC)     // Rosa para gradiente extendido

// ── Semantic colors ───────────────────────────────────────────────────────────
val LiveRed     = Color(0xFFE53E3E)     // Indicador EN VIVO
val StarGold    = Color(0xFFFFC107)     // Estrella de valoración
val SuccessGreen = Color(0xFF38A169)

// ── Player Colors ─────────────────────────────────────────────────────────────
val PlayerAccent    = Color(0xFF00D4FF)     // Azul eléctrico del reproductor
val PlayerFocused   = Color(0x3300D4FF)     // Fondo elemento enfocado
val PlayerBorder    = Color(0xFF00D4FF)     // Borde elemento enfocado
val PlayerGlow      = Color(0x8000D4FF)     // Sombra glow del foco
val PlayerOverlay   = Color(0xBB000000)     // Overlay semi-transparente
val PlayerSecondary = Color(0xFF9CA3AF)     // Texto secundario del reproductor
val PlayerSurface   = Color(0xE6121212)     // Superficies de menús

// ── Gradient brushes (reutilizables) ─────────────────────────────────────────
val AccentGradient = Brush.horizontalGradient(
    colors = listOf(LogoBlue, LogoViolet)
)
val AccentGradientVertical = Brush.verticalGradient(
    colors = listOf(LogoBlue, LogoViolet)
)
val HeroGradientBottom = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color(0x00000000),
        0.4f to Color(0x55000000),
        0.72f to Color(0xDD060A12),
        1.0f to Color(0xFF060A12)
    )
)
val HeroGradientLeft = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.0f to Color(0xCC060A12),
        0.55f to Color(0x77060A12),
        1.0f to Color.Transparent
    )
)

fun tvColorScheme() = darkColorScheme(
    primary    = Accent,
    background = Background,
    surface    = Surface,
    onSurface  = OnSurface,
    onBackground = OnSurface
)

fun phoneColorScheme() = darkColorScheme(
    primary    = Accent,
    background = Background,
    surface    = Surface,
    onSurface  = OnSurface,
    onBackground = OnSurface
)

// Detect Android TV / Google TV / leanback devices so the UI can switch to a
// D-pad friendly layout (side rail, large focusable cards) instead of the
// touch-oriented phone layout (bottom navigation bar).
fun isTvDevice(context: Context): Boolean {
    val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    val isTvUi = uiMode == Configuration.UI_MODE_TYPE_TELEVISION
    val hasLeanback = context.packageManager.hasSystemFeature("android.software.leanback")
    return isTvUi || hasLeanback
}

