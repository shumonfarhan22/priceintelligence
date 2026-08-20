package com.supreme.priceintelligence.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(entities = [InventoryItem::class], version = 3, exportSchema = true)
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

// Takes the platform-specific builder (which only knows WHERE the db file lives)
// and applies the configuration that's identical everywhere — migrations, the
// driver, and the coroutine context queries run on.
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}
