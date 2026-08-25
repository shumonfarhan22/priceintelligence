package com.supreme.priceintelligence.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.supreme.priceintelligence.settings.AppThemeMode

private val SupremeDarkColorScheme = darkColorScheme(
    primary = Brand,
    onPrimary = Bg,
    primaryContainer = BrandLight,
    onPrimaryContainer = Brand,
    secondary = Accent,
    onSecondary = TextPrimary,
    tertiary = Brand,
    onTertiary = Bg,
    tertiaryContainer = BrandLight,
    onTertiaryContainer = Brand,
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
    outline = SurfaceAlt
)

private val SupremeLightColorScheme = lightColorScheme(
    primary = Color(0xFF835A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0DFB0),
    onPrimaryContainer = Color(0xFF4C3400),
    secondary = Color(0xFF6B5330),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9DDC8),
    onSecondaryContainer = Color(0xFF3C2D18),
    tertiary = Color(0xFF126A43),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCEADF),
    onTertiaryContainer = Color(0xFF123B29),
    background = Color(0xFFF2EDE4),
    onBackground = Color(0xFF1D1B17),
    surface = Color(0xFFFAF8F3),
    onSurface = Color(0xFF1D1B17),
    surfaceVariant = Color(0xFFEBE3D7),
    onSurfaceVariant = Color(0xFF5C5852),
    error = Color(0xFFA43C31),
    onError = Color.White,
    errorContainer = Color(0xFFF2DDD8),
    onErrorContainer = Color(0xFF66241E),
    outline = Color(0xFFB8AA96)
)

@Composable
fun PriceIntelligenceTheme(
    themeMode: AppThemeMode,
    content: @Composable (isDarkTheme: Boolean) -> Unit
) {
    val isDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    CompositionLocalProvider(
        LocalSupremeColors provides if (isDarkTheme) {
            SupremeDarkColors
        } else {
            SupremeLightColors
        }
    ) {
        MaterialTheme(
            colorScheme = if (isDarkTheme) {
                SupremeDarkColorScheme
            } else {
                SupremeLightColorScheme
            },
            typography = Typography
        ) {
            content(isDarkTheme)
        }
    }
}