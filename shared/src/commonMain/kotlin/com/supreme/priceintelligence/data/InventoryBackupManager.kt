@file:OptIn(ExperimentalTime::class)

package com.supreme.priceintelligence.data

import com.supreme.priceintelligence.network.Retailer
import com.supreme.priceintelligence.network.normalizeRemoteImageUrl
import com.supreme.priceintelligence.network.normalizeRetailerUrl
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
private data class InventoryBackup(
    val formatVersion: Int = CURRENT_BACKUP_FORMAT,
    val exportedAt: Long,
    val products: List<BackupProduct>
)

@Serializable
private data class BackupProduct(
    val productName: String = "",
    val barcode: String? = null,
    val shopPrice: Double = 0.0,
    val purchaseCost: Double? = null,
    val pricebuddyProductId: Long? = null,
    val amazonUrl: String? = null,
    val flipkartUrl: String? = null,
    val imageUrl: String? = null,
    val searchCount: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val amazonLastPrice: Double? = null,
    val amazonLastChecked: Long? = null,
    val flipkartLastPrice: Double? = null,
    val flipkartLastChecked: Long? = null,
    val priceHistory: List<BackupPriceObservation> = emptyList()
)

@Serializable
private data class BackupPriceObservation(
    val retailer: String = "",
    val price: Double = 0.0,
    val checkedAt: Long = 0
)

data class BackupImportResult(
    val addedCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int
)

class InventoryBackupManager(
    private val repository: InventoryRepository
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun createBackupJson(): String {
        val products = repository.getAllAlphabetical().map { item ->
            item.toBackupProduct(
                priceHistory = repository.getPriceHistory(item.id).map { entry ->
                    BackupPriceObservation(
                        retailer = entry.retailer,
                        price = entry.price,
                        checkedAt = entry.checkedAt
                    )
                }.filter { observation ->
                    observation.price.isFinite() &&
                        observation.price > 0.0 &&
                        observation.checkedAt > 0L
                }
            )
        }
        return json.encodeToString(
            InventoryBackup(
                exportedAt = Clock.System.now().toEpochMilliseconds(),
                products = products
            )
        )
    }

    suspend fun importBackupJson(contents: String): BackupImportResult {
        val backup = try {
            json.decodeFromString<InventoryBackup>(contents)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("This is not a valid Price Intelligence backup", error)
        }

        require(backup.formatVersion in 1..CURRENT_BACKUP_FORMAT) {
            "This backup was created by a newer version of Price Intelligence"
        }

        val existing = repository.getAllAlphabetical()
        val names = existing.map { normalizeText(it.productName) }.toMutableSet()
        val barcodes = existing.mapNotNull { normalizeOptional(it.barcode) }.toMutableSet()
        val amazonUrls = existing.mapNotNull {
            normalizeOptional(normalizeRetailerUrl(it.amazonUrl, Retailer.AMAZON))
        }.toMutableSet()
        val flipkartUrls = existing.mapNotNull {
            normalizeOptional(normalizeRetailerUrl(it.flipkartUrl, Retailer.FLIPKART))
        }.toMutableSet()

        var added = 0
        var duplicates = 0
        var invalid = 0

        for (product in backup.products) {
            val item = product.toInventoryItemOrNull()
            if (item == null) {
                invalid++
                continue
            }

            val nameKey = normalizeText(item.productName)
            val barcodeKey = normalizeOptional(item.barcode)
            val amazonKey = normalizeOptional(item.amazonUrl)
            val flipkartKey = normalizeOptional(item.flipkartUrl)
            val isDuplicate = nameKey in names ||
                (barcodeKey != null && barcodeKey in barcodes) ||
                (amazonKey != null && amazonKey in amazonUrls) ||
                (flipkartKey != null && flipkartKey in flipkartUrls)

            if (isDuplicate) {
                duplicates++
                continue
            }

            try {
                val importedId = repository.importProduct(item)
                repository.importPriceHistory(
                    itemId = importedId,
                    entries = product.historyWithLegacyFallback().mapNotNull { observation ->
                        observation.toPriceHistoryEntryOrNull(importedId)
                    }
                )
                added++
                names += nameKey
                barcodeKey?.let { barcodes += it }
                amazonKey?.let { amazonUrls += it }
                flipkartKey?.let { flipkartUrls += it }
            } catch (error: CancellationException) {
                throw error
            } catch (_: androidx.sqlite.SQLiteException) {
                duplicates++
            }
        }

        return BackupImportResult(
            addedCount = added,
            duplicateCount = duplicates,
            invalidCount = invalid
        )
    }
}

private fun InventoryItem.toBackupProduct(
    priceHistory: List<BackupPriceObservation>
) = BackupProduct(
    productName = productName,
    barcode = barcode,
    shopPrice = shopPrice.takeIf {
        it.isFinite() && it > 0.0
    } ?: 0.0,
    purchaseCost = purchaseCost?.takeIf {
        it.isFinite() && it > 0.0
    },
    pricebuddyProductId = pricebuddyProductId,
    amazonUrl = amazonUrl,
    flipkartUrl = flipkartUrl,
    imageUrl = imageUrl,
    searchCount = searchCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    amazonLastPrice = amazonLastPrice?.takeIf { it.isFinite() && it > 0.0 },
    amazonLastChecked = amazonLastChecked,
    flipkartLastPrice = flipkartLastPrice?.takeIf { it.isFinite() && it > 0.0 },
    flipkartLastChecked = flipkartLastChecked,
    priceHistory = priceHistory
)

private fun BackupPriceObservation.toPriceHistoryEntryOrNull(
    inventoryItemId: Long
): PriceHistoryEntry? {
    if (retailer !in PriceRetailer.entries.map { it.name }) return null
    if (!price.isFinite() || price <= 0.0 || checkedAt <= 0L) return null

    return PriceHistoryEntry(
        inventoryItemId = inventoryItemId,
        retailer = retailer,
        price = price,
        checkedAt = checkedAt
    )
}

private fun BackupProduct.historyWithLegacyFallback(): List<BackupPriceObservation> {
    if (priceHistory.isNotEmpty()) return priceHistory

    return buildList {
        if (amazonLastPrice != null && amazonLastChecked != null) {
            add(
                BackupPriceObservation(
                    retailer = PriceRetailer.AMAZON.name,
                    price = amazonLastPrice,
                    checkedAt = amazonLastChecked
                )
            )
        }
        if (flipkartLastPrice != null && flipkartLastChecked != null) {
            add(
                BackupPriceObservation(
                    retailer = PriceRetailer.FLIPKART.name,
                    price = flipkartLastPrice,
                    checkedAt = flipkartLastChecked
                )
            )
        }
    }
}

private fun BackupProduct.toInventoryItemOrNull(): InventoryItem? {
    val cleanName = productName.trim()
    if (cleanName.isEmpty() || !shopPrice.isFinite() || shopPrice <= 0.0) return null

    val now = Clock.System.now().toEpochMilliseconds()
    return InventoryItem(
        productName = cleanName,
        barcode = barcode?.trim()?.ifBlank { null },
        shopPrice = shopPrice,
        purchaseCost = purchaseCost?.takeIf {
            it.isFinite() && it > 0.0
        },
        pricebuddyProductId = pricebuddyProductId,
        amazonUrl = normalizeRetailerUrl(amazonUrl, Retailer.AMAZON),
        flipkartUrl = normalizeRetailerUrl(flipkartUrl, Retailer.FLIPKART),
        imageUrl = normalizeRemoteImageUrl(imageUrl),
        searchCount = searchCount.coerceAtLeast(0),
        createdAt = createdAt.takeIf { it > 0 } ?: now,
        updatedAt = updatedAt.takeIf { it > 0 } ?: now,
        amazonLastPrice = amazonLastPrice?.takeIf { it.isFinite() && it > 0.0 },
        amazonLastChecked = amazonLastChecked?.takeIf { it > 0 },
        flipkartLastPrice = flipkartLastPrice?.takeIf { it.isFinite() && it > 0.0 },
        flipkartLastChecked = flipkartLastChecked?.takeIf { it > 0 }
    )
}

private fun normalizeText(value: String): String =
    value.trim().lowercase().replace(Regex("\\s+"), " ")

private fun normalizeOptional(value: String?): String? =
    value?.trim()?.lowercase()?.ifBlank { null }

private const val CURRENT_BACKUP_FORMAT = 2
