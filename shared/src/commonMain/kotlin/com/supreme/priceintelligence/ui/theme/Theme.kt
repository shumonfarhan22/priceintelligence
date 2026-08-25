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
    primaryContainer = Color(0xFFFFF0C2),
    onPrimaryContainer = Color(0xFF4B3300),
    secondary = Color(0xFF7B5D2E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E7CA),
    onSecondaryContainer = Color(0xFF493614),
    tertiary = Color(0xFF237A4B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2F1E7),
    onTertiaryContainer = Color(0xFF153D29),
    background = Color(0xFFF6F1E8),
    onBackground = Color(0xFF1C1915),
    surface = Color(0xFFFFFEFC),
    onSurface = Color(0xFF1C1915),
    surfaceVariant = Color(0xFFF2EBE0),
    onSurfaceVariant = Color(0xFF665F56),
    error = Color(0xFFB54838),
    onError = Color.White,
    errorContainer = Color(0xFFF7DDD7),
    onErrorContainer = Color(0xFF6C231A),
    outline = Color(0xFFE4D9C8)
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