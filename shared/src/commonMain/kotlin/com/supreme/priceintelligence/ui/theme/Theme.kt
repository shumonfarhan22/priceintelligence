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
    primary = Color(0xFF9A6700),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E2A5),
    onPrimaryContainer = Color(0xFF4B3300),
    secondary = Color(0xFF7B5D2E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E3C5),
    onSecondaryContainer = Color(0xFF493614),
    tertiary = Color(0xFF2F7D55),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDEBDD),
    onTertiaryContainer = Color(0xFF153D29),
    background = Color(0xFFF7F4ED),
    onBackground = Color(0xFF1C1915),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF1C1915),
    surfaceVariant = Color(0xFFEEE8DE),
    onSurfaceVariant = Color(0xFF70685D),
    error = Color(0xFFB84A3A),
    onError = Color.White,
    errorContainer = Color(0xFFF5D8D2),
    onErrorContainer = Color(0xFF6C231A),
    outline = Color(0xFFDED4C3)
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