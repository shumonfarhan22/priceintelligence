package com.supreme.priceintelligence.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InsightCustomizationTest {

    @Test
    fun expandedCustomizationRoundTripPreservesEveryChoice() {
        val original = AppCustomization(
            dashboardCardStyle = DashboardCardStyle.PRICE_FOCUSED,
            insightCustomization = InsightCustomization(
                contrastMode = AppContrastMode.HIGH,
                surfaceStyle = AppSurfaceStyle.GLASS,
                reduceTransparency = true,
                priceEmphasis = PriceEmphasis.BOLD,
                shopOverviewStartState = SectionStartState.EXPANDED,
                breakdownStartState = SectionStartState.EXPANDED,
                breakdownLayout = BreakdownLayout.COMPACT_STRIP,
                breakdownValueMode = BreakdownValueMode.COUNTS_AND_PERCENTAGES,
                prioritiesStartState = SectionStartState.EXPANDED,
                priorityProductLimit = PriorityProductLimit.TEN,
                prioritySortMode = PrioritySortMode.PERCENTAGE_GAP,
                priorityRowStyle = PriorityRowStyle.COMPACT,
                advancedInfoStartState = SectionStartState.EXPANDED,
                advancedInfoLevel = AdvancedInfoLevel.FULL,
                priceHistoryRange = PriceHistoryRange.FOURTEEN_DAYS,
                historyGraphStyle = HistoryGraphStyle.STEP,
                graphSize = GraphSize.LARGE,
                graphPointMode = GraphPointMode.ALWAYS_LATEST,
                retailerChartPalette =
                    RetailerChartPalette.CYAN_VIOLET,
                movementDefaultRetailer = MovementDefaultRetailer.FLIPKART,
                movementLayout = MovementLayout.PRODUCTS_FIRST,
                movementProductSort = MovementProductSort.PERCENTAGE_CHANGE,
                movementDirectionFilter = MovementDirectionFilter.DECREASES,
                movementGraphStyle = HistoryGraphStyle.AREA,
                movementProductGraphState = MovementProductGraphState.COLLAPSED
            )
        )

        assertEquals(
            original,
            readAppCustomization(writeAppCustomization(original))
        )
    }

    @Test
    fun olderStoredCustomizationReceivesSafeInsightDefaults() {
        val oldProfile = listOf(
            "v1",
            AppAccentColor.GOLD.name,
            "NATIVE",
            AppTextSize.STANDARD.name,
            AppDisplayDensity.COMFORTABLE.name,
            AppMotionPreference.SYSTEM.name,
            "true",
            DashboardCardStyle.DETAILED.name,
            DashboardDefaultSort.MOST_VIEWED.name,
            DashboardPageSize.TEN.name,
            PriceMovementDefaultRange.THIRTY_DAYS.name,
            PriceAlertDirection.BOTH.name,
            PriceAlertThreshold.ANY.name
        ).joinToString("|")

        assertEquals(
            InsightCustomization(),
            readAppCustomization(oldProfile).insightCustomization
        )
    }

    @Test
    fun editedPresetIsReportedAsCustom() {
        val analyst = personalizationForPreset(PersonalizationPreset.ANALYST)
        assertEquals(
            PersonalizationPreset.ANALYST,
            matchingPersonalizationPreset(analyst)
        )
        assertNull(
            matchingPersonalizationPreset(
                analyst.copy(hapticsEnabled = false)
            )
        )
    }
}