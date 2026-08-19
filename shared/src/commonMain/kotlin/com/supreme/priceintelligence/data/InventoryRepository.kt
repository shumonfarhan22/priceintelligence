@file:OptIn(ExperimentalTime::class)

package com.supreme.priceintelligence.data

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Suppress("SpellCheckingInspection")
class InventoryRepository(private val dao: InventoryDao) {

    suspend fun addProduct(
        name: String,
        shopPrice: Double,
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

    suspend fun deleteProduct(id: Long) = dao.deleteById(id)

    suspend fun isAmazonUrlDuplicate(url: String, excludeId: Long): Boolean =
        dao.checkAmazonUrlExists(url, excludeId) != null

    suspend fun isFlipkartUrlDuplicate(url: String, excludeId: Long): Boolean =
        dao.checkFlipkartUrlExists(url, excludeId) != null

    suspend fun getAllRanked(): List<InventoryItem> = dao.getAllRanked()

    suspend fun getAllAlphabetical(): List<InventoryItem> = dao.getAll()

    suspend fun getAllRecent(): List<InventoryItem> = dao.getAllRecent()

    /**
     * Mirrors the original search_products(): digits-only query tries an exact
     * barcode match first; otherwise (or if no barcode hit) does a multi-word
     * AND search over product_name — now filtered in Kotlin instead of raw SQL,
     * since @RawQuery isn't supported on iOS yet in Room's multiplatform build.
     */
    suspend fun search(query: String): List<InventoryItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getAllRanked()

        if (trimmed.all { it.isDigit() }) {
            val byBarcode = dao.findByBarcode(trimmed)
            if (byBarcode.isNotEmpty()) return byBarcode
        }

        val allRanked = dao.getAllRanked()

        if (looksLikeUrl(trimmed)) {
            val urlMatches = allRanked.filter { matchesUrl(it, trimmed) }
            if (urlMatches.isNotEmpty()) return urlMatches
        }

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        return allRanked.filter { item -> words.all { w -> item.productName.contains(w, ignoreCase = true) } }
    }

    /** Mirrors the original get_name_suggestions() autocomplete. */
    suspend fun getNameSuggestions(prefix: String, limit: Int = 6): List<String> {
        val trimmed = prefix.trim()
        if (trimmed.isEmpty()) return emptyList()

        if (looksLikeUrl(trimmed)) {
            val urlMatches = dao.getAllRanked().filter { matchesUrl(it, trimmed) }.take(limit)
            if (urlMatches.isNotEmpty()) return urlMatches.map { it.productName }.distinct()
        }

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val exactPrefix = trimmed.lowercase()

        // getAllRanked() is already ordered by search_count DESC, name — Kotlin's
        // sortedWith is stable, so this only reshuffles exact-prefix matches to
        // the front without disturbing that existing order, same as the old SQL did.
        val rows = dao.getAllRanked()
            .filter { item -> words.all { w -> item.productName.contains(w, ignoreCase = true) } }
            .sortedWith(compareBy { if (it.productName.lowercase().startsWith(exactPrefix)) 0 else 1 })
            .take(limit)

        val seen = LinkedHashSet<String>()
        val names = mutableListOf<String>()
        for (row in rows) {
            val key = row.productName.lowercase()
            if (seen.add(key)) names.add(row.productName)
        }
        return names
    }

    suspend fun incrementSearchCount(id: Long) =
        dao.incrementSearchCount(id, Clock.System.now().toEpochMilliseconds())

    suspend fun incrementSearchCountBulk(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.incrementSearchCountBulk(ids, Clock.System.now().toEpochMilliseconds())
    }

    suspend fun updateAmazonCache(itemId: Long, price: Double, timestamp: Long) =
        dao.updateAmazonCache(itemId, price, timestamp)

    suspend fun updateFlipkartCache(itemId: Long, price: Double, timestamp: Long) =
        dao.updateFlipkartCache(itemId, price, timestamp)

    // --- STRICT DATABASE PAGINATION ---

    suspend fun getTotalCount(): Int = dao.getTotalCount()

    suspend fun getPaged(sortOrder: String, limit: Int, offset: Int): List<InventoryItem> {
        return when (sortOrder) {
            "ALPHABETICAL" -> dao.getAllAlphabeticalPaged(limit, offset)
            "RECENT" -> dao.getAllRecentPaged(limit, offset)
            else -> dao.getAllRankedPaged(limit, offset)
        }
    }

    suspend fun searchPaged(query: String, sortOrder: String, limit: Int, offset: Int): List<InventoryItem> {
        val trimmed = query.trim()

        if (trimmed.all { it.isDigit() }) {
            val byBarcode = dao.findByBarcode(trimmed)
            if (byBarcode.isNotEmpty()) return byBarcode
        }

        // URL matches always rank by search_count, same as the original — regardless
        // of whatever sortOrder the user has picked for the rest of the list.
        if (looksLikeUrl(trimmed)) {
            val urlMatches = dao.getAllRanked().filter { matchesUrl(it, trimmed) }
            return urlMatches.drop(offset).take(limit)
        }

        val baseList = when (sortOrder) {
            "ALPHABETICAL" -> dao.getAll()
            "RECENT" -> dao.getAllRecent()
            else -> dao.getAllRanked()
        }
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered = baseList.filter { item -> words.all { w -> item.productName.contains(w, ignoreCase = true) } }
        return filtered.drop(offset).take(limit)
    }

    suspend fun getSearchCount(query: String): Int {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getTotalCount()

        if (trimmed.all { it.isDigit() }) {
            return if (dao.findByBarcode(trimmed).isNotEmpty()) 1 else 0
        }

        if (looksLikeUrl(trimmed)) {
            return dao.getAllRanked().count { matchesUrl(it, trimmed) }
        }

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        return dao.getAllRanked().count { item -> words.all { w -> item.productName.contains(w, ignoreCase = true) } }
    }

    private fun looksLikeUrl(value: String): Boolean =
        value.startsWith("http") || value.contains("amazon") || value.contains("flipkart")

    private fun matchesUrl(item: InventoryItem, pasted: String): Boolean {
        val amazon = item.amazonUrl
        val flipkart = item.flipkartUrl
        val amazonMatch = !amazon.isNullOrEmpty() && (amazon.contains(pasted) || pasted.contains(amazon))
        val flipkartMatch = !flipkart.isNullOrEmpty() && (flipkart.contains(pasted) || pasted.contains(flipkart))
        return amazonMatch || flipkartMatch
    }
}