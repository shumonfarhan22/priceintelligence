package com.supreme.priceintelligence.dashboard

import kotlin.math.absoluteValue
import kotlin.math.roundToLong

enum class ShopPricePosition {
    NO_ONLINE_PRICE,
    LOWER,
    MATCHED,
    HIGHER
}

data class PriceComparison(
    val onlineLowestPrice: Double?,
    /** Positive means the online price is cheaper than the shop price. */
    val shopDifference: Double?,
    val shopDifferencePercent: Double?,
    val shopPosition: ShopPricePosition
)

fun compareWithOnlinePrices(
    shopPrice: Double,
    amazonPrice: Double?,
    flipkartPrice: Double?
): PriceComparison {
    val onlineLowest = listOfNotNull(
        amazonPrice.validPriceOrNull(),
        flipkartPrice.validPriceOrNull()
    ).minOrNull()

    if (onlineLowest == null) {
        return PriceComparison(
            onlineLowestPrice = null,
            shopDifference = null,
            shopDifferencePercent = null,
            shopPosition = ShopPricePosition.NO_ONLINE_PRICE
        )
    }

    val difference = shopPrice - onlineLowest
    val position = when {
        difference.absoluteValue <= 0.01 -> ShopPricePosition.MATCHED
        difference > 0.0 -> ShopPricePosition.HIGHER
        else -> ShopPricePosition.LOWER
    }
    val percent = if (shopPrice.isFinite() && shopPrice > 0.0) {
        difference / shopPrice * 100.0
    } else {
        null
    }

    return PriceComparison(
        onlineLowestPrice = onlineLowest,
        shopDifference = difference,
        shopDifferencePercent = percent,
        shopPosition = position
    )
}

fun formatIndianPrice(value: Double): String {
    val paise = (value * 100).roundToLong()
    val whole = paise / 100
    val decimal = (paise % 100).absoluteValue
    val sign = if (whole < 0 || paise < 0) "-" else ""
    val groupedWhole = groupIndianDigits(whole.absoluteValue.toString())

    return if (decimal == 0L) {
        "${sign}₹$groupedWhole"
    } else {
        "${sign}₹$groupedWhole.${decimal.toString().padStart(2, '0')}"
    }
}

private fun Double?.validPriceOrNull(): Double? =
    this?.takeIf { value -> value.isFinite() && value > 0.0 }

private fun groupIndianDigits(digits: String): String {
    if (digits.length <= 3) return digits

    val lastThree = digits.takeLast(3)
    val leading = digits.dropLast(3)
    val groups = mutableListOf<String>()
    var end = leading.length
    while (end > 0) {
        val start = (end - 2).coerceAtLeast(0)
        groups.add(0, leading.substring(start, end))
        end = start
    }
    return groups.joinToString(",") + "," + lastThree
}
