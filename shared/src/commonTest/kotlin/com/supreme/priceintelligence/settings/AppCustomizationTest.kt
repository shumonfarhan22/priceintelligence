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
            fontStyle = AppFontStyle.EDITORIAL,
            textSize = AppTextSize.LARGE,
            displayDensity = AppDisplayDensity.COMPACT,
            motionPreference = AppMotionPreference.REDUCED,
            hapticsEnabled = false,
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
