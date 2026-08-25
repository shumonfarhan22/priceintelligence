package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.data.PriceRetailer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopPriceMovementTest {

    private val day =
        PRICE_MOVEMENT_DAY_MILLIS

    private val now =
        day * 40L

    private val product =
        InventoryItem(
            id = 1L,
            productName = "Pressure Cooker",
            shopPrice = 3000.0
        )

    @Test
    fun buildsRetailerPriceChangesFromHistory() {
        val snapshot =
            buildShopPriceMovementSnapshot(
                items = listOf(product),
                history = sampleHistory(),
                nowMillis = now
            )

        assertEquals(
            1,
            snapshot.products.size
        )

        assertEquals(
            3,
            snapshot.products
                .single()
                .changes
                .size
        )

        assertEquals(
            2,
            snapshot.products
                .single()
                .changes
                .count {
                    it.direction ==
                            DetectedPriceDirection
                                .LOWER
                }
        )

        assertEquals(
            1,
            snapshot.products
                .single()
                .changes
                .count {
                    it.direction ==
                            DetectedPriceDirection
                                .HIGHER
                }
        )
    }

    @Test
    fun filtersMovementByTimeAndRetailer() {
        val snapshot =
            buildShopPriceMovementSnapshot(
                items = listOf(product),
                history = sampleHistory(),
                nowMillis = now
            )

        val oneDayAmazon =
            buildShopPriceMovementView(
                snapshot = snapshot,
                range =
                    ShopMovementRange
                        .ONE_DAY,
                retailerFilter =
                    ShopMovementRetailerFilter
                        .AMAZON
            )

        assertEquals(
            1,
            oneDayAmazon.changes.size
        )

        assertEquals(
            PriceRetailer.AMAZON,
            oneDayAmazon
                .changes
                .single()
                .retailer
        )

        assertEquals(
            DetectedPriceDirection.HIGHER,
            oneDayAmazon
                .changes
                .single()
                .direction
        )
    }

    @Test
    fun unchangedPricesDoNotCreateMovements() {
        val history = listOf(
            historyEntry(
                id = 1L,
                retailer =
                    PriceRetailer.AMAZON,
                price = 1000.0,
                checkedAt = now - day
            ),
            historyEntry(
                id = 2L,
                retailer =
                    PriceRetailer.AMAZON,
                price = 1000.0,
                checkedAt = now
            )
        )

        val snapshot =
            buildShopPriceMovementSnapshot(
                items = listOf(product),
                history = history,
                nowMillis = now
            )

        assertTrue(
            snapshot.products.isEmpty()
        )
    }

    private fun sampleHistory():
            List<PriceHistoryEntry> =
        listOf(
            historyEntry(
                id = 1L,
                retailer =
                    PriceRetailer.AMAZON,
                price = 3000.0,
                checkedAt =
                    now - day * 10L
            ),
            historyEntry(
                id = 2L,
                retailer =
                    PriceRetailer.AMAZON,
                price = 2800.0,
                checkedAt =
                    now - day * 8L
            ),
            historyEntry(
                id = 3L,
                retailer =
                    PriceRetailer.AMAZON,
                price = 2900.0,
                checkedAt =
                    now - day / 2L
            ),
            historyEntry(
                id = 4L,
                retailer =
                    PriceRetailer.FLIPKART,
                price = 3100.0,
                checkedAt =
                    now - day * 7L
            ),
            historyEntry(
                id = 5L,
                retailer =
                    PriceRetailer.FLIPKART,
                price = 3000.0,
                checkedAt =
                    now - day * 2L
            )
        )

    private fun historyEntry(
        id: Long,
        retailer: PriceRetailer,
        price: Double,
        checkedAt: Long
    ): PriceHistoryEntry =
        PriceHistoryEntry(
            id = id,
            inventoryItemId =
                product.id,
            retailer =
                retailer.name,
            price = price,
            checkedAt = checkedAt
        )
}