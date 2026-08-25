@file:OptIn(ExperimentalTime::class)

package com.supreme.priceintelligence.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val PRICE_HISTORY_RETENTION_MILLIS =
    30L * 24L * 60L * 60L * 1000L

private const val PRICE_HISTORY_CLEANUP_INTERVAL_MILLIS =
    24L * 60L * 60L * 1000L

private const val INDIA_UTC_OFFSET_MILLIS = 19_800_000L
private const val CALENDAR_DAY_MILLIS = 86_400_000L
private const val DATABASE_SEARCH_WORD_LIMIT = 6

private data class DatabaseSearchWords(
    val word1: String,
    val word2: String,
    val word3: String,
    val word4: String,
    val word5: String,
    val word6: String
)

private fun databaseSearchWords(value: String): DatabaseSearchWords? {
    val words = value
        .trim()
        .split(Regex("\\s+"))
        .filter { word -> word.isNotBlank() }

    if (words.size > DATABASE_SEARCH_WORD_LIMIT) return null

    return DatabaseSearchWords(
        word1 = words.getOrElse(0) { "" },
        word2 = words.getOrElse(1) { "" },
        word3 = words.getOrElse(2) { "" },
        word4 = words.getOrElse(3) { "" },
        word5 = words.getOrElse(4) { "" },
        word6 = words.getOrElse(5) { "" }
    )
}

internal fun rankNameSuggestions(
    query: String,
    candidates: List<String>,
    limit: Int
): List<String> {
    if (limit <= 0) return emptyList()

    val normalizedQuery = query.trim().lowercase()
    val finalQueryWord =
        normalizedQuery.substringAfterLast(" ")

    return candidates
        .asSequence()
        .map { candidate ->
            candidate.trim()
        }
        .filter { candidate ->
            candidate.isNotBlank()
        }
        .distinctBy { candidate ->
            candidate.lowercase()
        }
        .filterNot { candidate ->
            normalizedQuery.isNotEmpty() &&
                    candidate.equals(
                        normalizedQuery,
                        ignoreCase = true
                    )
        }
        .sortedWith(
            compareBy<String> { candidate ->
                val normalizedCandidate =
                    candidate.lowercase()

                when {
                    normalizedQuery.isEmpty() -> 0

                    normalizedCandidate.startsWith(
                        normalizedQuery
                    ) -> 0

                    normalizedCandidate
                        .split(Regex("\\s+"))
                        .any { word ->
                            word.startsWith(finalQueryWord)
                        } -> 1

                    normalizedCandidate.contains(
                        normalizedQuery
                    ) -> 2

                    else -> 3
                }
            }
        )
        .take(limit)
        .toList()
}

private fun priceHistoryDayKey(timestamp: Long): Long =
    (timestamp + INDIA_UTC_OFFSET_MILLIS) / CALENDAR_DAY_MILLIS

private fun priceHistoryPriceKey(price: Double): Long =
    kotlin.math.floor(price * 100.0 + 0.5).toLong()

@Suppress("SpellCheckingInspection")
class InventoryRepository(private val dao: InventoryDao) {
    private val priceHistoryCleanupMutex = Mutex()
    private var lastPriceHistoryCleanupAt = 0L

    suspend fun addProduct(
        name: String,
        shopPrice: Double,
        purchaseCost: Double? = null,
        barcode: String? = null,
        amazonUrl: String? = null,
        flipkartUrl: String? = null,
    ): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return dao.insert(
            InventoryItem(
                productName = name,
                barcode = barcode,
                shopPrice = shopPrice,
                purchaseCost = purchaseCost,
                amazonUrl = amazonUrl,
                flipkartUrl = flipkartUrl,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun updateProduct(item: InventoryItem) {
        dao.update(item.copy(updatedAt = Clock.System.now().toEpochMilliseconds()))
    }

    suspend fun importProduct(item: InventoryItem): Long =
        dao.insert(item.copy(id = 0))

    suspend fun deleteProduct(id: Long) = dao.deleteById(id)

    suspend fun getProductById(id: Long): InventoryItem? =
        if (id > 0L) dao.getById(id) else null

    suspend fun isAmazonUrlDuplicate(url: String, excludeId: Long): Boolean =
        dao.checkAmazonUrlExists(url, excludeId) != null

    suspend fun isFlipkartUrlDuplicate(url: String, excludeId: Long): Boolean =
        dao.checkFlipkartUrlExists(url, excludeId) != null

    suspend fun getAllRanked(): List<InventoryItem> = dao.getAllRanked()

    suspend fun getAllAlphabetical(): List<InventoryItem> = dao.getAll()

    suspend fun getAllRecent(): List<InventoryItem> = dao.getAllRecent()

    /**
     * Digits-only queries try an exact barcode first. Normal name and retailer
     * link searches are filtered by SQLite so a large inventory is not copied
     * into memory merely to find a few matching products.
     */
    suspend fun search(query: String): List<InventoryItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getAllRanked()

        if (trimmed.all { character -> character.isDigit() }) {
            val byBarcode = dao.findByBarcode(trimmed)
            if (byBarcode.isNotEmpty()) return byBarcode
        }

        if (looksLikeUrl(trimmed)) {
            val urlMatches = dao.searchUrlPaged(
                query = trimmed,
                limit = Int.MAX_VALUE,
                offset = 0
            )
            if (urlMatches.isNotEmpty()) return urlMatches
        }

        val databaseWords = databaseSearchWords(trimmed)
        if (databaseWords != null) {
            return dao.searchNamePaged(
                word1 = databaseWords.word1,
                word2 = databaseWords.word2,
                word3 = databaseWords.word3,
                word4 = databaseWords.word4,
                word5 = databaseWords.word5,
                word6 = databaseWords.word6,
                sortOrder = "MOST_VIEWED",
                limit = Int.MAX_VALUE,
                offset = 0
            )
        }

        val words = trimmed
            .split(Regex("\\s+"))
            .filter { word -> word.isNotBlank() }

        return dao.getAllRanked().filter { item ->
            words.all { word ->
                item.productName.contains(word, ignoreCase = true)
            }
        }
    }

    /**
     * Returns useful Dashboard suggestions. An empty search shows popular
     * inventory products. Barcode and retailer-link searches resolve directly
     * to their matching product names.
     */
    suspend fun getNameSuggestions(
        prefix: String,
        limit: Int = 6
    ): List<String> {
        val safeLimit = limit.coerceAtLeast(0)
        if (safeLimit == 0) return emptyList()

        val trimmed = prefix.trim()
        val candidateLimit = (safeLimit * 4).coerceAtLeast(20)

        if (trimmed.isEmpty()) {
            return rankNameSuggestions(
                query = trimmed,
                candidates = dao.getAllRankedPaged(
                    limit = candidateLimit,
                    offset = 0
                ).map { item ->
                    item.productName
                },
                limit = safeLimit
            )
        }

        if (trimmed.all { character -> character.isDigit() }) {
            val barcodeMatches = dao.findByBarcode(trimmed)

            if (barcodeMatches.isNotEmpty()) {
                return rankNameSuggestions(
                    query = trimmed,
                    candidates = barcodeMatches.map { item ->
                        item.productName
                    },
                    limit = safeLimit
                )
            }
        }

        if (looksLikeUrl(trimmed)) {
            val urlMatches = dao.searchUrlPaged(
                query = trimmed,
                limit = candidateLimit,
                offset = 0
            )

            if (urlMatches.isNotEmpty()) {
                return rankNameSuggestions(
                    query = trimmed,
                    candidates = urlMatches.map { item ->
                        item.productName
                    },
                    limit = safeLimit
                )
            }
        }

        val databaseWords = databaseSearchWords(trimmed)

        val candidates =
            if (databaseWords != null) {
                dao.searchNameSuggestions(
                    prefix = trimmed,
                    word1 = databaseWords.word1,
                    word2 = databaseWords.word2,
                    word3 = databaseWords.word3,
                    word4 = databaseWords.word4,
                    word5 = databaseWords.word5,
                    word6 = databaseWords.word6,
                    limit = candidateLimit
                )
            } else {
                val words = trimmed
                    .split(Regex("\\s+"))
                    .filter { word -> word.isNotBlank() }

                dao.getAllRanked()
                    .filter { item ->
                        words.all { word ->
                            item.productName.contains(
                                word,
                                ignoreCase = true
                            )
                        }
                    }
                    .take(candidateLimit)
                    .map { item ->
                        item.productName
                    }
            }

        return rankNameSuggestions(
            query = trimmed,
            candidates = candidates,
            limit = safeLimit
        )
    }

    suspend fun incrementSearchCount(id: Long) =
        dao.incrementSearchCount(id)

    suspend fun incrementSearchCountBulk(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.incrementSearchCountBulk(ids)
    }

    suspend fun updateAmazonCache(itemId: Long, price: Double, timestamp: Long) =
        dao.updateAmazonCache(itemId, price, timestamp)

    suspend fun updateFlipkartCache(itemId: Long, price: Double, timestamp: Long) =
        dao.updateFlipkartCache(itemId, price, timestamp)

    suspend fun updateImageUrl(itemId: Long, imageUrl: String) {
        if (itemId <= 0L || imageUrl.isBlank()) return
        dao.updateImageUrl(itemId, imageUrl)
    }

    suspend fun recordPriceCheck(
        itemId: Long,
        retailer: PriceRetailer,
        price: Double,
        checkedAt: Long
    ): Boolean {
        if (itemId <= 0L || !price.isFinite() || price <= 0.0 || checkedAt <= 0L) return false
        if (dao.getById(itemId) == null) return false

        return try {
            cleanOldPriceHistoryWhenNeeded(checkedAt)

            val samePriceAlreadySavedToday = dao.getPriceHistory(
                itemId = itemId,
                limit =
                    MAX_PRICE_HISTORY_PER_RETAILER *
                        PriceRetailer.entries.size
            ).any { entry ->
                entry.retailer == retailer.name &&
                    priceHistoryDayKey(entry.checkedAt) ==
                    priceHistoryDayKey(checkedAt) &&
                    priceHistoryPriceKey(entry.price) ==
                    priceHistoryPriceKey(price)
            }

            when (retailer) {
                PriceRetailer.AMAZON ->
                    dao.updateAmazonCache(itemId, price, checkedAt)

                PriceRetailer.FLIPKART ->
                    dao.updateFlipkartCache(itemId, price, checkedAt)
            }

            if (!samePriceAlreadySavedToday) {
                dao.insertPriceHistory(
                    PriceHistoryEntry(
                        inventoryItemId = itemId,
                        retailer = retailer.name,
                        price = price,
                        checkedAt = checkedAt
                    )
                )

                dao.trimPriceHistory(
                    itemId = itemId,
                    retailer = retailer.name,
                    keepCount = MAX_PRICE_HISTORY_PER_RETAILER
                )
            }

            true
        } catch (error: androidx.sqlite.SQLiteException) {
            // A product can be deleted while its slower network request is in
            // flight. Treat that expected race as a canceled save, but keep
            // surfacing real database failures for products that still exist.
            if (dao.getById(itemId) == null) false else throw error
        }
    }

    suspend fun importPriceHistory(
        itemId: Long,
        entries: List<PriceHistoryEntry>
    ) {
        if (itemId <= 0L) return

        val validEntries = entries
            .asSequence()
            .filter { entry ->
                entry.retailer in PriceRetailer.entries.map { it.name } &&
                    entry.price.isFinite() &&
                    entry.price > 0.0 &&
                    entry.checkedAt > 0L
            }
            .sortedBy { it.checkedAt }
            .distinctBy { entry ->
                Triple(
                    entry.retailer,
                    priceHistoryDayKey(entry.checkedAt),
                    priceHistoryPriceKey(entry.price)
                )
            }
            .toList()

        val newestImportedTimestamp =
            validEntries.maxOfOrNull { entry ->
                entry.checkedAt
            }

        val importedHistoryCutoff =
            newestImportedTimestamp?.let { newest ->
                (
                    newest - PRICE_HISTORY_RETENTION_MILLIS
                ).coerceAtLeast(0L)
            } ?: 0L

        validEntries
            .asSequence()
            .filter { entry ->
                entry.checkedAt >= importedHistoryCutoff
            }
            .forEach { entry ->
                dao.insertPriceHistory(
                    entry.copy(
                        id = 0,
                        inventoryItemId = itemId
                    )
                )
            }

        PriceRetailer.entries.forEach { retailer ->
            dao.trimPriceHistory(
                itemId = itemId,
                retailer = retailer.name,
                keepCount = MAX_PRICE_HISTORY_PER_RETAILER
            )
        }
    }

    suspend fun getPriceHistory(
        itemId: Long,
        limit: Int = MAX_PRICE_HISTORY_PER_RETAILER * PriceRetailer.entries.size
    ): List<PriceHistoryEntry> = dao.getPriceHistory(
        itemId = itemId,
        limit = limit.coerceIn(
            1,
            MAX_PRICE_HISTORY_PER_RETAILER *
                PriceRetailer.entries.size
        )
    )

    suspend fun getAllPriceHistory():
        List<PriceHistoryEntry> =
        dao.getAllPriceHistory()

    private suspend fun cleanOldPriceHistoryWhenNeeded(checkedAt: Long) {
        priceHistoryCleanupMutex.withLock {
            val clockMovedBackwards =
                lastPriceHistoryCleanupAt > 0L &&
                    checkedAt < lastPriceHistoryCleanupAt
            val cleanupIsDue =
                lastPriceHistoryCleanupAt == 0L ||
                    clockMovedBackwards ||
                    checkedAt - lastPriceHistoryCleanupAt >=
                    PRICE_HISTORY_CLEANUP_INTERVAL_MILLIS

            if (cleanupIsDue) {
                dao.deletePriceHistoryOlderThan(
                    cutoffTimestamp = (
                        checkedAt - PRICE_HISTORY_RETENTION_MILLIS
                    ).coerceAtLeast(0L)
                )
                lastPriceHistoryCleanupAt = checkedAt
            }
        }
    }

    // --- STRICT DATABASE PAGINATION ---

    suspend fun getTotalCount(): Int = dao.getTotalCount()

    suspend fun getPaged(sortOrder: String, limit: Int, offset: Int): List<InventoryItem> {
        return when (sortOrder) {
            "ALPHABETICAL" -> dao.getAllAlphabeticalPaged(limit, offset)
            "RECENT" -> dao.getAllRecentPaged(limit, offset)
            "BEST_SAVING" -> dao.getAllRanked()
                .sortedByBestSavedSaving()
                .drop(offset)
                .take(limit)
            else -> dao.getAllRankedPaged(limit, offset)
        }
    }

    suspend fun searchPaged(
        query: String,
        sortOrder: String,
        limit: Int,
        offset: Int
    ): List<InventoryItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return getPaged(sortOrder, limit, offset)
        }

        if (trimmed.all { character -> character.isDigit() }) {
            val byBarcode = dao.findByBarcode(trimmed)
            if (byBarcode.isNotEmpty()) {
                return byBarcode.drop(offset).take(limit)
            }
        }

        // Retailer-link results always use popularity order, matching the
        // established app behavior regardless of the selected name sort.
        if (looksLikeUrl(trimmed)) {
            return dao.searchUrlPaged(
                query = trimmed,
                limit = limit,
                offset = offset
            )
        }

        val databaseWords = databaseSearchWords(trimmed)
        if (databaseWords != null) {
            val databaseSortOrder =
                if (sortOrder == "BEST_SAVING") "MOST_VIEWED" else sortOrder
            val databaseLimit =
                if (sortOrder == "BEST_SAVING") Int.MAX_VALUE else limit
            val databaseOffset =
                if (sortOrder == "BEST_SAVING") 0 else offset

            val databaseMatches = dao.searchNamePaged(
                word1 = databaseWords.word1,
                word2 = databaseWords.word2,
                word3 = databaseWords.word3,
                word4 = databaseWords.word4,
                word5 = databaseWords.word5,
                word6 = databaseWords.word6,
                sortOrder = databaseSortOrder,
                limit = databaseLimit,
                offset = databaseOffset
            )

            return if (sortOrder == "BEST_SAVING") {
                databaseMatches
                    .sortedByBestSavedSaving()
                    .drop(offset)
                    .take(limit)
            } else {
                databaseMatches
            }
        }

        // Preserve full matching behavior for unusually long searches.
        val words = trimmed
            .split(Regex("\\s+"))
            .filter { word -> word.isNotBlank() }
        val baseList = when (sortOrder) {
            "ALPHABETICAL" -> dao.getAll()
            "RECENT" -> dao.getAllRecent()
            "BEST_SAVING" -> dao.getAllRanked().sortedByBestSavedSaving()
            else -> dao.getAllRanked()
        }

        return baseList
            .filter { item ->
                words.all { word ->
                    item.productName.contains(word, ignoreCase = true)
                }
            }
            .drop(offset)
            .take(limit)
    }

    suspend fun getSearchCount(query: String): Int {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getTotalCount()

        if (trimmed.all { character -> character.isDigit() }) {
            val byBarcode = dao.findByBarcode(trimmed)
            if (byBarcode.isNotEmpty()) return byBarcode.size
        }

        if (looksLikeUrl(trimmed)) {
            return dao.countUrlMatches(trimmed)
        }

        val databaseWords = databaseSearchWords(trimmed)
        if (databaseWords != null) {
            return dao.countNameMatches(
                word1 = databaseWords.word1,
                word2 = databaseWords.word2,
                word3 = databaseWords.word3,
                word4 = databaseWords.word4,
                word5 = databaseWords.word5,
                word6 = databaseWords.word6
            )
        }

        val words = trimmed
            .split(Regex("\\s+"))
            .filter { word -> word.isNotBlank() }

        return dao.getAllRanked().count { item ->
            words.all { word ->
                item.productName.contains(word, ignoreCase = true)
            }
        }
    }

    // Returns every match when a whole-shop summary genuinely needs all rows.
    // Normal Dashboard pages use searchPaged and load only their visible rows.
    suspend fun getAllMatching(query: String): List<InventoryItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return dao.getAll()

        if (trimmed.all { character -> character.isDigit() }) {
            val byBarcode = dao.findByBarcode(trimmed)
            if (byBarcode.isNotEmpty()) return byBarcode
        }

        if (looksLikeUrl(trimmed)) {
            return dao.searchUrlPaged(
                query = trimmed,
                limit = Int.MAX_VALUE,
                offset = 0
            )
        }

        val databaseWords = databaseSearchWords(trimmed)
        if (databaseWords != null) {
            return dao.searchNamePaged(
                word1 = databaseWords.word1,
                word2 = databaseWords.word2,
                word3 = databaseWords.word3,
                word4 = databaseWords.word4,
                word5 = databaseWords.word5,
                word6 = databaseWords.word6,
                sortOrder = "ALPHABETICAL",
                limit = Int.MAX_VALUE,
                offset = 0
            )
        }

        val words = trimmed
            .split(Regex("\\s+"))
            .filter { word -> word.isNotBlank() }

        return dao.getAll().filter { item ->
            words.all { word ->
                item.productName.contains(word, ignoreCase = true)
            }
        }
    }

    private fun looksLikeUrl(value: String): Boolean {
        val normalized = value.lowercase()
        return normalized.startsWith("http") ||
            normalized.contains("amazon") ||
            normalized.contains("flipkart")
    }
}
