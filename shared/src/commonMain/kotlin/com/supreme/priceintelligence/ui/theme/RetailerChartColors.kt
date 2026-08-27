package com.supreme.priceintelligence.ui.theme

import androidx.compose.ui.graphics.Color
import com.supreme.priceintelligence.settings.CustomRetailerChartColors
import com.supreme.priceintelligence.settings.RetailerChartPalette

internal data class RetailerChartColors(
    val amazon: Color,
    val flipkart: Color
)

internal fun RetailerChartPalette.retailerChartColors(
    customColors: CustomRetailerChartColors =
        CustomRetailerChartColors()
): RetailerChartColors =
    when (this) {
        RetailerChartPalette.ORIGINAL ->
            RetailerChartColors(
                amazon = Color(0xFFFF9900),
                flipkart = Color(0xFF2874F0)
            )

        RetailerChartPalette.EMERALD_INDIGO ->
            RetailerChartColors(
                amazon = Color(0xFF10B981),
                flipkart = Color(0xFF8B7CF6)
            )

        RetailerChartPalette.COPPER_TEAL ->
            RetailerChartColors(
                amazon = Color(0xFFE08A5B),
                flipkart = Color(0xFF2DD4BF)
            )

        RetailerChartPalette.GOLD_AMETHYST ->
            RetailerChartColors(
                amazon = Color(0xFFD6A63D),
                flipkart = Color(0xFFC084FC)
            )

        RetailerChartPalette.CORAL_SAPPHIRE ->
            RetailerChartColors(
                amazon = Color(0xFFFB7185),
                flipkart = Color(0xFF60A5FA)
            )

        RetailerChartPalette.CYAN_VIOLET ->
            RetailerChartColors(
                amazon = Color(0xFF22D3EE),
                flipkart = Color(0xFFA78BFA)
            )

        RetailerChartPalette.AMBER_SKY ->
            RetailerChartColors(
                amazon = Color(0xFFF59E0B),
                flipkart = Color(0xFF38BDF8)
            )

        RetailerChartPalette.PINK_AQUA ->
            RetailerChartColors(
                amazon = Color(0xFFF472B6),
                flipkart = Color(0xFF2DD4BF)
            )

        RetailerChartPalette.CUSTOM ->
            RetailerChartColors(
                amazon =
                    paletteColorFromHex(
                        customColors.amazonHex,
                        Color(0xFFFF9900)
                    ),
                flipkart =
                    paletteColorFromHex(
                        customColors.flipkartHex,
                        Color(0xFF2874F0)
                    )
            )
    }