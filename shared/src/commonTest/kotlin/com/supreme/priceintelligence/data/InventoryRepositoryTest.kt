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
            listOf(
                "Samsung Galaxy S25 Ultra",
                "Clear Phone Case for Samsung"
            ),
            repository.getNameSuggestions("Samsung")
        )
    }

    @Test
    fun blankSuggestionsShowPopularInventoryProducts() = runTest {
        val repository =
            InventoryRepository(
                FakeInventoryDao(phone, case, charger)
            )

        assertEquals(
            listOf(
                "Clear Phone Case for Samsung",
                "Samsung Galaxy S25 Ultra",
                "Apple USB C Charger"
            ),
            repository.getNameSuggestions("")
        )
    }

    @Test
    fun barcodeSuggestionReturnsItsProductName() = runTest {
        val repository =
            InventoryRepository(
                FakeInventoryDao(phone, case, charger)
            )

        assertEquals(
            listOf("Samsung Galaxy S25 Ultra"),
            repository.getNameSuggestions("8901000000001")
        )
    }

    @Test
    fun completedProductNameIsNotRepeatedAsASuggestion() = runTest {
        val repository =
            InventoryRepository(
                FakeInventoryDao(phone, case, charger)
            )

        assertEquals(
            emptyList(),
            repository.getNameSuggestions(
                "Samsung Galaxy S25 Ultra"
            )
        )
    }

    @Test
    fun nameSearchFiltersBeforePaginationAndReturnsCorrectCount() = runTest {
        val products = (1..12).map { number ->
            item(
                id = number.toLong(),
                name = "Samsung Test Product ${number.toString().padStart(2, '0')}",
                barcode = "test-barcode-$number",
                searchCount = 0,
                updatedAt = number.toLong()
            )
        }
        val repository = InventoryRepository(
            FakeInventoryDao(*products.toTypedArray())
        )

        val secondPage = repository.searchPaged(
            query = "test SAMSUNG",
            sortOrder = "ALPHABETICAL",
            limit = 5,
            offset = 5
        )

        assertEquals(12, repository.getSearchCount("test SAMSUNG"))
        assertEquals(
            listOf(
                "Samsung Test Product 06",
                "Samsung Test Product 07",
                "Samsung Test Product 08",
                "Samsung Test Product 09",
                "Samsung Test Product 10"
            ),
            secondPage.map { product -> product.productName }
        )
    }

    @Test
    fun retailerLinkSearchAndCountIgnoreLetterCase() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone, case, charger))
        val uppercaseLink = "HTTPS://WWW.FLIPKART.COM/CLEAR-CASE"

        assertEquals(
            listOf(case),
            repository.searchPaged(
                query = uppercaseLink,
                sortOrder = "ALPHABETICAL",
                limit = 10,
                offset = 0
            )
        )
        assertEquals(1, repository.getSearchCount(uppercaseLink))
    }

    @Test
    fun duplicateStoreLinksIgnoreTheProductBeingEdited() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone, case, charger))

        assertTrue(repository.isAmazonUrlDuplicate("https://www.amazon.in/s25", excludeId = 2))
        assertFalse(repository.isAmazonUrlDuplicate("https://www.amazon.in/s25", excludeId = 1))
    }

    @Test
    fun priceChecksUpdateCacheAndKeepOnlyTheNewestHistory() = runTest {
        val dao = FakeInventoryDao(phone)
        val repository = InventoryRepository(dao)

        repeat(65) { index ->
            repository.recordPriceCheck(
                itemId = phone.id,
                retailer = PriceRetailer.AMAZON,
                price = 1000.0 - index,
                checkedAt = 1000L + index
            )
        }

        val history = repository.getPriceHistory(phone.id)
        assertEquals(60, history.size)
        assertEquals(936.0, history.first().price)
        assertEquals(995.0, history.last().price)
        val updatedPhone = repository.getAllAlphabetical().single()
        assertEquals(936.0, updatedPhone.amazonLastPrice)
        assertEquals(1064L, updatedPhone.amazonLastChecked)
        assertEquals(1, dao.priceHistoryCleanupCallCount)
    }

    @Test
    fun viewingAProductDoesNotPretendItWasEdited() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone))

        repository.incrementSearchCount(phone.id)

        val viewedPhone = repository.getAllAlphabetical().single()
        assertEquals(phone.updatedAt, viewedPhone.updatedAt)
        assertEquals(phone.searchCount + 1, viewedPhone.searchCount)
    }

    @Test
    fun imageRefreshPreservesAProductEditMadeWhileRefreshing() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone))

        repository.updateProduct(
            phone.copy(
                productName = "Edited Samsung Phone",
                shopPrice = 75_000.0,
                barcode = "edited-barcode"
            )
        )
        val editedBeforeImage = requireNotNull(
            repository.getProductById(phone.id)
        )

        repository.updateImageUrl(
            itemId = phone.id,
            imageUrl = "https://images.example.com/phone.jpg"
        )

        val afterImageRefresh = requireNotNull(
            repository.getProductById(phone.id)
        )
        assertEquals("Edited Samsung Phone", afterImageRefresh.productName)
        assertEquals(75_000.0, afterImageRefresh.shopPrice)
        assertEquals("edited-barcode", afterImageRefresh.barcode)
        assertEquals(editedBeforeImage.updatedAt, afterImageRefresh.updatedAt)
        assertEquals(
            "https://images.example.com/phone.jpg",
            afterImageRefresh.imageUrl
        )
    }

    @Test
    fun latePriceResultIsIgnoredAfterProductWasDeleted() = runTest {
        val repository = InventoryRepository(FakeInventoryDao(phone))
        repository.deleteProduct(phone.id)

        val wasSaved = repository.recordPriceCheck(
            itemId = phone.id,
            retailer = PriceRetailer.AMAZON,
            price = 900.0,
            checkedAt = 500
        )

        assertFalse(wasSaved)
        assertTrue(repository.getPriceHistory(phone.id).isEmpty())
    }

    @Test
    fun bestSavingSortUsesSavedPricesAndPutsUnknownPricesLast() = runTest {
        val smallSaving = phone.copy(
            shopPrice = 50_000.0,
            amazonLastPrice = 49_000.0
        )
        val largeSaving = case.copy(
            shopPrice = 25_000.0,
            flipkartLastPrice = 20_000.0
        )
        val noSavedPrice = charger.copy(shopPrice = 2_000.0)
        val repository = InventoryRepository(
            FakeInventoryDao(smallSaving, noSavedPrice, largeSaving)
        )

        assertEquals(
            listOf(largeSaving, smallSaving, noSavedPrice),
            repository.getPaged("BEST_SAVING", limit = 10, offset = 0)
        )
    }

    @Test
    fun bestSavingSortStillWorksInsideSearchResults() = runTest {
        val smallerSaving = phone.copy(
            productName = "Samsung Phone A",
            shopPrice = 10_000.0,
            amazonLastPrice = 9_500.0
        )
        val largerSaving = case.copy(
            productName = "Samsung Phone B",
            shopPrice = 10_000.0,
            flipkartLastPrice = 8_000.0
        )
        val repository = InventoryRepository(FakeInventoryDao(smallerSaving, largerSaving))

        assertEquals(
            listOf(largerSaving, smallerSaving),
            repository.searchPaged("Samsung", "BEST_SAVING", limit = 10, offset = 0)
        )
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
    private val priceHistory = mutableListOf<PriceHistoryEntry>()

    var priceHistoryCleanupCallCount: Int = 0
        private set

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
        priceHistory.removeAll { it.inventoryItemId == id }
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

    override suspend fun searchNamePaged(
        word1: String,
        word2: String,
        word3: String,
        word4: String,
        word5: String,
        word6: String,
        sortOrder: String,
        limit: Int,
        offset: Int
    ): List<InventoryItem> {
        val matches = matchingNames(
            word1 = word1,
            word2 = word2,
            word3 = word3,
            word4 = word4,
            word5 = word5,
            word6 = word6
        )

        val sorted = when (sortOrder) {
            "ALPHABETICAL" ->
                matches.sortedBy { item -> item.productName.lowercase() }

            "RECENT" ->
                matches.sortedWith(
                    compareByDescending<InventoryItem> { item -> item.updatedAt }
                        .thenByDescending { item -> item.id }
                )

            else ->
                matches.sortedWith(
                    compareByDescending<InventoryItem> { item -> item.searchCount }
                        .thenBy { item -> item.productName.lowercase() }
                )
        }

        return sorted.drop(offset).take(limit)
    }

    override suspend fun countNameMatches(
        word1: String,
        word2: String,
        word3: String,
        word4: String,
        word5: String,
        word6: String
    ): Int = matchingNames(
        word1 = word1,
        word2 = word2,
        word3 = word3,
        word4 = word4,
        word5 = word5,
        word6 = word6
    ).size

    override suspend fun searchNameSuggestions(
        prefix: String,
        word1: String,
        word2: String,
        word3: String,
        word4: String,
        word5: String,
        word6: String,
        limit: Int
    ): List<String> {
        val normalizedPrefix = prefix.lowercase()

        return matchingNames(
            word1 = word1,
            word2 = word2,
            word3 = word3,
            word4 = word4,
            word5 = word5,
            word6 = word6
        )
            .groupBy { item -> item.productName.lowercase() }
            .map { (_, duplicateNames) ->
                duplicateNames.maxBy { item -> item.searchCount }
            }
            .sortedWith(
                compareBy<InventoryItem> { item ->
                    if (item.productName.lowercase().startsWith(normalizedPrefix)) 0 else 1
                }
                    .thenByDescending { item -> item.searchCount }
                    .thenBy { item -> item.productName.lowercase() }
            )
            .take(limit)
            .map { item -> item.productName }
    }

    override suspend fun searchUrlPaged(
        query: String,
        limit: Int,
        offset: Int
    ): List<InventoryItem> =
        matchingUrls(query)
            .sortedWith(
                compareByDescending<InventoryItem> { item -> item.searchCount }
                    .thenBy { item -> item.productName.lowercase() }
            )
            .drop(offset)
            .take(limit)

    override suspend fun countUrlMatches(query: String): Int =
        matchingUrls(query).size

    private fun matchingNames(
        word1: String,
        word2: String,
        word3: String,
        word4: String,
        word5: String,
        word6: String
    ): List<InventoryItem> {
        val words = listOf(word1, word2, word3, word4, word5, word6)
            .filter { word -> word.isNotBlank() }

        return items.filter { item ->
            words.all { word ->
                item.productName.contains(word, ignoreCase = true)
            }
        }
    }

    private fun matchingUrls(query: String): List<InventoryItem> {
        val normalizedQuery = query.lowercase()

        return items.filter { item ->
            val amazon = item.amazonUrl?.lowercase()
            val flipkart = item.flipkartUrl?.lowercase()

            val amazonMatches =
                !amazon.isNullOrEmpty() &&
                    (
                        amazon.contains(normalizedQuery) ||
                            normalizedQuery.contains(amazon)
                    )
            val flipkartMatches =
                !flipkart.isNullOrEmpty() &&
                    (
                        flipkart.contains(normalizedQuery) ||
                            normalizedQuery.contains(flipkart)
                    )

            amazonMatches || flipkartMatches
        }
    }

    override suspend fun findByBarcode(barcode: String): List<InventoryItem> =
        getAllRanked().filter { it.barcode == barcode }

    override suspend fun incrementSearchCount(id: Long) {
        getById(id)?.let { update(it.copy(searchCount = it.searchCount + 1)) }
    }

    override suspend fun incrementSearchCountBulk(ids: List<Long>) {
        ids.forEach { incrementSearchCount(it) }
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

    override suspend fun updateImageUrl(itemId: Long, imageUrl: String) {
        getById(itemId)?.let {
            update(it.copy(imageUrl = imageUrl))
        }
    }

    override suspend fun insertPriceHistory(entry: PriceHistoryEntry): Long {
        val id = entry.id.takeIf { it != 0L } ?: ((priceHistory.maxOfOrNull { it.id } ?: 0L) + 1L)
        priceHistory += entry.copy(id = id)
        return id
    }

    override suspend fun getPriceHistory(itemId: Long, limit: Int): List<PriceHistoryEntry> =
        priceHistory
            .filter { it.inventoryItemId == itemId }
            .sortedWith(
                compareByDescending<PriceHistoryEntry> {
                    it.checkedAt
                }.thenByDescending { it.id }
            )
            .take(limit)

    override suspend fun getAllPriceHistory():
        List<PriceHistoryEntry> =
        priceHistory.sortedWith(
            compareBy<PriceHistoryEntry> {
                it.checkedAt
            }.thenBy { it.id }
        )

    override suspend fun deletePriceHistoryOlderThan(cutoffTimestamp: Long) {
        priceHistoryCleanupCallCount += 1
        priceHistory.removeAll { entry ->
            entry.checkedAt < cutoffTimestamp
        }
    }

    override suspend fun trimPriceHistory(itemId: Long, retailer: String, keepCount: Int) {
        val keepIds = priceHistory
            .filter { it.inventoryItemId == itemId && it.retailer == retailer }
            .sortedWith(compareByDescending<PriceHistoryEntry> { it.checkedAt }.thenByDescending { it.id })
            .take(keepCount)
            .map { it.id }
            .toSet()
        priceHistory.removeAll { entry ->
            entry.inventoryItemId == itemId && entry.retailer == retailer && entry.id !in keepIds
        }
    }

    override suspend fun checkAmazonUrlExists(url: String, excludeId: Long): Long? =
        items.firstOrNull { it.id != excludeId && it.amazonUrl == url }?.id

    override suspend fun checkFlipkartUrlExists(url: String, excludeId: Long): Long? =
        items.firstOrNull { it.id != excludeId && it.flipkartUrl == url }?.id
}
