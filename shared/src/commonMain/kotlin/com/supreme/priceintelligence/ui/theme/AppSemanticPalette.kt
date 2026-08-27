package com.supreme.priceintelligence.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.supreme.priceintelligence.settings.AppColorPalette
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.settings.CustomAppColorPalette
import com.supreme.priceintelligence.settings.normalizePaletteHex

internal data class AppSemanticPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val competitive: Color,
    val onCompetitive: Color,
    val competitiveContainer: Color,
    val onCompetitiveContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val review: Color,
    val reviewContainer: Color
)

private data class RawAppPalette(
    val primary: Color,
    val secondary: Color,
    val competitive: Color,
    val warning: Color,
    val review: Color
)

internal fun AppCustomization.semanticPalette(
    isDarkTheme: Boolean
): AppSemanticPalette {
    val rawPalette =
        when (appColorPalette) {
            AppColorPalette.SUPREME_HARMONY ->
                RawAppPalette(
                    primary = Color(0xFF10B981),
                    secondary = Color(0xFF8B7CF6),
                    competitive = Color(0xFF34D399),
                    warning = Color(0xFFF59E0B),
                    review = Color(0xFFFB7185)
                )

            AppColorPalette.OCEAN_COPPER ->
                RawAppPalette(
                    primary = Color(0xFF3B82F6),
                    secondary = Color(0xFFE08A5B),
                    competitive = Color(0xFF14B8A6),
                    warning = Color(0xFFD6A63D),
                    review = Color(0xFFF43F5E)
                )

            AppColorPalette.ROYAL_AMETHYST ->
                RawAppPalette(
                    primary = Color(0xFFA855F7),
                    secondary = Color(0xFFD6A63D),
                    competitive = Color(0xFF22C55E),
                    warning = Color(0xFFF97316),
                    review = Color(0xFFE11D48)
                )

            AppColorPalette.CUSTOM ->
                customColorPalette.toRawPalette()
        }

    val background =
        if (isDarkTheme) {
            Color(0xFF0B0F14)
        } else {
            Color(0xFFF2EDE4)
        }

    val primary =
        ensureVisible(
            color = rawPalette.primary,
            background = background
        )

    val secondary =
        ensureVisible(
            color = rawPalette.secondary,
            background = background
        )

    val competitive =
        ensureVisible(
            color = rawPalette.competitive,
            background = background
        )

    val warning =
        ensureVisible(
            color = rawPalette.warning,
            background = background
        )

    val review =
        ensureVisible(
            color = rawPalette.review,
            background = background
        )

    return AppSemanticPalette(
        primary = primary,
        onPrimary = readableContentColor(primary),
        primaryContainer =
            roleContainer(
                roleColor = primary,
                background = background,
                isDarkTheme = isDarkTheme
            ),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = readableContentColor(secondary),
        secondaryContainer =
            roleContainer(
                roleColor = secondary,
                background = background,
                isDarkTheme = isDarkTheme
            ),
        onSecondaryContainer = secondary,
        competitive = competitive,
        onCompetitive =
            readableContentColor(competitive),
        competitiveContainer =
            roleContainer(
                roleColor = competitive,
                background = background,
                isDarkTheme = isDarkTheme
            ),
        onCompetitiveContainer = competitive,
        warning = warning,
        warningContainer =
            roleContainer(
                roleColor = warning,
                background = background,
                isDarkTheme = isDarkTheme
            ),
        review = review,
        reviewContainer =
            roleContainer(
                roleColor = review,
                background = background,
                isDarkTheme = isDarkTheme
            )
    )
}

internal fun paletteColorFromHex(
    value: String,
    fallback: Color
): Color {
    val normalized =
        normalizePaletteHex(value)
            ?: return fallback

    val red =
        normalized
            .substring(1, 3)
            .toInt(16)

    val green =
        normalized
            .substring(3, 5)
            .toInt(16)

    val blue =
        normalized
            .substring(5, 7)
            .toInt(16)

    return Color(
        red = red,
        green = green,
        blue = blue
    )
}

private fun CustomAppColorPalette.toRawPalette():
        RawAppPalette =
    RawAppPalette(
        primary =
            paletteColorFromHex(
                primaryHex,
                Color(0xFF10B981)
            ),
        secondary =
            paletteColorFromHex(
                secondaryHex,
                Color(0xFF8B7CF6)
            ),
        competitive =
            paletteColorFromHex(
                competitiveHex,
                Color(0xFF34D399)
            ),
        warning =
            paletteColorFromHex(
                warningHex,
                Color(0xFFF59E0B)
            ),
        review =
            paletteColorFromHex(
                reviewHex,
                Color(0xFFFB7185)
            )
    )

private fun roleContainer(
    roleColor: Color,
    background: Color,
    isDarkTheme: Boolean
): Color =
    lerp(
        start = roleColor,
        stop = background,
        fraction =
            if (isDarkTheme) {
                0.72f
            } else {
                0.82f
            }
    )

private fun ensureVisible(
    color: Color,
    background: Color
): Color {
    var adjustedColor = color

    val adjustmentTarget =
        if (background.luminance() < 0.50f) {
            Color.White
        } else {
            Color(0xFF111827)
        }

    repeat(10) {
        if (
            contrastRatio(
                adjustedColor,
                background
            ) >= 3.0f
        ) {
            return adjustedColor
        }

        adjustedColor =
            lerp(
                start = adjustedColor,
                stop = adjustmentTarget,
                fraction = 0.14f
            )
    }

    return adjustedColor
}

private fun readableContentColor(
    background: Color
): Color {
    val darkText =
        Color(0xFF111827)

    val whiteContrast =
        contrastRatio(
            Color.White,
            background
        )

    val darkContrast =
        contrastRatio(
            darkText,
            background
        )

    return if (whiteContrast >= darkContrast) {
        Color.White
    } else {
        darkText
    }
}

private fun contrastRatio(
    first: Color,
    second: Color
): Float {
    val firstLuminance =
        first.luminance()

    val secondLuminance =
        second.luminance()

    val lighter =
        maxOf(
            firstLuminance,
            secondLuminance
        )

    val darker =
        minOf(
            firstLuminance,
            secondLuminance
        )

    return (lighter + 0.05f) /
            (darker + 0.05f)
}