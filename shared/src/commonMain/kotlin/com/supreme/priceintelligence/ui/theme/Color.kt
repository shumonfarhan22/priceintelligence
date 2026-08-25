package com.supreme.priceintelligence.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Supreme Dark base palette
val Bg = Color(0xFF0B0F14)
val Surface = Color(0xFF1E2128)
val SurfaceAlt = Color(0xFF313540)

val Brand = Color(0xFF10B981)
val BrandLight = Color(0xFF123626)
val Accent = Color(0xFF8B7CF6)

val TextPrimary = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val TextLight = Color(0xFF5B6472)

val Danger = Color(0xFFF87171)
val DangerBg = Color(0xFF3F1717)

val NavUnselected = Color(0xFF7C8794)
val NavSelected = Color(0xFF10B981)

@Immutable
data class SupremeColors(
    val isDark: Boolean,
    val panel: Color,
    val panelStrong: Color,
    val panelMuted: Color,
    val field: Color,
    val border: Color,
    val divider: Color,
    val warning: Color,
    val warningContainer: Color,
    val competitive: Color,
    val competitiveContainer: Color,
    val navigationUnselected: Color,
    val imagePanel: Color,
    val scrim: Color
)

internal val SupremeDarkColors = SupremeColors(
    isDark = true,
    panel = Color.White.copy(alpha = 0.04f),
    panelStrong = Color(0xFF14181D),
    panelMuted = Color.White.copy(alpha = 0.06f),
    field = Color(0xFF0F1216),
    border = Color.White.copy(alpha = 0.10f),
    divider = Color(0xFF2A313C),
    warning = Color(0xFFF59E0B),
    warningContainer = Color(0xFF3C2A08),
    competitive = Color(0xFF10B981),
    competitiveContainer = Color(0xFF123626),
    navigationUnselected = Color(0xFF7C8794),
    imagePanel = Color(0xFFF8FAFC),
    scrim = Color.Black
)

internal val SupremeLightColors = SupremeColors(
    isDark = false,
    panel = Color(0xFFFFFEFC),
    panelStrong = Color.White,
    panelMuted = Color(0xFFF2EBE0),
    field = Color(0xFFFFFEFC),
    border = Color(0xFFE4D9C8),
    divider = Color(0xFFEEE5D8),
    warning = Color(0xFF9A6700),
    warningContainer = Color(0xFFFFF0C2),
    competitive = Color(0xFF237A4B),
    competitiveContainer = Color(0xFFE2F1E7),
    navigationUnselected = Color(0xFF756D63),
    imagePanel = Color(0xFFFAF7F1),
    scrim = Color.Black
)

internal val LocalSupremeColors =
    staticCompositionLocalOf { SupremeDarkColors }

val MaterialTheme.supremeColors: SupremeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSupremeColors.current