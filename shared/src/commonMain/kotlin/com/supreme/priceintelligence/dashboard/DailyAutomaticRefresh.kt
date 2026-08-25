package com.supreme.priceintelligence.dashboard

internal const val AUTOMATIC_REFRESH_DAY_MILLIS =
    24L * 60L * 60L * 1000L

private const val INDIA_TIME_OFFSET_MILLIS =
    5L * 60L * 60L * 1000L +
            30L * 60L * 1000L

internal fun automaticRefreshDayKey(
    nowMillis: Long
): Long =
    (nowMillis + INDIA_TIME_OFFSET_MILLIS) /
            AUTOMATIC_REFRESH_DAY_MILLIS

internal fun readAutomaticRefreshAttempts(
    storedValue: String,
    nowMillis: Long
): Set<Long> {
    val parts = storedValue.split(
        "|",
        limit = 2
    )

    if (parts.size != 2) {
        return emptySet()
    }

    val storedDay = parts[0].toLongOrNull()
        ?: return emptySet()

    if (storedDay != automaticRefreshDayKey(nowMillis)) {
        return emptySet()
    }

    return parts[1]
        .split(",")
        .mapNotNull { value ->
            value.trim()
                .toLongOrNull()
                ?.takeIf { productId ->
                    productId > 0L
                }
        }
        .toSet()
}

internal fun writeAutomaticRefreshAttempts(
    productIds: Set<Long>,
    nowMillis: Long
): String {
    val safeIds = productIds
        .asSequence()
        .filter { productId -> productId > 0L }
        .distinct()
        .sorted()
        .joinToString(",")

    return "${automaticRefreshDayKey(nowMillis)}|$safeIds"
}