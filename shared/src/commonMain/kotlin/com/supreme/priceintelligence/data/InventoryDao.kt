package com.supreme.priceintelligence.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface InventoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: InventoryItem): Long

    @Update
    suspend fun update(item: InventoryItem)

    @Query("DELETE FROM inventory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM inventory WHERE id = :id")
    suspend fun getById(id: Long): InventoryItem?

    @Query("SELECT COUNT(*) FROM inventory")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM inventory ORDER BY product_name COLLATE NOCASE")
    suspend fun getAll(): List<InventoryItem>

    @Query("SELECT * FROM inventory ORDER BY search_count DESC, product_name COLLATE NOCASE")
    suspend fun getAllRanked(): List<InventoryItem>

    @Query("SELECT * FROM inventory ORDER BY updated_at DESC, id DESC")
    suspend fun getAllRecent(): List<InventoryItem>

    @Query("SELECT * FROM inventory ORDER BY product_name COLLATE NOCASE LIMIT :limit OFFSET :offset")
    suspend fun getAllAlphabeticalPaged(limit: Int, offset: Int): List<InventoryItem>

    @Query("SELECT * FROM inventory ORDER BY search_count DESC, product_name COLLATE NOCASE LIMIT :limit OFFSET :offset")
    suspend fun getAllRankedPaged(limit: Int, offset: Int): List<InventoryItem>

    @Query("SELECT * FROM inventory ORDER BY updated_at DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllRecentPaged(limit: Int, offset: Int): List<InventoryItem>

    @Query(
        """
        SELECT * FROM inventory
        WHERE (:word1 = '' OR instr(lower(product_name), lower(:word1)) > 0)
          AND (:word2 = '' OR instr(lower(product_name), lower(:word2)) > 0)
          AND (:word3 = '' OR instr(lower(product_name), lower(:word3)) > 0)
          AND (:word4 = '' OR instr(lower(product_name), lower(:word4)) > 0)
          AND (:word5 = '' OR instr(lower(product_name), lower(:word5)) > 0)
          AND (:word6 = '' OR instr(lower(product_name), lower(:word6)) > 0)
        ORDER BY
          CASE WHEN :sortOrder = 'ALPHABETICAL' THEN lower(product_name) END ASC,
          CASE WHEN :sortOrder = 'RECENT' THEN updated_at END DESC,
          CASE WHEN :sortOrder = 'RECENT' THEN id END DESC,
          CASE WHEN :sortOrder NOT IN ('ALPHABETICAL', 'RECENT') THEN search_count END DESC,
          lower(product_name) ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchNamePaged(
        word1: String,
        word2: String,
        word3: String,
        word4: String,
        word5: String,
        word6: String,
        sortOrder: String,
        limit: Int,
        offset: Int
    ): List<InventoryItem>

    @Query(
        """
        SELECT COUNT(*) FROM inventory
        WHERE (:word1 = '' OR instr(lower(product_name), lower(:word1)) > 0)
          AND (:word2 = '' OR instr(lower(product_name), lower(:word2)) > 0)
          AND (:word3 = '' OR instr(lower(product_name), lower(:word3)) > 0)
          AND (:word4 = '' OR instr(lower(product_name), lower(:word4)) > 0)
          AND (:word5 = '' OR instr(lower(product_name), lower(:word5)) > 0)
          AND (:word6 = '' OR instr(lower(product_name), lower(:word6)) > 0)
        """
    )
    suspend fun countNameMatches(
        word1: String,
        word2: String,
        word3: String,
        word4: String,
        word5: String,
        word6: String
    ): Int

    @Query(
        """
        SELECT product_name FROM inventory
        WHERE (:word1 = '' OR instr(lower(product_name), lower(:word1)) > 0)
          AND (:word2 = '' OR instr(lower(product_name), lower(:word2)) > 0)
          AND (:word3 = '' OR instr(lower(product_name), lower(:word3)) > 0)
          AND (:word4 = '' OR instr(lower(product_name), lower(:word4)) > 0)
          AND (:word5 = '' OR instr(lower(product_name), lower(:word5)) > 0)
          AND (:word6 = '' OR instr(lower(product_name), lower(:word6)) > 0)
        GROUP BY lower(product_name)
        ORDER BY
          CASE WHEN instr(lower(product_name), lower(:prefix)) = 1 THEN 0 ELSE 1 END ASC,
          MAX(search_count) DESC,
          lower(product_name) ASC
        LIMIT :limit
        """
    )
    suspend fun searchNameSuggestions(
        prefix: String,
        word1: String,
        word2: String,
        word3: String,
        word4: String,
        word5: String,
        word6: String,
        limit: Int
    ): List<String>

    @Query(
        """
        SELECT * FROM inventory
        WHERE (
            amazon_url IS NOT NULL
            AND amazon_url != ''
            AND (
                instr(lower(amazon_url), lower(:query)) > 0
                OR instr(lower(:query), lower(amazon_url)) > 0
            )
        ) OR (
            flipkart_url IS NOT NULL
            AND flipkart_url != ''
            AND (
                instr(lower(flipkart_url), lower(:query)) > 0
                OR instr(lower(:query), lower(flipkart_url)) > 0
            )
        )
        ORDER BY search_count DESC, product_name COLLATE NOCASE
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchUrlPaged(
        query: String,
        limit: Int,
        offset: Int
    ): List<InventoryItem>

    @Query(
        """
        SELECT COUNT(*) FROM inventory
        WHERE (
            amazon_url IS NOT NULL
            AND amazon_url != ''
            AND (
                instr(lower(amazon_url), lower(:query)) > 0
                OR instr(lower(:query), lower(amazon_url)) > 0
            )
        ) OR (
            flipkart_url IS NOT NULL
            AND flipkart_url != ''
            AND (
                instr(lower(flipkart_url), lower(:query)) > 0
                OR instr(lower(:query), lower(flipkart_url)) > 0
            )
        )
        """
    )
    suspend fun countUrlMatches(query: String): Int

    @Query("SELECT * FROM inventory WHERE barcode = :barcode ORDER BY search_count DESC")
    suspend fun findByBarcode(barcode: String): List<InventoryItem>

    @Query("UPDATE inventory SET search_count = search_count + 1 WHERE id = :id")
    suspend fun incrementSearchCount(id: Long)

    @Query("UPDATE inventory SET search_count = search_count + 1 WHERE id IN (:ids)")
    suspend fun incrementSearchCountBulk(ids: List<Long>)

    @Query("UPDATE inventory SET amazon_last_price = :price, amazon_last_checked = :timestamp WHERE id = :itemId")
    suspend fun updateAmazonCache(itemId: Long, price: Double, timestamp: Long)

    @Query("UPDATE inventory SET flipkart_last_price = :price, flipkart_last_checked = :timestamp WHERE id = :itemId")
    suspend fun updateFlipkartCache(itemId: Long, price: Double, timestamp: Long)

    @Query("UPDATE inventory SET image_url = :imageUrl WHERE id = :itemId")
    suspend fun updateImageUrl(itemId: Long, imageUrl: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPriceHistory(entry: PriceHistoryEntry): Long

    @Query(
        "SELECT * FROM price_history " +
            "WHERE inventory_item_id = :itemId " +
            "ORDER BY checked_at DESC, id DESC LIMIT :limit"
    )
    suspend fun getPriceHistory(itemId: Long, limit: Int): List<PriceHistoryEntry>

    @Query(
        "SELECT * FROM price_history " +
            "ORDER BY checked_at ASC, id ASC"
    )
    suspend fun getAllPriceHistory(): List<PriceHistoryEntry>

    @Query(
        "DELETE FROM price_history " +
            "WHERE checked_at < :cutoffTimestamp"
    )
    suspend fun deletePriceHistoryOlderThan(cutoffTimestamp: Long)

    @Query(
        "DELETE FROM price_history " +
            "WHERE inventory_item_id = :itemId AND retailer = :retailer " +
            "AND id NOT IN (" +
            "SELECT id FROM price_history " +
            "WHERE inventory_item_id = :itemId AND retailer = :retailer " +
            "ORDER BY checked_at DESC, id DESC LIMIT :keepCount)"
    )
    suspend fun trimPriceHistory(itemId: Long, retailer: String, keepCount: Int)

    @Query("SELECT id FROM inventory WHERE amazon_url = :url AND id != :excludeId LIMIT 1")
    suspend fun checkAmazonUrlExists(url: String, excludeId: Long): Long?

    @Query("SELECT id FROM inventory WHERE flipkart_url = :url AND id != :excludeId LIMIT 1")
    suspend fun checkFlipkartUrlExists(url: String, excludeId: Long): Long?
}
