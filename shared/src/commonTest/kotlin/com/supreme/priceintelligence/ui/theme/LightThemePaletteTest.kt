package com.supreme.priceintelligence.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.supreme.priceintelligence.settings.AppColorPalette
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.settings.CustomAppColorPalette
import com.supreme.priceintelligence.settings.RetailerChartPalette
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LightThemePaletteTest {

    @Test
    fun lightSemanticRolesRemainReadable() {
        val palette =
            AppCustomization()
                .semanticPalette(isDarkTheme = false)

        val background = Color(0xFFF2EDE4)

        listOf(
            palette.primary,
            palette.secondary,
            palette.competitive,
            palette.warning,
            palette.review
        ).forEach { roleColor ->
            assertTrue(
                contrastRatio(
                    roleColor,
                    background
                ) >= 4.5f
            )
        }

        assertTrue(
            contrastRatio(
                palette.onPrimaryContainer,
                palette.primaryContainer
            ) >= 4.5f
        )
        assertTrue(
            contrastRatio(
                palette.onSecondaryContainer,
                palette.secondaryContainer
            ) >= 4.5f
        )
        assertTrue(
            contrastRatio(
                palette.onCompetitiveContainer,
                palette.competitiveContainer
            ) >= 4.5f
        )
        assertTrue(
            contrastRatio(
                palette.onWarningContainer,
                palette.warningContainer
            ) >= 4.5f
        )
        assertTrue(
            contrastRatio(
                palette.onReviewContainer,
                palette.reviewContainer
            ) >= 4.5f
        )
    }

    @Test
    fun veryLightCustomColoursAreCorrectedForLightTheme() {
        val palette =
            AppCustomization(
                appColorPalette =
                    AppColorPalette.CUSTOM,
                customColorPalette =
                    CustomAppColorPalette(
                        primaryHex = "#FFFFFF",
                        secondaryHex = "#F8FAFC",
                        competitiveHex = "#D1FAE5",
                        warningHex = "#FEF3C7",
                        reviewHex = "#FFE4E6"
                    )
            ).semanticPalette(isDarkTheme = false)

        val background = Color(0xFFF2EDE4)

        listOf(
            palette.primary,
            palette.secondary,
            palette.competitive,
            palette.warning,
            palette.review
        ).forEach { roleColor ->
            assertTrue(
                contrastRatio(
                    roleColor,
                    background
                ) >= 4.5f
            )
        }
    }

    @Test
    fun lightRetailerGraphsKeepVisibleLines() {
        val chartSurface = Color(0xFFEBE3D7)

        RetailerChartPalette.entries
            .forEach { chartPalette ->
                val colors =
                    chartPalette.retailerChartColors(
                        isDarkTheme = false
                    )

                assertTrue(
                    contrastRatio(
                        colors.amazon,
                        chartSurface
                    ) >= 3.0f
                )
                assertTrue(
                    contrastRatio(
                        colors.flipkart,
                        chartSurface
                    ) >= 3.0f
                )
            }
    }

    @Test
    fun lightTintedSurfacesAreOpaque() {
        val lightTint =
            SupremeLightColors.tintedSurface(
                roleColor = Color(0xFF8B7CF6),
                strength = 0.15f
            )

        val darkTint =
            SupremeDarkColors.tintedSurface(
                roleColor = Color(0xFF8B7CF6),
                strength = 0.15f
            )

        assertEquals(1f, lightTint.alpha)
        assertTrue(
            abs(darkTint.alpha - 0.15f) < 0.002f
        )
    }

    private fun contrastRatio(
        first: Color,
        second: Color
    ): Float {
        val lighter = maxOf(
            first.luminance(),
            second.luminance()
        )

        val darker = minOf(
            first.luminance(),
            second.luminance()
        )

        return (lighter + 0.05f) /
            (darker + 0.05f)
    }
}
