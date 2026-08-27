package com.supreme.priceintelligence.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.supreme.priceintelligence.settings.AppAccentColor
import com.supreme.priceintelligence.settings.AppContrastMode
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.settings.AppSurfaceStyle
import com.supreme.priceintelligence.settings.AppThemeMode
import com.supreme.priceintelligence.settings.InsightCustomization

private data class AccentPalette(
    val darkPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
    val lightPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color
)

private fun AppAccentColor.palette(): AccentPalette =
    when (this) {
        AppAccentColor.SUPREME -> AccentPalette(
            darkPrimary = Color(0xFF10B981),
            darkContainer = Color(0xFF123626),
            darkOnContainer = Color(0xFF6EE7B7),
            lightPrimary = Color(0xFF835A00),
            lightContainer = Color(0xFFF0DFB0),
            lightOnContainer = Color(0xFF4C3400)
        )

        AppAccentColor.EMERALD -> AccentPalette(
            darkPrimary = Color(0xFF10B981),
            darkContainer = Color(0xFF123626),
            darkOnContainer = Color(0xFF6EE7B7),
            lightPrimary = Color(0xFF047857),
            lightContainer = Color(0xFFD9F4E8),
            lightOnContainer = Color(0xFF064E3B)
        )

        AppAccentColor.GOLD -> AccentPalette(
            darkPrimary = Color(0xFFD6A63D),
            darkContainer = Color(0xFF3B2B08),
            darkOnContainer = Color(0xFFF6D77D),
            lightPrimary = Color(0xFF835A00),
            lightContainer = Color(0xFFF0DFB0),
            lightOnContainer = Color(0xFF4C3400)
        )

        AppAccentColor.INDIGO -> AccentPalette(
            darkPrimary = Color(0xFFA99AFB),
            darkContainer = Color(0xFF29234F),
            darkOnContainer = Color(0xFFD9D2FF),
            lightPrimary = Color(0xFF5B4BC4),
            lightContainer = Color(0xFFE5E0FF),
            lightOnContainer = Color(0xFF30256F)
        )

        AppAccentColor.OCEAN -> AccentPalette(
            darkPrimary = Color(0xFF38BDF8),
            darkContainer = Color(0xFF0B3345),
            darkOnContainer = Color(0xFFA5E4FF),
            lightPrimary = Color(0xFF036A91),
            lightContainer = Color(0xFFD6F0FC),
            lightOnContainer = Color(0xFF003A52)
        )

        AppAccentColor.TEAL -> AccentPalette(
            darkPrimary = Color(0xFF2DD4BF),
            darkContainer = Color(0xFF0B3933),
            darkOnContainer = Color(0xFF99F6E4),
            lightPrimary = Color(0xFF0F766E),
            lightContainer = Color(0xFFD2F3EE),
            lightOnContainer = Color(0xFF134E4A)
        )

        AppAccentColor.SAPPHIRE -> AccentPalette(
            darkPrimary = Color(0xFF60A5FA),
            darkContainer = Color(0xFF172F54),
            darkOnContainer = Color(0xFFBFDBFE),
            lightPrimary = Color(0xFF1D4ED8),
            lightContainer = Color(0xFFDBEAFE),
            lightOnContainer = Color(0xFF1E3A8A)
        )

        AppAccentColor.AMETHYST -> AccentPalette(
            darkPrimary = Color(0xFFC084FC),
            darkContainer = Color(0xFF38204D),
            darkOnContainer = Color(0xFFE9D5FF),
            lightPrimary = Color(0xFF7E22CE),
            lightContainer = Color(0xFFF0DEFF),
            lightOnContainer = Color(0xFF4A126F)
        )

        AppAccentColor.COPPER -> AccentPalette(
            darkPrimary = Color(0xFFE08A5B),
            darkContainer = Color(0xFF44271A),
            darkOnContainer = Color(0xFFFFD2BA),
            lightPrimary = Color(0xFF9A4825),
            lightContainer = Color(0xFFF6DED1),
            lightOnContainer = Color(0xFF58230F)
        )
    }

private fun supremeDarkColorScheme(
    palette: AppSemanticPalette
) = darkColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer =
        palette.onPrimaryContainer,
    secondary = palette.secondary,
    onSecondary = palette.onSecondary,
    secondaryContainer =
        palette.secondaryContainer,
    onSecondaryContainer =
        palette.onSecondaryContainer,
    tertiary = palette.competitive,
    onTertiary = palette.onCompetitive,
    tertiaryContainer =
        palette.competitiveContainer,
    onTertiaryContainer =
        palette.onCompetitiveContainer,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = TextMuted,
    error = palette.review,
    onError =
        readableThemeContentColor(
            palette.review
        ),
    errorContainer =
        palette.reviewContainer,
    onErrorContainer = palette.review,
    outline = SurfaceAlt
)

private fun supremeLightColorScheme(
    palette: AppSemanticPalette
) = lightColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer =
        palette.onPrimaryContainer,
    secondary = palette.secondary,
    onSecondary = palette.onSecondary,
    secondaryContainer =
        palette.secondaryContainer,
    onSecondaryContainer =
        palette.onSecondaryContainer,
    tertiary = palette.competitive,
    onTertiary = palette.onCompetitive,
    tertiaryContainer =
        palette.competitiveContainer,
    onTertiaryContainer =
        palette.onCompetitiveContainer,
    background = Color(0xFFF2EDE4),
    onBackground = Color(0xFF1D1B17),
    surface = Color(0xFFFAF8F3),
    onSurface = Color(0xFF1D1B17),
    surfaceVariant = Color(0xFFEBE3D7),
    onSurfaceVariant = Color(0xFF5C5852),
    error = palette.review,
    onError =
        readableThemeContentColor(
            palette.review
        ),
    errorContainer =
        palette.reviewContainer,
    onErrorContainer = palette.review,
    outline = Color(0xFFB8AA96)
)

private fun readableThemeContentColor(
    background: Color
): Color {
    val luminance =
        (
                background.red * 0.2126f +
                        background.green * 0.7152f +
                        background.blue * 0.0722f
                )

    return if (luminance > 0.56f) {
        Color(0xFF111827)
    } else {
        Color.White
    }
}

private fun personalizedSupremeColors(
    isDarkTheme: Boolean,
    customization: InsightCustomization,
    palette: AppSemanticPalette
): SupremeColors {
    val base = if (isDarkTheme) {
        SupremeDarkColors
    } else {
        SupremeLightColors
    }

    val surfaceStyle = if (customization.reduceTransparency) {
        AppSurfaceStyle.SOLID
    } else {
        customization.surfaceStyle
    }

    val panel = when {
        isDarkTheme && surfaceStyle == AppSurfaceStyle.SOLID ->
            Color(0xFF151A21)

        isDarkTheme && surfaceStyle == AppSurfaceStyle.GLASS ->
            Color.White.copy(alpha = 0.025f)

        !isDarkTheme && surfaceStyle == AppSurfaceStyle.SOLID ->
            Color(0xFFFFFDF8)

        !isDarkTheme && surfaceStyle == AppSurfaceStyle.GLASS ->
            Color.White.copy(alpha = 0.72f)

        else -> base.panel
    }

    val panelMuted = when {
        isDarkTheme && surfaceStyle == AppSurfaceStyle.SOLID ->
            Color(0xFF1B2129)

        isDarkTheme && surfaceStyle == AppSurfaceStyle.GLASS ->
            Color.White.copy(alpha = 0.045f)

        !isDarkTheme && surfaceStyle == AppSurfaceStyle.SOLID ->
            Color(0xFFF1EADF)

        !isDarkTheme && surfaceStyle == AppSurfaceStyle.GLASS ->
            Color.White.copy(alpha = 0.58f)

        else -> base.panelMuted
    }

    val border = if (
        customization.contrastMode == AppContrastMode.HIGH
    ) {
        if (isDarkTheme) {
            Color.White.copy(alpha = 0.22f)
        } else {
            Color(0xFF887A67)
        }
    } else {
        base.border
    }

    return base.copy(
        panel = panel,
        panelStrong = if (surfaceStyle == AppSurfaceStyle.SOLID) {
            panel
        } else {
            base.panelStrong
        },
        panelMuted = panelMuted,
        field = if (surfaceStyle == AppSurfaceStyle.SOLID) {
            if (isDarkTheme) Color(0xFF10151B) else Color(0xFFFFFDF8)
        } else {
            base.field
        },
        border = border,
        divider = if (
            customization.contrastMode == AppContrastMode.HIGH
        ) {
            border.copy(
                alpha =
                    if (isDarkTheme) {
                        0.72f
                    } else {
                        0.58f
                    }
            )
        } else {
            base.divider
        },
        warning = palette.warning,
        warningContainer =
            palette.warningContainer,
        competitive = palette.competitive,
        competitiveContainer =
            palette.competitiveContainer
    )
}

@Composable
fun PriceIntelligenceTheme(
    themeMode: AppThemeMode,
    customization: AppCustomization,
    content: @Composable (isDarkTheme: Boolean) -> Unit
) {
    val isDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val palette =
        customization.semanticPalette(
            isDarkTheme = isDarkTheme
        )

    val personalizedColors =
        personalizedSupremeColors(
            isDarkTheme = isDarkTheme,
            customization =
                customization.insightCustomization,
            palette = palette
        )
    val systemDensity = LocalDensity.current
    val customizedDensity = Density(
        density = systemDensity.density,
        fontScale =
            systemDensity.fontScale *
                customization.textSize.scale
    )

    CompositionLocalProvider(
        LocalDensity provides customizedDensity,
        LocalSupremeColors provides personalizedColors
    ) {
        MaterialTheme(
            colorScheme = if (isDarkTheme) {
                supremeDarkColorScheme(palette)
            } else {
                supremeLightColorScheme(palette)
            },
            typography = supremeTypography(
                customization.fontStyle
            )
        ) {
            content(isDarkTheme)
        }
    }
}