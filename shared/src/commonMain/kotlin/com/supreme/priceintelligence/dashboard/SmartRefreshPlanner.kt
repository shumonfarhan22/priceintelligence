package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.InventoryItem
import kotlin.math.absoluteValue

// Smart Optimization changes the order of daily checks.
// It must never reduce daily price-history coverage.
private const val SMART_REFRESH_PROFILE_VERSION = "1"
private const val SMART_REFRESH_MAX_PROFILE_RECORDS = 120
private const val SMART_REFRESH_MINIMUM_DELAY_MILLIS = 16_000L
private const val SMART_REFRESH_DELAY_VARIATION_MILLIS = 9_000L

internal data class SmartRefreshRecord(
    val productId: Long,
    val consecutiveFailures: Int = 0,
    val volatilityPoints: Int = 0,
    val lastOutcomeAt: Long = 0L
)

internal fun readSmartRefreshProfile(
    storedValue: String
): Map<Long, SmartRefreshRecord> {
    val separatorIndex = storedValue.indexOf('|')

    if (
        separatorIndex <= 0 ||
        storedValue.substring(0, separatorIndex) !=
        SMART_REFRESH_PROFILE_VERSION
    ) {
        return emptyMap()
    }

    return storedValue
        .substring(separatorIndex + 1)
        .split(';')
        .mapNotNull { encodedRecord ->
            val parts = encodedRecord.split(',')

            if (parts.size != 4) {
                return@mapNotNull null
            }

            val productId =
                parts[0].toLongOrNull()
                    ?.takeIf { id -> id > 0L }
                    ?: return@mapNotNull null

            val failures =
                parts[1].toIntOrNull()
                    ?.coerceIn(0, 5)
                    ?: return@mapNotNull null

            val volatility =
                parts[2].toIntOrNull()
                    ?.coerceIn(0, 10)
                    ?: return@mapNotNull null

            val lastOutcomeAt =
                parts[3].toLongOrNull()
                    ?.coerceAtLeast(0L)
                    ?: return@mapNotNull null

            SmartRefreshRecord(
                productId = productId,
                consecutiveFailures = failures,
                volatilityPoints = volatility,
                lastOutcomeAt = lastOutcomeAt
            )
        }
        .associateBy { record -> record.productId }
}

internal fun writeSmartRefreshProfile(
    records: Map<Long, SmartRefreshRecord>
): String {
    val encodedRecords = records.values
        .asSequence()
        .filter { record -> record.productId > 0L }
        .sortedByDescending { record ->
            record.lastOutcomeAt
        }
        .take(SMART_REFRESH_MAX_PROFILE_RECORDS)
        .sortedBy { record -> record.productId }
        .joinToString(";") { record ->
            listOf(
                record.productId,
                record.consecutiveFailures.coerceIn(0, 5),
                record.volatilityPoints.coerceIn(0, 10),
                record.lastOutcomeAt.coerceAtLeast(0L)
            ).joinToString(",")
        }

    return "$SMART_REFRESH_PROFILE_VERSION|$encodedRecords"
}

internal fun updateSmartRefreshOutcome(
    records: Map<Long, SmartRefreshRecord>,
    productId: Long,
    succeeded: Boolean,
    priceMoved: Boolean,
    nowMillis: Long
): Map<Long, SmartRefreshRecord> {
    if (productId <= 0L || nowMillis <= 0L) {
        return records
    }

    val current =
        records[productId]
            ?: SmartRefreshRecord(productId = productId)

    val updated = if (succeeded) {
        current.copy(
            consecutiveFailures = 0,
            volatilityPoints =
                if (priceMoved) {
                    (current.volatilityPoints + 2)
                        .coerceAtMost(10)
                } else {
                    (current.volatilityPoints - 1)
                        .coerceAtLeast(0)
                },
            lastOutcomeAt = nowMillis
        )
    } else {
        current.copy(
            consecutiveFailures =
                (current.consecutiveFailures + 1)
                    .coerceAtMost(5),
            lastOutcomeAt = nowMillis
        )
    }

    return records + (productId to updated)
}

internal fun hasMeaningfulPriceMovement(
    oldPrice: Double?,
    newPrice: Double?
): Boolean {
    if (
        oldPrice == null ||
        newPrice == null ||
        !oldPrice.isFinite() ||
        !newPrice.isFinite() ||
        oldPrice <= 0.0 ||
        newPrice <= 0.0
    ) {
        return false
    }

    val percentageDifference =
        (newPrice - oldPrice).absoluteValue /
                oldPrice.coerceAtLeast(1.0)

    return percentageDifference >= 0.01
}

internal fun buildSmartRefreshPlan(
    products: List<InventoryItem>,
    profile: Map<Long, SmartRefreshRecord>,
    attemptedProductIds: Set<Long>,
    nowMillis: Long
): List<InventoryItem> {
    if (nowMillis <= 0L) {
        return emptyList()
    }

    return products
        .asSequence()
        .filter { item ->
            item.hasLinkedRetailer()
        }
        .filter { item ->
            item.id !in attemptedProductIds
        }
        .sortedWith(
            compareByDescending<InventoryItem> { item ->
                smartRefreshPriorityScore(
                    item = item,
                    record = profile[item.id],
                    nowMillis = nowMillis
                )
            }.thenBy { item ->
                item.productName.lowercase()
            }.thenBy { item ->
                item.id
            }
        )
        .toList()
}

internal fun smartRefreshSpacingMillis(
    productId: Long,
    nowMillis: Long
): Long {
    val dayKey = automaticRefreshDayKey(nowMillis)

    val rawJitter =
        (
                productId * 37L +
                        dayKey * 17L
                ) % (
                SMART_REFRESH_DELAY_VARIATION_MILLIS + 1L
                )

    val safeJitter =
        if (rawJitter < 0L) {
            -rawJitter
        } else {
            rawJitter
        }

    return SMART_REFRESH_MINIMUM_DELAY_MILLIS +
            safeJitter
}

private fun smartRefreshPriorityScore(
    item: InventoryItem,
    record: SmartRefreshRecord?,
    nowMillis: Long
): Double {
    val oldestSuccessfulCheck =
        item.oldestLinkedRetailerCheck()

    val missingPriceScore =
        if (oldestSuccessfulCheck <= 0L) {
            120.0
        } else {
            0.0
        }

    val ageScore =
        if (oldestSuccessfulCheck <= 0L) {
            0.0
        } else {
            daysBetween(
                earlierMillis = oldestSuccessfulCheck,
                nowMillis = nowMillis
            )
                .coerceAtMost(30L)
                .toDouble() * 6.0
        }

    val usageScore =
        item.searchCount
            .coerceIn(0, 50)
            .toDouble() * 2.0

    val volatilityScore =
        (record?.volatilityPoints ?: 0)
            .coerceIn(0, 10)
            .toDouble() * 10.0

    val reviewScore =
        item.savedOnlineReviewPriority()
            .coerceAtMost(30.0)

    val retailerScore =
        item.linkedRetailerCount()
            .toDouble() * 3.0

    val failurePenalty =
        (record?.consecutiveFailures ?: 0)
            .coerceIn(0, 5)
            .toDouble() * 8.0

    val dayKey = automaticRefreshDayKey(nowMillis)

    val tieBreaker =
        (
                (
                        item.id * 31L +
                                dayKey
                        ) % 10L
                )
            .let { value ->
                if (value < 0L) -value else value
            }
            .toDouble() / 10.0

    return missingPriceScore +
            ageScore +
            usageScore +
            volatilityScore +
            reviewScore +
            retailerScore -
            failurePenalty +
            tieBreaker
}

private fun InventoryItem.hasLinkedRetailer(): Boolean =
    !amazonUrl.isNullOrBlank() ||
            !flipkartUrl.isNullOrBlank()

private fun InventoryItem.linkedRetailerCount(): Int {
    var count = 0

    if (!amazonUrl.isNullOrBlank()) {
        count += 1
    }

    if (!flipkartUrl.isNullOrBlank()) {
        count += 1
    }

    return count
}

private fun InventoryItem.oldestLinkedRetailerCheck(): Long {
    var oldestCheck = Long.MAX_VALUE
    var hasRetailer = false

    if (!amazonUrl.isNullOrBlank()) {
        hasRetailer = true

        val checkedAt =
            amazonLastChecked
                ?.takeIf { value -> value > 0L }
                ?: return 0L

        oldestCheck = minOf(oldestCheck, checkedAt)
    }

    if (!flipkartUrl.isNullOrBlank()) {
        hasRetailer = true

        val checkedAt =
            flipkartLastChecked
                ?.takeIf { value -> value > 0L }
                ?: return 0L

        oldestCheck = minOf(oldestCheck, checkedAt)
    }

    return if (hasRetailer) {
        oldestCheck
    } else {
        0L
    }
}

private fun InventoryItem.savedOnlineReviewPriority(): Double {
    if (!shopPrice.isFinite() || shopPrice <= 0.0) {
        return 0.0
    }

    val bestOnlinePrice = listOfNotNull(
        amazonLastPrice?.takeIf { price ->
            price.isFinite() && price > 0.0
        },
        flipkartLastPrice?.takeIf { price ->
            price.isFinite() && price > 0.0
        }
    ).minOrNull() ?: return 0.0

    val shopDifference = shopPrice - bestOnlinePrice

    if (shopDifference <= 0.01) {
        return 0.0
    }

    return (
            shopDifference /
                    shopPrice.coerceAtLeast(1.0) *
                    100.0
            ).coerceAtMost(30.0)
}

private fun daysBetween(
    earlierMillis: Long,
    nowMillis: Long
): Long {
    if (earlierMillis <= 0L || nowMillis <= 0L) {
        return 0L
    }

    return (
            automaticRefreshDayKey(nowMillis) -
                    automaticRefreshDayKey(earlierMillis)
            ).coerceAtLeast(0L)
}