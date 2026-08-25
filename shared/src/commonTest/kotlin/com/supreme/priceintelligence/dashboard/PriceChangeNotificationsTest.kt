package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.data.PriceRetailer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PriceChangeNotificationsTest {

    @Test
    fun detectsAmazonAndFlipkartPriceChanges() {
        val item = InventoryItem(
            id = 7L,
            productName = "Mixer Grinder",
            shopPrice = 3500.0,
            amazonLastPrice = 3200.0,
            flipkartLastPrice = 3300.0
        )

        val changes = detectPriceChanges(
            item = item,
            amazonPrice = 3100.0,
            flipkartPrice = 3400.0,
            detectedAt = 1000L
        )

        assertEquals(2, changes.size)

        assertEquals(
            DetectedPriceDirection.LOWER,
            changes.first {
                it.retailer == PriceRetailer.AMAZON
            }.direction
        )

        assertEquals(
            DetectedPriceDirection.HIGHER,
            changes.first {
                it.retailer == PriceRetailer.FLIPKART
            }.direction
        )
    }

    @Test
    fun ignoresUnchangedPricesAndFirstChecks() {
        val item = InventoryItem(
            id = 8L,
            productName = "Bottle",
            shopPrice = 1200.0,
            amazonLastPrice = 999.0,
            flipkartLastPrice = null
        )

        val changes = detectPriceChanges(
            item = item,
            amazonPrice = 999.0,
            flipkartPrice = 950.0,
            detectedAt = 2000L
        )

        assertTrue(changes.isEmpty())
    }

    @Test
    fun buildsReadableNotificationText() {
        val notification =
            buildPriceChangeNotificationText(
                listOf(
                    DetectedPriceChange(
                        productId = 9L,
                        productName =
                            "Pressure Cooker",
                        retailer =
                            PriceRetailer.AMAZON,
                        oldPrice = 3000.0,
                        newPrice = 2800.0,
                        direction =
                            DetectedPriceDirection.LOWER,
                        detectedAt = 3000L
                    )
                )
            )

        assertEquals(
            "Amazon price dropped",
            notification.title
        )

        assertTrue(
            notification.body.contains(
                "Pressure Cooker"
            )
        )

        assertTrue(
            notification.body.contains(
                "₹3,000"
            )
        )

        assertTrue(
            notification.body.contains(
                "₹2,800"
            )
        )
    }
}