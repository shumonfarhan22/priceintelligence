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

@Database(entities = [InventoryItem::class], version = 3, exportSchema = false)
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

// Same migration as before — adds indices on product_name and barcode so search
// and barcode lookups stay fast — just speaking the new SQLiteConnection API
// instead of the old Android-only SupportSQLiteDatabase.
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_product_name ON inventory(product_name)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_barcode ON inventory(barcode)")
    }
}

// Takes the platform-specific builder (which only knows WHERE the db file lives)
// and applies the configuration that's identical everywhere — migrations, the
// driver, and the coroutine context queries run on.
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .addMigrations(MIGRATION_1_2)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}