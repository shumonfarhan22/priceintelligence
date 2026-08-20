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
        val sourceManager = InventoryBackupManager(
            InventoryRepository(FakeInventoryDao(sourceItem))
        )
        val backupJson = sourceManager.createBackupJson()

        val destinationRepository = InventoryRepository(FakeInventoryDao())
        val result = InventoryBackupManager(destinationRepository).importBackupJson(backupJson)

        assertEquals(BackupImportResult(1, 0, 0), result)
        val restored = assertNotNull(destinationRepository.getAllAlphabetical().singleOrNull())
        assertTrue(restored.id > 0L)
        assertEquals(sourceItem.productName, restored.productName)
        assertEquals(sourceItem.barcode, restored.barcode)
        assertEquals(sourceItem.shopPrice, restored.shopPrice)
        assertEquals(sourceItem.amazonLastPrice, restored.amazonLastPrice)
        assertEquals(sourceItem.amazonLastChecked, restored.amazonLastChecked)
        assertEquals(sourceItem.imageUrl, restored.imageUrl)
        assertEquals(sourceItem.searchCount, restored.searchCount)
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
}
