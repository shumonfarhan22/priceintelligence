package com.supreme.priceintelligence.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

@Database(
    entities = [InventoryItem::class, PriceHistoryEntry::class],
    version = 5,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
}

// Room's own compiler generates the actual platform implementations of this —
// we just declare that it exists.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

// Adds the search indices. Old version-1 data could contain repeated barcodes,
// so keep the oldest row's barcode and clear later duplicates before enforcing
// the same uniqueness rule used by the current entity.
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_product_name ON inventory(product_name)")
        connection.execSQL(
            """
            UPDATE inventory
            SET barcode = NULL
            WHERE barcode IS NOT NULL
              AND id NOT IN (
                SELECT MIN(id)
                FROM inventory
                WHERE barcode IS NOT NULL
                GROUP BY barcode
              )
            """.trimIndent()
        )
        connection.execSQL("DROP INDEX IF EXISTS index_inventory_barcode")
        connection.execSQL("CREATE UNIQUE INDEX index_inventory_barcode ON inventory(barcode)")
    }
}

// Version 3 introduced the saved-price fields. Without this upgrade, an
// existing version-2 database cannot open after the app is updated.
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE inventory ADD COLUMN amazon_last_price REAL")
        connection.execSQL("ALTER TABLE inventory ADD COLUMN amazon_last_checked INTEGER")
        connection.execSQL("ALTER TABLE inventory ADD COLUMN flipkart_last_price REAL")
        connection.execSQL("ALTER TABLE inventory ADD COLUMN flipkart_last_checked INTEGER")
    }
}

// Version 4 keeps a bounded local history of successful retailer checks. Seed
// it from the previously saved prices so upgrades do not start with an empty
// history when useful cached data already exists.
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS price_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                inventory_item_id INTEGER NOT NULL,
                retailer TEXT NOT NULL,
                price REAL NOT NULL,
                checked_at INTEGER NOT NULL,
                FOREIGN KEY(inventory_item_id) REFERENCES inventory(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_price_history_inventory_item_id " +
                "ON price_history(inventory_item_id)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_price_history_inventory_item_id_retailer_checked_at " +
                "ON price_history(inventory_item_id, retailer, checked_at)"
        )
        connection.execSQL(
            """
            INSERT INTO price_history (inventory_item_id, retailer, price, checked_at)
            SELECT id, 'AMAZON', amazon_last_price, amazon_last_checked
            FROM inventory
            WHERE amazon_last_price > 0 AND amazon_last_checked > 0
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO price_history (inventory_item_id, retailer, price, checked_at)
            SELECT id, 'FLIPKART', flipkart_last_price, flipkart_last_checked
            FROM inventory
            WHERE flipkart_last_price > 0 AND flipkart_last_checked > 0
            """.trimIndent()
        )
    }
}

// Version 5 adds an optional retailer purchase cost. Existing products keep
// NULL because their purchase cost is not known.
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE inventory ADD COLUMN purchase_cost REAL"
        )
    }
}


// Takes the platform-specific builder (which only knows WHERE the db file lives)
// and applies the configuration that's identical everywhere — migrations, the
// driver, and the coroutine context queries run on.
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5
        )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(databaseDispatcher)
        .build()
}
