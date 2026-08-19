@file:OptIn(ExperimentalTime::class)

package com.supreme.priceintelligence.data

import androidx.room3.RoomRawQuery
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
     * Mirrors db.py's search_products(): digits-only query tries an exact
     * barcode match first; otherwise (or if no barcode hit) it does a
     * multi-word AND search over product_name.
     */
    suspend fun search(query: String): List<InventoryItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getAllRanked()

        if (trimmed.all { it.isDigit() }) {
            val byBarcode = dao.findByBarcode(trimmed)
            if (byBarcode.isNotEmpty()) return byBarcode
        }

        // If the user pasted a link, try to find the exact product directly
        if (trimmed.startsWith("http") || trimmed.contains("amazon") || trimmed.contains("flipkart")) {
            val urlSql = """
                SELECT * FROM inventory 
                WHERE (amazon_url LIKE ? ESCAPE '\' OR (? LIKE '%' || amazon_url || '%' AND amazon_url IS NOT NULL AND amazon_url != ''))
                   OR (flipkart_url LIKE ? ESCAPE '\' OR (? LIKE '%' || flipkart_url || '%' AND flipkart_url IS NOT NULL AND flipkart_url != ''))
                ORDER BY search_count DESC
            """.trimIndent()

            val urlQuery = "%${escapeLike(trimmed)}%"
            val urlMatches = dao.searchRaw(buildRawQuery(urlSql, listOf(urlQuery, trimmed, urlQuery, trimmed)))
            if (urlMatches.isNotEmpty()) return urlMatches
        }

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val whereClause = words.joinToString(" AND ") { "product_name LIKE ? ESCAPE '\\'" }
        val args: List<Any> = words.map { "%${escapeLike(it)}%" }
        val sql = "SELECT * FROM inventory WHERE $whereClause " +
                "ORDER BY search_count DESC, product_name COLLATE NOCASE"
        return dao.searchRaw(buildRawQuery(sql, args))
    }

    /** Mirrors db.py's get_name_suggestions() autocomplete. */
    suspend fun getNameSuggestions(prefix: String, limit: Int = 6): List<String> {
        val trimmed = prefix.trim()
        if (trimmed.isEmpty()) return emptyList()

        if (trimmed.startsWith("http") || trimmed.contains("amazon") || trimmed.contains("flipkart")) {
            val urlSql = """
                SELECT * FROM inventory 
                WHERE (amazon_url LIKE ? ESCAPE '\' OR (? LIKE '%' || amazon_url || '%' AND amazon_url IS NOT NULL AND amazon_url != ''))
                   OR (flipkart_url LIKE ? ESCAPE '\' OR (? LIKE '%' || flipkart_url || '%' AND flipkart_url IS NOT NULL AND flipkart_url != ''))
                LIMIT ?
            """.trimIndent()

            val urlQuery = "%${escapeLike(trimmed)}%"
            val urlMatches = dao.suggestRaw(buildRawQuery(urlSql, listOf(urlQuery, trimmed, urlQuery, trimmed, limit)))
            if (urlMatches.isNotEmpty()) return urlMatches.map { it.productName }.distinct()
        }

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val whereClause = words.joinToString(" AND ") { "product_name LIKE ? ESCAPE '\\'" }
        val likeArgs: List<Any> = words.map { "%${escapeLike(it)}%" }
        val exactLike = "${escapeLike(trimmed)}%"

        val sql = """
            SELECT * FROM inventory
            WHERE $whereClause
            ORDER BY (CASE WHEN product_name LIKE ? ESCAPE '\' THEN 0 ELSE 1 END) ASC,
                     search_count DESC, product_name COLLATE NOCASE
            LIMIT ?
        """.trimIndent()

        val args: List<Any> = likeArgs + listOf(exactLike, limit)
        val rows = dao.suggestRaw(buildRawQuery(sql, args))

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

    // --- PRICE MEMORY BANK ---
    // DashboardViewModel used to reach around the repository straight to the DAO
    // for these two — now that it only holds a repository, these thin wrappers
    // keep that same call working.
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

        if (trimmed.startsWith("http") || trimmed.contains("amazon") || trimmed.contains("flipkart")) {
            val urlSql = """
                SELECT * FROM inventory 
                WHERE (amazon_url LIKE ? ESCAPE '\' OR (? LIKE '%' || amazon_url || '%' AND amazon_url IS NOT NULL AND amazon_url != ''))
                   OR (flipkart_url LIKE ? ESCAPE '\' OR (? LIKE '%' || flipkart_url || '%' AND flipkart_url IS NOT NULL AND flipkart_url != ''))
                ORDER BY search_count DESC LIMIT ? OFFSET ?
            """.trimIndent()
            val urlQuery = "%${escapeLike(trimmed)}%"
            return dao.searchRaw(buildRawQuery(urlSql, listOf(urlQuery, trimmed, urlQuery, trimmed, limit, offset)))
        }

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val whereClause = words.joinToString(" AND ") { "product_name LIKE ? ESCAPE '\\'" }

        val orderClause = when (sortOrder) {
            "ALPHABETICAL" -> "product_name COLLATE NOCASE ASC"
            "RECENT" -> "updated_at DESC, id DESC"
            else -> "search_count DESC, product_name COLLATE NOCASE"
        }

        val sql = "SELECT * FROM inventory WHERE $whereClause ORDER BY $orderClause LIMIT ? OFFSET ?"
        val args: MutableList<Any> = words.map { "%${escapeLike(it)}%" as Any }.toMutableList()
        args.add(limit)
        args.add(offset)

        return dao.searchRaw(buildRawQuery(sql, args))
    }

    suspend fun getSearchCount(query: String): Int {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getTotalCount()

        if (trimmed.all { it.isDigit() }) {
            return if (dao.findByBarcode(trimmed).isNotEmpty()) 1 else 0
        }

        if (trimmed.startsWith("http") || trimmed.contains("amazon") || trimmed.contains("flipkart")) {
            val urlSql = """
                SELECT COUNT(*) FROM inventory 
                WHERE (amazon_url LIKE ? ESCAPE '\' OR (? LIKE '%' || amazon_url || '%' AND amazon_url IS NOT NULL AND amazon_url != ''))
                   OR (flipkart_url LIKE ? ESCAPE '\' OR (? LIKE '%' || flipkart_url || '%' AND flipkart_url IS NOT NULL AND flipkart_url != ''))
            """.trimIndent()
            val urlQuery = "%${escapeLike(trimmed)}%"
            return dao.countRaw(buildRawQuery(urlSql, listOf(urlQuery, trimmed, urlQuery, trimmed)))
        }

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val whereClause = words.joinToString(" AND ") { "product_name LIKE ? ESCAPE '\\'" }
        val sql = "SELECT COUNT(*) FROM inventory WHERE $whereClause"
        val args: List<Any> = words.map { "%${escapeLike(it)}%" }

        return dao.countRaw(buildRawQuery(sql, args))
    }

    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    // Room KMP's raw queries bind arguments via a callback instead of a plain array —
    // this one helper keeps every call site above looking almost exactly like it did before.
    private fun buildRawQuery(sql: String, args: List<Any>): RoomRawQuery {
        return RoomRawQuery(sql = sql, onBindStatement = { stmt ->
            args.forEachIndexed { index, value ->
                when (value) {
                    is String -> stmt.bindText(index + 1, value)
                    is Int -> stmt.bindLong(index + 1, value.toLong())
                    is Long -> stmt.bindLong(index + 1, value)
                    else -> error("Unsupported bind arg type: ${value::class}")
                }
            }
        })
    }
}