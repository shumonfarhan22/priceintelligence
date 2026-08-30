package com.supreme.priceintelligence.settings

import com.supreme.priceintelligence.dashboard.DetectedPriceChange
import com.supreme.priceintelligence.dashboard.DetectedPriceDirection
import com.supreme.priceintelligence.dashboard.filterPriceChangesForAlerts
import com.supreme.priceintelligence.data.PriceRetailer
import kotlin.test.Test
import kotlin.test.assertEquals

class AppCustomizationTest {

    @Test
    fun customizationRoundTripPreservesEveryChoice() {
        val original = AppCustomization(
            accentColor = AppAccentColor.AMETHYST,
            appColorPalette =
                AppColorPalette.CUSTOM,
            customColorPalette =
                CustomAppColorPalette(
                    primaryHex = "#3B82F6",
                    secondaryHex = "#E08A5B",
                    competitiveHex = "#14B8A6",
                    warningHex = "#D6A63D",
                    reviewHex = "#F43F5E"
                ),
            fontStyle = AppFontStyle.POPPINS,
            textSize = AppTextSize.LARGE,
            displayDensity = AppDisplayDensity.COMPACT,
            motionPreference = AppMotionPreference.REDUCED,
            launchTileIconStyle =
                LaunchTileIconStyle.DATA,
            hapticsEnabled = false,
            automaticPriceChecksEnabled = false,
            dashboardCardStyle = DashboardCardStyle.COMPACT,
            dashboardDefaultSort = DashboardDefaultSort.BEST_SAVING,
            dashboardPageSize = DashboardPageSize.TWENTY,
            priceMovementDefaultRange = PriceMovementDefaultRange.FOURTEEN_DAYS,
            priceAlertDirection = PriceAlertDirection.DECREASES_ONLY,
            priceAlertThreshold = PriceAlertThreshold.PERCENT_5
        )

        assertEquals(
            original,
            readAppCustomization(
                writeAppCustomization(original)
            )
        )
    }

    @Test
    fun namedPersonalizationPresetsSurviveRoundTrip() {
        val savedProfile =
            writeAppCustomization(
                AppCustomization(
                    fontStyle =
                        AppFontStyle.TECHNICAL,
                    textSize =
                        AppTextSize.COMFORTABLE
                )
            )

        val original =
            AppCustomization(
                savedPersonalizationPresets =
                    listOf(
                        SavedPersonalizationPreset(
                            name = "My Dark Shop",
                            themeMode =
                                AppThemeMode.DARK,
                            advancedModeEnabled = true,
                            priceChangeNotificationsEnabled =
                                true,
                            customizationProfile =
                                savedProfile
                        ),
                        SavedPersonalizationPreset(
                            name = "Compact Analytics",
                            themeMode =
                                AppThemeMode.LIGHT,
                            advancedModeEnabled = true,
                            priceChangeNotificationsEnabled =
                                false,
                            customizationProfile =
                                savedProfile
                        )
                    )
            )

        assertEquals(
            original,
            readAppCustomization(
                writeAppCustomization(original)
            )
        )
    }

    @Test
    fun customHexColoursAreNormalizedAndValidated() {
        assertEquals(
            "#10B981",
            normalizePaletteHex("10b981")
        )

        assertEquals(
            "#8B7CF6",
            normalizePaletteHex("  #8b7cf6  ")
        )

        assertEquals(
            null,
            normalizePaletteHex("#XYZ123")
        )

        assertEquals(
            null,
            normalizePaletteHex("#12345")
        )
    }

    @Test
    fun oldFontChoicesMigrateToNewSafeChoices() {
        val expectedStyles = mapOf(
            "NATIVE" to AppFontStyle.SYSTEM,
            "MODERN" to AppFontStyle.INTER,
            "FRIENDLY" to AppFontStyle.POPPINS,
            "EDITORIAL" to AppFontStyle.LATO,
            "CLASSIC" to AppFontStyle.LATO,
            "TECHNICAL" to AppFontStyle.TECHNICAL,
            "COMPACT" to AppFontStyle.ROBOTO,
            "SPACIOUS" to AppFontStyle.MONTSERRAT
        )

        expectedStyles.forEach {
                (storedName, expectedStyle) ->
            val restored = readAppCustomization(
                "v1|SUPREME|$storedName"
            )

            assertEquals(
                expectedStyle,
                restored.fontStyle
            )
        }
    }

    @Test
    fun invalidOrOlderProfileUsesSafeDefaults() {
        assertEquals(
            AppCustomization(),
            readAppCustomization("broken|profile")
        )
    }

    @Test
    fun alertFiltersUseShopBusinessDirectionAndThreshold() {
        val decrease = change(
            direction = DetectedPriceDirection.LOWER,
            oldPrice = 1000.0,
            newPrice = 960.0
        )

        val increase = change(
            direction = DetectedPriceDirection.HIGHER,
            oldPrice = 1000.0,
            newPrice = 1060.0
        )

        val result = filterPriceChangesForAlerts(
            changes = listOf(decrease, increase),
            customization = AppCustomization(
                priceAlertDirection =
                    PriceAlertDirection.INCREASES_ONLY,
                priceAlertThreshold =
                    PriceAlertThreshold.RUPEES_50
            )
        )

        assertEquals(listOf(increase), result)
    }

    private fun change(
        direction: DetectedPriceDirection,
        oldPrice: Double,
        newPrice: Double
    ) = DetectedPriceChange(
        productId = 1L,
        productName = "Test product",
        retailer = PriceRetailer.AMAZON,
        oldPrice = oldPrice,
        newPrice = newPrice,
        direction = direction,
        detectedAt = 1000L
    )
}
