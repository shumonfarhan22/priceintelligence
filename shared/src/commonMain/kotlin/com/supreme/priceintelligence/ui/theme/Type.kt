package com.supreme.priceintelligence.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.settings.AppFontStyle

private val BaseTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

internal fun supremeTypography(
    fontStyle: AppFontStyle
): Typography {
    val fontFamily = when (fontStyle) {
        AppFontStyle.NATIVE -> FontFamily.Default
        AppFontStyle.MODERN -> FontFamily.SansSerif
        AppFontStyle.FRIENDLY -> FontFamily.Cursive
        AppFontStyle.EDITORIAL,
        AppFontStyle.CLASSIC -> FontFamily.Serif
        AppFontStyle.TECHNICAL -> FontFamily.Monospace
        AppFontStyle.COMPACT,
        AppFontStyle.SPACIOUS -> FontFamily.SansSerif
    }

    fun TextStyle.customized(): TextStyle {
        val adjustedWeight =
            if (
                fontStyle == AppFontStyle.CLASSIC &&
                (fontWeight?.weight ?: 400) < 500
            ) {
                FontWeight.Medium
            } else {
                fontWeight
            }

        val adjustedSpacing = when (fontStyle) {
            AppFontStyle.COMPACT -> (-0.15).sp
            AppFontStyle.SPACIOUS -> 0.55.sp
            else -> letterSpacing
        }

        return copy(
            fontFamily = fontFamily,
            fontWeight = adjustedWeight,
            letterSpacing = adjustedSpacing
        )
    }

    return Typography(
        displayLarge = BaseTypography.displayLarge.customized(),
        displayMedium = BaseTypography.displayMedium.customized(),
        displaySmall = BaseTypography.displaySmall.customized(),
        headlineLarge = BaseTypography.headlineLarge.customized(),
        headlineMedium = BaseTypography.headlineMedium.customized(),
        headlineSmall = BaseTypography.headlineSmall.customized(),
        titleLarge = BaseTypography.titleLarge.customized(),
        titleMedium = BaseTypography.titleMedium.customized(),
        titleSmall = BaseTypography.titleSmall.customized(),
        bodyLarge = BaseTypography.bodyLarge.customized(),
        bodyMedium = BaseTypography.bodyMedium.customized(),
        bodySmall = BaseTypography.bodySmall.customized(),
        labelLarge = BaseTypography.labelLarge.customized(),
        labelMedium = BaseTypography.labelMedium.customized(),
        labelSmall = BaseTypography.labelSmall.customized()
    )
}