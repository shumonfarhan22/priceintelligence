@file:OptIn(ExperimentalTime::class)

package com.supreme.priceintelligence.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Suppress("SpellCheckingInspection")
@Entity(
    tableName = "inventory",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["product_name"])
    ]
)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "product_name")
    val productName: String,

    @ColumnInfo(name = "barcode")
    val barcode: String? = null,

    @ColumnInfo(name = "shop_price")
    val shopPrice: Double,

    @ColumnInfo(name = "purchase_cost")
    val purchaseCost: Double? = null,

    @ColumnInfo(name = "pricebuddy_product_id")
    val pricebuddyProductId: Long? = null,

    @ColumnInfo(name = "amazon_url")
    val amazonUrl: String? = null,

    @ColumnInfo(name = "flipkart_url")
    val flipkartUrl: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "search_count")
    val searchCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),

    // --- PRICE MEMORY BANK ---
    @ColumnInfo(name = "amazon_last_price") val amazonLastPrice: Double? = null,
    @ColumnInfo(name = "amazon_last_checked") val amazonLastChecked: Long? = null,
    @ColumnInfo(name = "flipkart_last_price") val flipkartLastPrice: Double? = null,
    @ColumnInfo(name = "flipkart_last_checked") val flipkartLastChecked: Long? = null
)