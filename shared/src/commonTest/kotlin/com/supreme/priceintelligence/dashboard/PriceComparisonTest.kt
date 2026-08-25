package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.network.ScrapeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun handlesInvalidLegacyShopPriceWithoutCrashing() {
        val comparison = compareWithOnlinePrices(Double.NaN, 900.0, null)

        assertEquals(ShopPricePosition.INVALID_SHOP_PRICE, comparison.shopPosition)
        assertEquals(900.0, comparison.onlineLowestPrice)
        assertEquals(null, comparison.shopDifference)
        assertEquals("Price unavailable", formatIndianPrice(Double.NaN))
    }

    @Test
    fun formatsIndianCurrencyGroupingAndPaise() {
        assertEquals("₹999", formatIndianPrice(999.0))
        assertEquals("₹12,345.50", formatIndianPrice(12_345.5))
        assertEquals("₹1,23,45,678.25", formatIndianPrice(12_345_678.25))
    }

    @Test
    fun freshnessSummarySeparatesCurrentStaleMissingAndUnlinkedProducts() {
        val nowMillis =
            PRICE_FRESHNESS_WINDOW_MILLIS * 10L

        val current = inventoryItem(
            id = 1,
            name = "Current price",
            shopPrice = 1_000.0,
            amazonPrice = 900.0
        ).copy(
            amazonUrl = "https://amazon.in/current",
            amazonLastChecked =
                nowMillis -
                    PRICE_FRESHNESS_WINDOW_MILLIS / 2L
        )

        val stale = inventoryItem(
            id = 2,
            name = "Stale price",
            shopPrice = 1_000.0,
            amazonPrice = 900.0
        ).copy(
            amazonUrl = "https://amazon.in/stale",
            amazonLastChecked =
                nowMillis -
                    PRICE_FRESHNESS_WINDOW_MILLIS
        )

        val missingPrice = inventoryItem(
            id = 3,
            name = "Missing saved price",
            shopPrice = 1_000.0,
            amazonPrice = null
        ).copy(
            amazonUrl = "https://amazon.in/missing"
        )

        val withoutLinks = inventoryItem(
            id = 4,
            name = "Without retailer links",
            shopPrice = 1_000.0,
            amazonPrice = null
        )

        val summary = listOf(
            current,
            stale,
            missingPrice,
            withoutLinks
        ).buildPriceFreshnessSummary(
            nowMillis = nowMillis
        )

        assertEquals(4, summary.totalProductCount)
        assertEquals(3, summary.linkedProductCount)
        assertEquals(1, summary.currentProductCount)
        assertEquals(2, summary.needsCheckCount)
        assertEquals(
            1,
            summary.missingRetailerLinksCount
        )

        assertFalse(
            current.needsPriceCheck(nowMillis)
        )
        assertTrue(
            stale.needsPriceCheck(nowMillis)
        )
        assertTrue(
            missingPrice.needsPriceCheck(nowMillis)
        )
        assertFalse(
            withoutLinks.needsPriceCheck(nowMillis)
        )
    }

    @Test
    fun freshnessRequiresEveryConfiguredRetailerToBeCurrent() {
        val nowMillis =
            PRICE_FRESHNESS_WINDOW_MILLIS * 10L

        val item = inventoryItem(
            id = 5,
            name = "Two retailer product",
            shopPrice = 1_000.0,
            amazonPrice = 900.0
        ).copy(
            amazonUrl = "https://amazon.in/two-retailers",
            amazonLastChecked = nowMillis,
            flipkartUrl =
                "https://flipkart.com/two-retailers",
            flipkartLastPrice = 950.0,
            flipkartLastChecked =
                nowMillis -
                    PRICE_FRESHNESS_WINDOW_MILLIS -
                    1L
        )

        assertTrue(
            item.needsPriceCheck(nowMillis)
        )

        assertFalse(
            item.copy(
                flipkartLastChecked = nowMillis
            ).needsPriceCheck(nowMillis)
        )
    }

    @Test
    fun decisionSummarySeparatesShopWinsReviewsAndBothUnavailableReasons() {
        val items = listOf(
            inventoryItem(id = 1, name = "Shop winner", shopPrice = 900.0, amazonPrice = 1_000.0),
            inventoryItem(id = 2, name = "Needs review", shopPrice = 2_000.0, amazonPrice = 1_500.0),
            inventoryItem(id = 3, name = "Not checked", shopPrice = 500.0, amazonPrice = null),
            inventoryItem(id = 4, name = "Bad shop price", shopPrice = Double.NaN, amazonPrice = 900.0)
        )

        val summary = items.buildDecisionSummary(livePriceCards = emptyList())

        assertEquals(1, summary.shopCompetitiveCount)
        assertEquals(1, summary.onlineCheaperCount)
        assertEquals(1, summary.noOnlinePriceCount)
        assertEquals(1, summary.invalidShopPriceCount)
        assertEquals(1, summary.priorityProducts.size)
        assertEquals("Needs review", summary.priorityProducts.first().productName)
        assertEquals(500.0, summary.priorityProducts.first().gap)
    }

    @Test
    fun decisionSummaryRanksPriorityProductsFromMostToLeastGap() {
        val items = listOf(
            inventoryItem(id = 1, name = "Small gap", shopPrice = 1_100.0, amazonPrice = 1_000.0),
            inventoryItem(id = 2, name = "Biggest gap", shopPrice = 3_000.0, amazonPrice = 1_000.0),
            inventoryItem(id = 3, name = "Middle gap", shopPrice = 2_000.0, amazonPrice = 1_000.0)
        )

        val summary = items.buildDecisionSummary(livePriceCards = emptyList())

        assertEquals(
            listOf("Biggest gap", "Middle gap", "Small gap"),
            summary.priorityProducts.map { product -> product.productName }
        )
    }

    @Test
    fun decisionSummaryCoversTheWholeListNotJustOnePage() {
        val items = (1..25).map { id ->
            inventoryItem(id = id.toLong(), name = "Product $id", shopPrice = 100.0, amazonPrice = 90.0)
        }

        val summary = items.buildDecisionSummary(livePriceCards = emptyList())

        assertEquals(25, summary.comparedCount)
        assertEquals(25, summary.onlineCheaperCount)
    }

    @Test
    fun livePriceOverridesStoredLastPriceWhenBothArePresent() {
        val item = inventoryItem(id = 5, name = "Live check", shopPrice = 1_000.0, amazonPrice = 950.0)
        val liveCard = ProductCardUiState(
            item = item,
            amazonResult = ScrapeResult(price = 700.0)
        )

        val summary = listOf(item).buildDecisionSummary(livePriceCards = listOf(liveCard))

        assertEquals(1, summary.onlineCheaperCount)
        assertEquals(300.0, summary.priorityProducts.first().gap)
        assertEquals(1, summary.livePriceProductCount)
    }

    private fun inventoryItem(
        id: Long,
        name: String,
        shopPrice: Double,
        amazonPrice: Double?
    ): InventoryItem = InventoryItem(
        id = id,
        productName = name,
        shopPrice = shopPrice,
        amazonLastPrice = amazonPrice,
        createdAt = 1,
        updatedAt = 1
    )
}
