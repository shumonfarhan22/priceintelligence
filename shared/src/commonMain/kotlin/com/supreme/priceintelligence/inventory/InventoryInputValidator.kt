package com.supreme.priceintelligence.inventory

import com.supreme.priceintelligence.network.Retailer
import com.supreme.priceintelligence.network.normalizeRetailerUrl

internal data class ValidatedInventoryInput(
    val productName: String,
    val shopPrice: Double,
    val barcode: String?,
    val amazonUrl: String?,
    val flipkartUrl: String?
)

internal data class InventoryInputValidation(
    val input: ValidatedInventoryInput? = null,
    val errorMessage: String? = null
)

internal fun validateInventoryInput(
    productName: String,
    shopPrice: String,
    barcode: String,
    amazonUrl: String,
    flipkartUrl: String
): InventoryInputValidation {
    val cleanName = productName.trim()
    if (cleanName.isEmpty()) {
        return InventoryInputValidation(errorMessage = "Product name is required")
    }

    val cleanPrice = shopPrice.trim().toDoubleOrNull()
        ?: return InventoryInputValidation(errorMessage = "Enter a valid shop price")

    if (!cleanPrice.isFinite() || cleanPrice <= 0.0) {
        return InventoryInputValidation(errorMessage = "Shop price must be greater than zero")
    }

    val rawAmazon = amazonUrl.trim().ifBlank { null }
    val cleanAmazon = normalizeRetailerUrl(rawAmazon, Retailer.AMAZON)
    if (rawAmazon != null && cleanAmazon == null) {
        return InventoryInputValidation(
            errorMessage = "Enter a valid Amazon product link"
        )
    }

    val rawFlipkart = flipkartUrl.trim().ifBlank { null }
    val cleanFlipkart = normalizeRetailerUrl(rawFlipkart, Retailer.FLIPKART)
    if (rawFlipkart != null && cleanFlipkart == null) {
        return InventoryInputValidation(
            errorMessage = "Enter a valid Flipkart product link"
        )
    }

    return InventoryInputValidation(
        input = ValidatedInventoryInput(
            productName = cleanName,
            shopPrice = cleanPrice,
            barcode = barcode.trim().ifBlank { null },
            amazonUrl = cleanAmazon,
            flipkartUrl = cleanFlipkart
        )
    )
}
