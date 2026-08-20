package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.data.PriceRetailer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PriceHistoryInsightsTest {
    @Test
    fun summarizesLatestLowHighAndPriceDrop() {
        val history = listOf(
            entry(id = 3, price = 900.0, checkedAt = 300),
            entry(id = 2, price = 1000.0, checkedAt = 200),
            entry(id = 1, price = 950.0, checkedAt = 100)
        )

        val summary = summarizePriceHistory(history, PriceRetailer.AMAZON)!!

        assertEquals(900.0, summary.latestPrice)
        assertEquals(1000.0, summary.previousPrice)
        assertEquals(900.0, summary.lowestSavedPrice)
        assertEquals(1000.0, summary.highestSavedPrice)
        assertEquals(3, summary.observationCount)
        assertEquals(PriceMovement.LOWER, summary.movement)
        assertEquals(100.0, summary.movementAmount)
        assertEquals(10.0, summary.movementPercent)
        assertEquals(listOf(900.0, 1000.0, 950.0), summary.recentPrices)
    }

    @Test
    fun treatsTinyPriceDifferenceAsUnchanged() {
        val history = listOf(
            entry(id = 2, price = 999.995, checkedAt = 200),
            entry(id = 1, price = 1000.0, checkedAt = 100)
        )

        assertEquals(
            PriceMovement.UNCHANGED,
            summarizePriceHistory(history, PriceRetailer.AMAZON)?.movement
        )
    }

    @Test
    fun ignoresOtherRetailersAndInvalidPrices() {
        val history = listOf(
            entry(id = 1, price = 900.0, checkedAt = 100, retailer = PriceRetailer.FLIPKART),
            entry(id = 2, price = -1.0, checkedAt = 200)
        )

        assertNull(summarizePriceHistory(history, PriceRetailer.AMAZON))
    }

    private fun entry(
        id: Long,
        price: Double,
        checkedAt: Long,
        retailer: PriceRetailer = PriceRetailer.AMAZON
    ) = PriceHistoryEntry(
        id = id,
        inventoryItemId = 10,
        retailer = retailer.name,
        price = price,
        checkedAt = checkedAt
    )
}
