package com.supreme.priceintelligence.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryRepositoryTest {
    private val phone = item(
        id = 1,
        name = "Samsung Galaxy S25 Ultra",
        barcode = "8901000000001",
        amazonUrl = "https://www.amazon.in/s25",
        searchCount = 8,
        updatedAt = 100
    )
    private val case = item(
        id = 2,
        name = "Clear Phone Case for Samsung",
        barcode = "8901000000002",
        flipkartUrl = "https://www.flipkart.com/clear-case",
        searchCount = 20,
        updatedAt = 300
    )
    private val charger = item(
        id = 3,
        name = "Apple USB C Charger",
        barcode = "8901000000003",
        searchCount = 5,
        updatedAt = 200
    )

    @Test
    fun exactBarcodeSearchWins() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone, case, charger))

        assertEquals(listOf(phone), repository.search("8901000000001"))
    }

    @Test
    fun multiWordSearchRequiresEveryWordIgnoringCase() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone, case, charger))

        assertEquals(listOf(phone), repository.search("galaxy SAMSUNG"))
        assertEquals(emptyList(), repository.search("Samsung charger"))
    }

    @Test
    fun pastedStoreLinkFindsTheProduct() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone, case, charger))

        assertEquals(listOf(case), repository.search("https://www.flipkart.com/clear-case"))
    }

    @Test
    fun suggestionsPutExactPrefixBeforeMorePopularContainsMatch() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone, case, charger))

        assertEquals(
            listOf("Samsung Galaxy S25 Ultra", "Clear Phone Case for Samsung"),
            repository.getNameSuggestions("Samsung")
        )
    }

    @Test
    fun duplicateStoreLinksIgnoreTheProductBeingEdited() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone, case, charger))

        assertTrue(repository.isAmazonUrlDuplicate("https://www.amazon.in/s25", excludeId = 2))
        assertFalse(repository.isAmazonUrlDuplicate("https://www.amazon.in/s25", excludeId = 1))
    }

    private fun item(
        id: Long,
        name: String,
        barcode: String,
        amazonUrl: String? = null,
        flipkartUrl: String? = null,
        searchCount: Int,
        updatedAt: Long
    ) = InventoryItem(
        id = id,
        productName = name,
        barcode = barcode,
        shopPrice = 100.0,
        amazonUrl = amazonUrl,
        flipkartUrl = flipkartUrl,
        searchCount = searchCount,
        createdAt = updatedAt,
        updatedAt = updatedAt
    )
}

internal class FakeInventoryDao(vararg initialItems: InventoryItem) : InventoryDao {
    private val items = initialItems.toMutableList()

    override suspend fun insert(item: InventoryItem): Long {
        val id = item.id.takeIf { it != 0L } ?: ((items.maxOfOrNull { it.id } ?: 0L) + 1L)
        items += item.copy(id = id)
        return id
    }

    override suspend fun update(item: InventoryItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = item
    }

    override suspend fun deleteById(id: Long) {
        items.removeAll { it.id == id }
    }

    override suspend fun getById(id: Long): InventoryItem? = items.firstOrNull { it.id == id }

    override suspend fun getTotalCount(): Int = items.size

    override suspend fun getAll(): List<InventoryItem> = items.sortedBy { it.productName.lowercase() }

    override suspend fun getAllRanked(): List<InventoryItem> = items.sortedWith(
        compareByDescending<InventoryItem> { it.searchCount }.thenBy { it.productName.lowercase() }
    )

    override suspend fun getAllRecent(): List<InventoryItem> = items.sortedWith(
        compareByDescending<InventoryItem> { it.updatedAt }.thenByDescending { it.id }
    )

    override suspend fun getAllAlphabeticalPaged(limit: Int, offset: Int): List<InventoryItem> =
        getAll().drop(offset).take(limit)

    override suspend fun getAllRankedPaged(limit: Int, offset: Int): List<InventoryItem> =
        getAllRanked().drop(offset).take(limit)

    override suspend fun getAllRecentPaged(limit: Int, offset: Int): List<InventoryItem> =
        getAllRecent().drop(offset).take(limit)

    override suspend fun findByBarcode(barcode: String): List<InventoryItem> =
        getAllRanked().filter { it.barcode == barcode }

    override suspend fun incrementSearchCount(id: Long, now: Long) {
        getById(id)?.let { update(it.copy(searchCount = it.searchCount + 1, updatedAt = now)) }
    }

    override suspend fun incrementSearchCountBulk(ids: List<Long>, now: Long) {
        ids.forEach { incrementSearchCount(it, now) }
    }

    override suspend fun updateAmazonCache(itemId: Long, price: Double, timestamp: Long) {
        getById(itemId)?.let {
            update(it.copy(amazonLastPrice = price, amazonLastChecked = timestamp))
        }
    }

    override suspend fun updateFlipkartCache(itemId: Long, price: Double, timestamp: Long) {
        getById(itemId)?.let {
            update(it.copy(flipkartLastPrice = price, flipkartLastChecked = timestamp))
        }
    }

    override suspend fun checkAmazonUrlExists(url: String, excludeId: Long): Long? =
        items.firstOrNull { it.id != excludeId && it.amazonUrl == url }?.id

    override suspend fun checkFlipkartUrlExists(url: String, excludeId: Long): Long? =
        items.firstOrNull { it.id != excludeId && it.flipkartUrl == url }?.id
}
