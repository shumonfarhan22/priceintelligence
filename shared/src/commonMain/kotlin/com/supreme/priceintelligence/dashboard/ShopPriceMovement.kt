package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.data.PriceRetailer
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

internal const val PRICE_MOVEMENT_DAY_MILLIS =
    24L * 60L * 60L * 1000L

enum class ShopMovementRange(
    val days: Int,
    val buttonLabel: String
) {
    ONE_DAY(
        days = 1,
        buttonLabel = "1D"
    ),
    SEVEN_DAYS(
        days = 7,
        buttonLabel = "7D"
    ),
    FOURTEEN_DAYS(
        days = 14,
        buttonLabel = "14D"
    ),
    THIRTY_DAYS(
        days = 30,
        buttonLabel = "30D"
    )
}

enum class ShopMovementRetailerFilter(
    val buttonLabel: String
) {
    ALL("All"),
    AMAZON("Amazon"),
    FLIPKART("Flipkart")
}

data class ShopPricePoint(
    val price: Double,
    val checkedAt: Long
)

data class ShopPriceChange(
    val productId: Long,
    val retailer: PriceRetailer,
    val oldPrice: Double,
    val newPrice: Double,
    val checkedAt: Long,
    val direction: DetectedPriceDirection
) {
    val difference: Double
        get() = (newPrice - oldPrice).absoluteValue

    val percentage: Double
        get() = if (oldPrice > 0.0) {
            difference / oldPrice * 100.0
        } else {
            0.0
        }
}

data class ShopProductMovement(
    val item: InventoryItem,
    val amazonHistory: List<ShopPricePoint>,
    val flipkartHistory: List<ShopPricePoint>,
    val changes: List<ShopPriceChange>
)

data class ShopPriceMovementSnapshot(
    val products: List<ShopProductMovement> =
        emptyList(),
    val productsWithHistory:
        List<ShopProductMovement> =
        emptyList(),
    val generatedAt: Long = 0L
)

data class ShopProductMovementView(
    val item: InventoryItem,
    val amazonHistory: List<ShopPricePoint>,
    val flipkartHistory: List<ShopPricePoint>,
    val changes: List<ShopPriceChange>
) {
    val latestChange: ShopPriceChange?
        get() = changes.maxByOrNull {
            it.checkedAt
        }
}

data class ShopPriceMovementView(
    val products: List<ShopProductMovementView> =
        emptyList(),
    val changes: List<ShopPriceChange> =
        emptyList()
) {
    val lowerCount: Int
        get() = changes.count {
            it.direction ==
                    DetectedPriceDirection.LOWER
        }

    val higherCount: Int
        get() = changes.count {
            it.direction ==
                    DetectedPriceDirection.HIGHER
        }

    val changedProductCount: Int
        get() = products.size
}

internal fun buildShopPriceMovementSnapshot(
    items: List<InventoryItem>,
    history: List<PriceHistoryEntry>,
    nowMillis: Long
): ShopPriceMovementSnapshot {
    val itemById =
        items.associateBy { item -> item.id }

    val pointsByKey =
        mutableMapOf<
                ProductRetailerKey,
                MutableList<ShopPricePoint>
                >()

    history.forEach { entry ->
        val item =
            itemById[entry.inventoryItemId]
                ?: return@forEach

        if (
            item.id <= 0L ||
            !entry.price.isFinite() ||
            entry.price <= 0.0 ||
            entry.checkedAt <= 0L
        ) {
            return@forEach
        }

        val retailer =
            PriceRetailer.entries
                .firstOrNull { option ->
                    option.name == entry.retailer
                }
                ?: return@forEach

        val key = ProductRetailerKey(
            productId = item.id,
            retailer = retailer
        )

        pointsByKey
            .getOrPut(key) {
                mutableListOf()
            }
            .add(
                ShopPricePoint(
                    price = entry.price,
                    checkedAt = entry.checkedAt
                )
            )
    }

    val productsWithHistory =
        items.mapNotNull { item ->
            val amazonHistory =
                pointsByKey[
                    ProductRetailerKey(
                        productId = item.id,
                        retailer =
                            PriceRetailer.AMAZON
                    )
                ]
                    .orEmpty()
                    .cleanAndSort()

            val flipkartHistory =
                pointsByKey[
                    ProductRetailerKey(
                        productId = item.id,
                        retailer =
                            PriceRetailer.FLIPKART
                    )
                ]
                    .orEmpty()
                    .cleanAndSort()

            val changes =
                buildRetailerChanges(
                    productId = item.id,
                    retailer =
                        PriceRetailer.AMAZON,
                    points = amazonHistory
                ) +
                        buildRetailerChanges(
                            productId = item.id,
                            retailer =
                                PriceRetailer.FLIPKART,
                            points = flipkartHistory
                        )

            if (
                amazonHistory.isEmpty() &&
                flipkartHistory.isEmpty()
            ) {
                null
            } else {
                ShopProductMovement(
                    item = item,
                    amazonHistory = amazonHistory,
                    flipkartHistory =
                        flipkartHistory,
                    changes =
                        changes.sortedByDescending {
                            it.checkedAt
                        }
                )
            }
        }
            .sortedByDescending { product ->
                product.changes
                    .maxOfOrNull {
                        it.checkedAt
                    }
                    ?: 0L
            }

    return ShopPriceMovementSnapshot(
        products =
            productsWithHistory.filter { product ->
                product.changes.isNotEmpty()
            },
        productsWithHistory =
            productsWithHistory,
        generatedAt = nowMillis
    )
}

internal fun buildShopPriceMovementView(
    snapshot: ShopPriceMovementSnapshot,
    range: ShopMovementRange,
    retailerFilter:
    ShopMovementRetailerFilter,
    notificationTarget:
        PriceMovementNotificationTarget? = null
): ShopPriceMovementView {
    if (snapshot.generatedAt <= 0L) {
        return ShopPriceMovementView()
    }

    val cutoff =
        (
                snapshot.generatedAt -
                        range.days *
                        PRICE_MOVEMENT_DAY_MILLIS
                ).coerceAtLeast(0L)

    val sourceProducts =
        if (notificationTarget == null) {
            snapshot.products
        } else {
            snapshot.productsWithHistory
                .ifEmpty {
                    snapshot.products
                }
        }

    val productViews =
        sourceProducts.mapNotNull { product ->
            val normalChanges =
                product.changes
                    .asSequence()
                    .filter { change ->
                        change.checkedAt >= cutoff
                    }
                    .filter { change ->
                        retailerFilter.matches(
                            change.retailer
                        )
                    }
                    .sortedByDescending {
                        it.checkedAt
                    }
                    .toList()

            val notificationChange =
                notificationTarget
                    ?.takeIf { target ->
                        target.productId ==
                            product.item.id &&
                            target.detectedAt >= cutoff &&
                            retailerFilter.matches(
                                target.retailer
                            )
                    }
                    ?.let { target ->
                        ShopPriceChange(
                            productId =
                                target.productId,
                            retailer =
                                target.retailer,
                            oldPrice =
                                target.oldPrice,
                            newPrice =
                                target.newPrice,
                            checkedAt =
                                target.detectedAt,
                            direction =
                                target.direction
                        )
                    }

            val filteredChanges =
                buildList {
                    addAll(normalChanges)
                    notificationChange?.let(::add)
                }
                    .distinctBy { change ->
                        Triple(
                            change.productId,
                            change.retailer,
                            change.checkedAt
                        )
                    }
                    .sortedByDescending {
                        it.checkedAt
                    }

            if (filteredChanges.isEmpty()) {
                null
            } else {
                ShopProductMovementView(
                    item = product.item,
                    amazonHistory =
                        if (
                            retailerFilter ==
                            ShopMovementRetailerFilter
                                .FLIPKART
                        ) {
                            emptyList()
                        } else {
                            product.amazonHistory
                                .withBaseline(cutoff)
                        },
                    flipkartHistory =
                        if (
                            retailerFilter ==
                            ShopMovementRetailerFilter
                                .AMAZON
                        ) {
                            emptyList()
                        } else {
                            product.flipkartHistory
                                .withBaseline(cutoff)
                        },
                    changes = filteredChanges
                )
            }
        }
            .sortedByDescending { product ->
                product.latestChange
                    ?.checkedAt
                    ?: 0L
            }

    return ShopPriceMovementView(
        products = productViews,
        changes =
            productViews
                .flatMap { product ->
                    product.changes
                }
                .sortedByDescending {
                    it.checkedAt
                }
    )
}

private data class ProductRetailerKey(
    val productId: Long,
    val retailer: PriceRetailer
)

private fun List<ShopPricePoint>.cleanAndSort():
        List<ShopPricePoint> =
    asSequence()
        .filter { point ->
            point.price.isFinite() &&
                    point.price > 0.0 &&
                    point.checkedAt > 0L
        }
        .sortedBy { point ->
            point.checkedAt
        }
        .distinctBy { point ->
            point.checkedAt to
                    priceInPaise(point.price)
        }
        .toList()

private fun buildRetailerChanges(
    productId: Long,
    retailer: PriceRetailer,
    points: List<ShopPricePoint>
): List<ShopPriceChange> =
    points.zipWithNext()
        .mapNotNull { pair ->
            val oldPoint = pair.first
            val newPoint = pair.second

            if (
                priceInPaise(oldPoint.price) ==
                priceInPaise(newPoint.price)
            ) {
                null
            } else {
                ShopPriceChange(
                    productId = productId,
                    retailer = retailer,
                    oldPrice = oldPoint.price,
                    newPrice = newPoint.price,
                    checkedAt =
                        newPoint.checkedAt,
                    direction =
                        if (
                            newPoint.price <
                            oldPoint.price
                        ) {
                            DetectedPriceDirection
                                .LOWER
                        } else {
                            DetectedPriceDirection
                                .HIGHER
                        }
                )
            }
        }

private fun ShopMovementRetailerFilter.matches(
    retailer: PriceRetailer
): Boolean =
    when (this) {
        ShopMovementRetailerFilter.ALL ->
            true

        ShopMovementRetailerFilter.AMAZON ->
            retailer == PriceRetailer.AMAZON

        ShopMovementRetailerFilter.FLIPKART ->
            retailer == PriceRetailer.FLIPKART
    }

private fun List<ShopPricePoint>.withBaseline(
    cutoff: Long
): List<ShopPricePoint> {
    val sorted =
        sortedBy { point -> point.checkedAt }

    val previousPoint =
        sorted.lastOrNull { point ->
            point.checkedAt < cutoff
        }

    val visiblePoints =
        sorted.filter { point ->
            point.checkedAt >= cutoff
        }

    return buildList {
        previousPoint?.let(::add)
        addAll(visiblePoints)
    }.distinctBy { point ->
        point.checkedAt to
                priceInPaise(point.price)
    }
}

private fun priceInPaise(
    price: Double
): Long =
    (price * 100.0).roundToLong()