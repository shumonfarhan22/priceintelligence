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

    @Query("SELECT * FROM inventory WHERE barcode = :barcode ORDER BY search_count DESC")
    suspend fun findByBarcode(barcode: String): List<InventoryItem>

    @Query("UPDATE inventory SET search_count = search_count + 1, updated_at = :now WHERE id = :id")
    suspend fun incrementSearchCount(id: Long, now: Long)

    @Query("UPDATE inventory SET search_count = search_count + 1, updated_at = :now WHERE id IN (:ids)")
    suspend fun incrementSearchCountBulk(ids: List<Long>, now: Long)

    @Query("UPDATE inventory SET amazon_last_price = :price, amazon_last_checked = :timestamp WHERE id = :itemId")
    suspend fun updateAmazonCache(itemId: Long, price: Double, timestamp: Long)

    @Query("UPDATE inventory SET flipkart_last_price = :price, flipkart_last_checked = :timestamp WHERE id = :itemId")
    suspend fun updateFlipkartCache(itemId: Long, price: Double, timestamp: Long)

    @Query("SELECT id FROM inventory WHERE amazon_url = :url AND id != :excludeId LIMIT 1")
    suspend fun checkAmazonUrlExists(url: String, excludeId: Long): Long?

    @Query("SELECT id FROM inventory WHERE flipkart_url = :url AND id != :excludeId LIMIT 1")
    suspend fun checkFlipkartUrlExists(url: String, excludeId: Long): Long?
}