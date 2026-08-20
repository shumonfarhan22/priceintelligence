package com.supreme.priceintelligence.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InventoryBackupManagerTest {
    @Test
    fun exportsAndRestoresAllPriceInformation() = runTest {
        val sourceItem = InventoryItem(
            id = 7,
            productName = "Samsung Galaxy S25",
            barcode = "8901000000001",
            shopPrice = 74999.0,
            amazonUrl = "https://www.amazon.in/s25",
            imageUrl = "https://images.example/s25.jpg",
            searchCount = 9,
            createdAt = 100,
            updatedAt = 200,
            amazonLastPrice = 72999.0,
            amazonLastChecked = 300
        )
        val sourceRepository = InventoryRepository(FakeInventoryDao(sourceItem))
        sourceRepository.recordPriceCheck(
            itemId = sourceItem.id,
            retailer = PriceRetailer.AMAZON,
            price = 71999.0,
            checkedAt = 400
        )
        val sourceManager = InventoryBackupManager(sourceRepository)
        val backupJson = sourceManager.createBackupJson()

        val destinationRepository = InventoryRepository(FakeInventoryDao())
        val result = InventoryBackupManager(destinationRepository).importBackupJson(backupJson)

        assertEquals(BackupImportResult(1, 0, 0), result)
        val restored = assertNotNull(destinationRepository.getAllAlphabetical().singleOrNull())
        assertTrue(restored.id > 0L)
        assertEquals(sourceItem.productName, restored.productName)
        assertEquals(sourceItem.barcode, restored.barcode)
        assertEquals(sourceItem.shopPrice, restored.shopPrice)
        assertEquals(71999.0, restored.amazonLastPrice)
        assertEquals(400, restored.amazonLastChecked)
        assertEquals(sourceItem.imageUrl, restored.imageUrl)
        assertEquals(sourceItem.searchCount, restored.searchCount)
        val restoredHistory = destinationRepository.getPriceHistory(restored.id)
        assertEquals(1, restoredHistory.size)
        assertEquals(71999.0, restoredHistory.single().price)
        assertEquals(400, restoredHistory.single().checkedAt)
    }

    @Test
    fun skipsProductsAlreadyPresentByNameOrBarcode() = runTest {
        val existing = InventoryItem(
            id = 1,
            productName = "Existing Phone",
            barcode = "12345",
            shopPrice = 100.0
        )
        val backupJson = """
            {
              "formatVersion": 1,
              "exportedAt": 100,
              "products": [
                {"productName":" existing   phone ","shopPrice":200.0},
                {"productName":"Different name","barcode":"12345","shopPrice":300.0},
                {"productName":"New charger","barcode":"67890","shopPrice":400.0}
              ]
            }
        """.trimIndent()
        val repository = InventoryRepository(FakeInventoryDao(existing))

        val result = InventoryBackupManager(repository).importBackupJson(backupJson)

        assertEquals(BackupImportResult(1, 2, 0), result)
        assertEquals(2, repository.getAllAlphabetical().size)
    }

    @Test
    fun countsInvalidRowsWithoutBlockingGoodRows() = runTest {
        val backupJson = """
            {
              "formatVersion": 1,
              "exportedAt": 100,
              "products": [
                {"productName":"","shopPrice":100.0},
                {"productName":"Free item","shopPrice":0.0},
                {"productName":"Valid item","shopPrice":50.0}
              ]
            }
        """.trimIndent()
        val repository = InventoryRepository(FakeInventoryDao())

        val result = InventoryBackupManager(repository).importBackupJson(backupJson)

        assertEquals(BackupImportResult(1, 0, 2), result)
    }

    @Test
    fun rejectsBrokenOrNewerBackupFiles() = runTest {
        val manager = InventoryBackupManager(InventoryRepository(FakeInventoryDao()))

        assertFailsWith<IllegalArgumentException> {
            manager.importBackupJson("not json")
        }
        assertFailsWith<IllegalArgumentException> {
            manager.importBackupJson(
                """{"formatVersion":99,"exportedAt":100,"products":[]}"""
            )
        }
    }

    @Test
    fun removesUnsafeLinksFromEditedBackupFiles() = runTest {
        val backupJson = """
            {
              "formatVersion": 1,
              "exportedAt": 100,
              "products": [
                {
                  "productName":"Safe product",
                  "shopPrice":100.0,
                  "amazonUrl":"javascript:alert(1)",
                  "flipkartUrl":"http://www.flipkart.com/item",
                  "imageUrl":"file:///private/data.jpg"
                }
              ]
            }
        """.trimIndent()
        val repository = InventoryRepository(FakeInventoryDao())

        InventoryBackupManager(repository).importBackupJson(backupJson)

        val imported = repository.getAllAlphabetical().single()
        assertEquals(null, imported.amazonUrl)
        assertEquals("https://www.flipkart.com/item", imported.flipkartUrl)
        assertEquals(null, imported.imageUrl)
    }

    @Test
    fun versionOneBackupTurnsSavedCacheIntoFirstHistoryEntry() = runTest {
        val backupJson = """
            {
              "formatVersion": 1,
              "exportedAt": 100,
              "products": [
                {
                  "productName":"Legacy phone",
                  "shopPrice":1000.0,
                  "amazonLastPrice":900.0,
                  "amazonLastChecked":200
                }
              ]
            }
        """.trimIndent()
        val repository = InventoryRepository(FakeInventoryDao())

        InventoryBackupManager(repository).importBackupJson(backupJson)

        val restored = repository.getAllAlphabetical().single()
        val history = repository.getPriceHistory(restored.id)
        assertEquals(1, history.size)
        assertEquals(PriceRetailer.AMAZON.name, history.single().retailer)
        assertEquals(900.0, history.single().price)
        assertEquals(200, history.single().checkedAt)
    }

    @Test
    fun corruptedLegacyNumbersDoNotBreakTheWholeBackup() = runTest {
        val corrupted = InventoryItem(
            id = 1,
            productName = "Legacy typo",
            shopPrice = Double.NaN,
            amazonLastPrice = Double.POSITIVE_INFINITY,
            amazonLastChecked = 100
        )

        val backupJson = InventoryBackupManager(
            InventoryRepository(FakeInventoryDao(corrupted))
        ).createBackupJson()

        assertTrue("NaN" !in backupJson)
        assertTrue("Infinity" !in backupJson)
        assertTrue("\"shopPrice\": 0.0" in backupJson)
    }
}
