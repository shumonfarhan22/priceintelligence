package com.supreme.priceintelligence.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

enum class PriceRetailer {
    AMAZON,
    FLIPKART
}

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = InventoryItem::class,
            parentColumns = ["id"],
            childColumns = ["inventory_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["inventory_item_id"]),
        Index(value = ["inventory_item_id", "retailer", "checked_at"])
    ]
)
data class PriceHistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "inventory_item_id")
    val inventoryItemId: Long,

    @ColumnInfo(name = "retailer")
    val retailer: String,

    @ColumnInfo(name = "price")
    val price: Double,

    @ColumnInfo(name = "checked_at")
    val checkedAt: Long
)

internal const val MAX_PRICE_HISTORY_PER_RETAILER = 60
