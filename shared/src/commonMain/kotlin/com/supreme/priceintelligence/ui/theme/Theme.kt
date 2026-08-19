package com.supreme.priceintelligence.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = Brand,
    onPrimary = Bg,
    primaryContainer = BrandLight,
    onPrimaryContainer = Brand,
    secondary = Accent,
    onSecondary = TextPrimary,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = TextMuted,
    error = Danger,
    onError = TextPrimary,
    errorContainer = DangerBg,
    onErrorContainer = Danger,
    outline = SurfaceAlt,
)

/**
 * This app always renders in the dark "Premium Dark Mode" palette from
 * Color.kt (ported from theme.py) — there's no light-mode variant, same
 * as the original app had. No dynamic-color or system-theme
 * switching needed, so this is simpler than the wizard's default Theme.kt.
 */
@Composable
fun PriceIntelligenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}