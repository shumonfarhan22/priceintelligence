package com.supreme.priceintelligence.data

/**
 * Returns the latest usable saved online price. Live prices are kept in the
 * Dashboard state and take priority there; this helper is deliberately for
 * database-backed sorting only.
 */
internal fun InventoryItem.lowestSavedOnlinePrice(): Double? = listOfNotNull(
    amazonLastPrice.validSavedPrice(),
    flipkartLastPrice.validSavedPrice()
).minOrNull()

/** Positive values mean an online store was cheaper than the shop. */
internal fun InventoryItem.savedOnlineSaving(): Double? {
    if (!shopPrice.isFinite() || shopPrice <= 0.0) return null
    return lowestSavedOnlinePrice()?.let { onlinePrice -> shopPrice - onlinePrice }
}

internal fun List<InventoryItem>.sortedByBestSavedSaving(): List<InventoryItem> =
    sortedWith(
        compareByDescending<InventoryItem> { item ->
            item.savedOnlineSaving()?.takeIf { saving -> saving > 0.01 }
        }.thenBy { item -> item.productName.lowercase() }
    )

private fun Double?.validSavedPrice(): Double? =
    this?.takeIf { value -> value.isFinite() && value > 0.0 }
