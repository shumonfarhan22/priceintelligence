import * as SQLite from 'expo-sqlite';
import { Platform } from 'react-native';

const DATABASE_NAME = 'price-intelligence-v2.db';
const DATABASE_VERSION = 1;

let databasePromise: Promise<SQLite.SQLiteDatabase> | null = null;

export function getDatabase(): Promise<SQLite.SQLiteDatabase> {
  if (!databasePromise) {
    databasePromise = openAndMigrateDatabase();
  }
  return databasePromise;
}

export async function withWriteTransaction(
  database: SQLite.SQLiteDatabase,
  task: (transaction: SQLite.SQLiteDatabase) => Promise<void>,
): Promise<void> {
  if (Platform.OS === 'web') {
    await database.withTransactionAsync(() => task(database));
    return;
  }
  await database.withExclusiveTransactionAsync(task);
}

async function openAndMigrateDatabase(): Promise<SQLite.SQLiteDatabase> {
  const database = await SQLite.openDatabaseAsync(DATABASE_NAME);
  await database.execAsync('PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL;');
  const versionRow = await database.getFirstAsync<{ user_version: number }>('PRAGMA user_version');
  const currentVersion = versionRow?.user_version ?? 0;

  if (currentVersion > DATABASE_VERSION) {
    await database.closeAsync();
    databasePromise = null;
    throw new Error('This database was created by a newer version of Price Intelligence.');
  }

  if (currentVersion === 0) {
    await withWriteTransaction(database, async (transaction) => {
      await transaction.execAsync(`
        CREATE TABLE inventory (
          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          product_name TEXT NOT NULL,
          barcode TEXT,
          shop_price REAL NOT NULL CHECK(shop_price > 0),
          purchase_cost REAL,
          pricebuddy_product_id INTEGER,
          amazon_url TEXT,
          flipkart_url TEXT,
          image_url TEXT,
          search_count INTEGER NOT NULL DEFAULT 0 CHECK(search_count >= 0),
          created_at INTEGER NOT NULL CHECK(created_at > 0),
          updated_at INTEGER NOT NULL CHECK(updated_at > 0),
          amazon_last_price REAL,
          amazon_last_checked INTEGER,
          flipkart_last_price REAL,
          flipkart_last_checked INTEGER
        );
        CREATE UNIQUE INDEX index_inventory_barcode
          ON inventory(barcode) WHERE barcode IS NOT NULL;
        CREATE INDEX index_inventory_product_name ON inventory(product_name COLLATE NOCASE);

        CREATE TABLE price_history (
          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          inventory_item_id INTEGER NOT NULL,
          retailer TEXT NOT NULL CHECK(retailer IN ('AMAZON', 'FLIPKART')),
          price REAL NOT NULL CHECK(price > 0),
          checked_at INTEGER NOT NULL,
          FOREIGN KEY(inventory_item_id) REFERENCES inventory(id) ON DELETE CASCADE
        );
        CREATE INDEX index_price_history_item ON price_history(inventory_item_id);
        CREATE INDEX index_price_history_timeline
          ON price_history(inventory_item_id, retailer, checked_at);
        CREATE UNIQUE INDEX index_price_history_observation
          ON price_history(inventory_item_id, retailer, checked_at, price);
        CREATE TRIGGER bound_price_history_after_insert
          AFTER INSERT ON price_history
          BEGIN
            DELETE FROM price_history
            WHERE id IN (
              SELECT id
              FROM price_history
              WHERE inventory_item_id = NEW.inventory_item_id
                AND retailer = NEW.retailer
              ORDER BY checked_at DESC, id DESC
              LIMIT -1 OFFSET 60
            );
          END;

        PRAGMA user_version = ${DATABASE_VERSION};
      `);
    });
  }

  await database.execAsync(`
    UPDATE inventory
    SET image_url = NULL
    WHERE image_url LIKE '%/images/G/%'
       OR lower(image_url) LIKE '%prime%'
       OR lower(image_url) LIKE '%badge%'
       OR lower(image_url) LIKE '%sprite%';
  `);

  return database;
}
