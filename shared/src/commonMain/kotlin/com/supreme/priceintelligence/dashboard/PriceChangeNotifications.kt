package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.data.PriceRetailer
import kotlin.math.roundToLong

enum class DetectedPriceDirection {
    LOWER,
    HIGHER
}

data class DetectedPriceChange(
    val productId: Long,
    val productName: String,
    val retailer: PriceRetailer,
    val oldPrice: Double,
    val newPrice: Double,
    val direction: DetectedPriceDirection,
    val detectedAt: Long
)

data class PriceChangeNotificationText(
    val title: String,
    val body: String
)

interface PriceChangeNotifier {
    fun requestPermission()

    fun publishPriceChanges(
        changes: List<DetectedPriceChange>
    )
}

object NoOpPriceChangeNotifier : PriceChangeNotifier {
    override fun requestPermission() = Unit

    override fun publishPriceChanges(
        changes: List<DetectedPriceChange>
    ) = Unit
}

internal fun detectPriceChanges(
    item: InventoryItem,
    amazonPrice: Double?,
    flipkartPrice: Double?,
    detectedAt: Long
): List<DetectedPriceChange> = buildList {
    createDetectedPriceChange(
        item = item,
        retailer = PriceRetailer.AMAZON,
        oldPrice = item.amazonLastPrice,
        newPrice = amazonPrice,
        detectedAt = detectedAt
    )?.let(::add)

    createDetectedPriceChange(
        item = item,
        retailer = PriceRetailer.FLIPKART,
        oldPrice = item.flipkartLastPrice,
        newPrice = flipkartPrice,
        detectedAt = detectedAt
    )?.let(::add)
}

internal fun buildPriceChangeNotificationText(
    changes: List<DetectedPriceChange>
): PriceChangeNotificationText {
    val distinctChanges =
        changes.distinctBy { change ->
            Triple(
                change.productId,
                change.retailer,
                priceInPaise(change.newPrice)
            )
        }

    val singleChange = distinctChanges.singleOrNull()

    if (singleChange != null) {
        val movementText =
            if (
                singleChange.direction ==
                DetectedPriceDirection.LOWER
            ) {
                "dropped"
            } else {
                "increased"
            }

        return PriceChangeNotificationText(
            title =
                "${singleChange.retailer.displayName()} price $movementText",
            body =
                "${singleChange.productName}: " +
                        "${formatIndianPrice(singleChange.oldPrice)} → " +
                        formatIndianPrice(singleChange.newPrice)
        )
    }

    val lowerCount =
        distinctChanges.count { change ->
            change.direction ==
                    DetectedPriceDirection.LOWER
        }

    val higherCount =
        distinctChanges.count { change ->
            change.direction ==
                    DetectedPriceDirection.HIGHER
        }

    val productCount =
        distinctChanges
            .map { change -> change.productId }
            .distinct()
            .size

    val movementParts = buildList {
        if (lowerCount > 0) {
            add("$lowerCount lower")
        }

        if (higherCount > 0) {
            add("$higherCount higher")
        }
    }

    return PriceChangeNotificationText(
        title =
            "${distinctChanges.size} online prices changed",
        body = buildString {
            append(productCount)
            append(
                if (productCount == 1) {
                    " product"
                } else {
                    " products"
                }
            )

            if (movementParts.isNotEmpty()) {
                append(" • ")
                append(movementParts.joinToString(" • "))
            }
        }
    )
}

private fun createDetectedPriceChange(
    item: InventoryItem,
    retailer: PriceRetailer,
    oldPrice: Double?,
    newPrice: Double?,
    detectedAt: Long
): DetectedPriceChange? {
    if (
        oldPrice == null ||
        newPrice == null ||
        !oldPrice.isFinite() ||
        !newPrice.isFinite() ||
        oldPrice <= 0.0 ||
        newPrice <= 0.0 ||
        detectedAt <= 0L
    ) {
        return null
    }

    if (
        priceInPaise(oldPrice) ==
        priceInPaise(newPrice)
    ) {
        return null
    }

    return DetectedPriceChange(
        productId = item.id,
        productName = item.productName,
        retailer = retailer,
        oldPrice = oldPrice,
        newPrice = newPrice,
        direction =
            if (newPrice < oldPrice) {
                DetectedPriceDirection.LOWER
            } else {
                DetectedPriceDirection.HIGHER
            },
        detectedAt = detectedAt
    )
}

private fun PriceRetailer.displayName(): String =
    when (this) {
        PriceRetailer.AMAZON -> "Amazon"
        PriceRetailer.FLIPKART -> "Flipkart"
    }

private fun priceInPaise(
    price: Double
): Long = (price * 100.0).roundToLong()