package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.data.PriceRetailer
import kotlin.math.absoluteValue

enum class PriceMovement {
    LOWER,
    HIGHER,
    UNCHANGED,
    UNKNOWN
}

data class RetailerPriceHistorySummary(
    val retailer: PriceRetailer,
    val latestPrice: Double,
    val previousPrice: Double?,
    val lowestSavedPrice: Double,
    val highestSavedPrice: Double,
    val latestCheckedAt: Long,
    val observationCount: Int,
    val movement: PriceMovement,
    val movementAmount: Double?,
    val movementPercent: Double?,
    /** Newest first, matching the database order. */
    val recentPrices: List<Double>
)

fun summarizePriceHistory(
    entries: List<PriceHistoryEntry>,
    retailer: PriceRetailer
): RetailerPriceHistorySummary? {
    val validEntries = entries
        .asSequence()
        .filter { entry ->
            entry.retailer == retailer.name &&
                entry.price.isFinite() &&
                entry.price > 0.0 &&
                entry.checkedAt > 0L
        }
        .sortedWith(
            compareByDescending<PriceHistoryEntry> { it.checkedAt }
                .thenByDescending { it.id }
        )
        .toList()

    val latest = validEntries.firstOrNull() ?: return null
    val previous = validEntries.getOrNull(1)
    val change = previous?.let { latest.price - it.price }
    val movement = when {
        change == null -> PriceMovement.UNKNOWN
        change.absoluteValue <= 0.01 -> PriceMovement.UNCHANGED
        change < 0.0 -> PriceMovement.LOWER
        else -> PriceMovement.HIGHER
    }
    val percent = previous
        ?.price
        ?.takeIf { it > 0.0 }
        ?.let { previousPrice -> change?.div(previousPrice)?.times(100.0) }

    return RetailerPriceHistorySummary(
        retailer = retailer,
        latestPrice = latest.price,
        previousPrice = previous?.price,
        lowestSavedPrice = validEntries.minOf { it.price },
        highestSavedPrice = validEntries.maxOf { it.price },
        latestCheckedAt = latest.checkedAt,
        observationCount = validEntries.size,
        movement = movement,
        movementAmount = change?.absoluteValue,
        movementPercent = percent?.absoluteValue,
        recentPrices = validEntries.take(4).map { it.price }
    )
}
