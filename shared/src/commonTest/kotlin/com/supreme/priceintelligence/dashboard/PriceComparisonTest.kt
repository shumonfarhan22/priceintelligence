package com.supreme.priceintelligence.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PriceComparisonTest {

    @Test
    fun identifiesWhenOnlineIsCheaper() {
        val comparison = compareWithOnlinePrices(
            shopPrice = 50_000.0,
            amazonPrice = 47_500.0,
            flipkartPrice = 49_000.0
        )

        assertEquals(47_500.0, comparison.onlineLowestPrice)
        assertEquals(2_500.0, comparison.shopDifference)
        assertEquals(5.0, comparison.shopDifferencePercent)
        assertEquals(ShopPricePosition.HIGHER, comparison.shopPosition)
    }

    @Test
    fun identifiesCompetitiveAndMatchingShopPrices() {
        assertEquals(
            ShopPricePosition.LOWER,
            compareWithOnlinePrices(900.0, 1_000.0, null).shopPosition
        )
        assertEquals(
            ShopPricePosition.MATCHED,
            compareWithOnlinePrices(1_000.0, 1_000.009, null).shopPosition
        )
    }

    @Test
    fun ignoresInvalidOnlinePrices() {
        val comparison = compareWithOnlinePrices(
            shopPrice = 1_000.0,
            amazonPrice = Double.NaN,
            flipkartPrice = -1.0
        )

        assertNull(comparison.onlineLowestPrice)
        assertNull(comparison.shopDifference)
        assertEquals(ShopPricePosition.NO_ONLINE_PRICE, comparison.shopPosition)
    }

    @Test
    fun formatsIndianCurrencyGroupingAndPaise() {
        assertEquals("₹999", formatIndianPrice(999.0))
        assertEquals("₹12,345.50", formatIndianPrice(12_345.5))
        assertEquals("₹1,23,45,678.25", formatIndianPrice(12_345_678.25))
    }
}
