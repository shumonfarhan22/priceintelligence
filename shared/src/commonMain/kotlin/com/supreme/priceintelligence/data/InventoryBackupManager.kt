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
    val flipkartLastChecked: Long? = null
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
        val products = repository.getAllAlphabetical().map { it.toBackupProduct() }
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
                repository.importProduct(item)
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

private fun InventoryItem.toBackupProduct() = BackupProduct(
    productName = productName,
    barcode = barcode,
    shopPrice = shopPrice,
    pricebuddyProductId = pricebuddyProductId,
    amazonUrl = amazonUrl,
    flipkartUrl = flipkartUrl,
    imageUrl = imageUrl,
    searchCount = searchCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    amazonLastPrice = amazonLastPrice,
    amazonLastChecked = amazonLastChecked,
    flipkartLastPrice = flipkartLastPrice,
    flipkartLastChecked = flipkartLastChecked
)

private fun BackupProduct.toInventoryItemOrNull(): InventoryItem? {
    val cleanName = productName.trim()
    if (cleanName.isEmpty() || !shopPrice.isFinite() || shopPrice <= 0.0) return null

    val now = Clock.System.now().toEpochMilliseconds()
    return InventoryItem(
        productName = cleanName,
        barcode = barcode?.trim()?.ifBlank { null },
        shopPrice = shopPrice,
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

private const val CURRENT_BACKUP_FORMAT = 1
