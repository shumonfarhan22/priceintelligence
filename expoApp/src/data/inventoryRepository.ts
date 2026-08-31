import type { SQLiteDatabase } from 'expo-sqlite';

import type { BackupImportResult, ImportedProduct, InventoryProduct, PriceObservation } from '../domain/models';
import {
  encodeBackupDocument,
  normalizeNameKey,
  normalizeOptionalKey,
  parseBackupJson,
} from './backup';
import { getDatabase } from './database';

interface InventoryRow {
  id: number;
  product_name: string;
  barcode: string | null;
  shop_price: number;
  purchase_cost: number | null;
  pricebuddy_product_id: number | null;
  amazon_url: string | null;
  flipkart_url: string | null;
  image_url: string | null;
  search_count: number;
  created_at: number;
  updated_at: number;
  amazon_last_price: number | null;
  amazon_last_checked: number | null;
  flipkart_last_price: number | null;
  flipkart_last_checked: number | null;
}

interface HistoryRow {
  retailer: 'AMAZON' | 'FLIPKART';
  price: number;
  checked_at: number;
}

export class InventoryRepository {
  private constructor(private readonly database: SQLiteDatabase) {}

  static async create(): Promise<InventoryRepository> {
    return new InventoryRepository(await getDatabase());
  }

  async countProducts(): Promise<number> {
    const row = await this.database.getFirstAsync<{ count: number }>(
      'SELECT COUNT(*) AS count FROM inventory',
    );
    return row?.count ?? 0;
  }

  async importBackupJson(contents: string): Promise<BackupImportResult> {
    const parsed = parseBackupJson(contents);
    let addedCount = 0;
    let duplicateCount = 0;

    await this.database.withExclusiveTransactionAsync(async (transaction) => {
      const existing = await transaction.getAllAsync<InventoryRow>('SELECT * FROM inventory');
      const names = new Set(existing.map((item) => normalizeNameKey(item.product_name)));
      const barcodes = keySet(existing.map((item) => item.barcode));
      const amazonUrls = keySet(existing.map((item) => item.amazon_url));
      const flipkartUrls = keySet(existing.map((item) => item.flipkart_url));

      for (const product of parsed.products) {
        const nameKey = normalizeNameKey(product.productName);
        const barcodeKey = normalizeOptionalKey(product.barcode);
        const amazonKey = normalizeOptionalKey(product.amazonUrl);
        const flipkartKey = normalizeOptionalKey(product.flipkartUrl);
        const duplicate = names.has(nameKey)
          || (barcodeKey != null && barcodes.has(barcodeKey))
          || (amazonKey != null && amazonUrls.has(amazonKey))
          || (flipkartKey != null && flipkartUrls.has(flipkartKey));

        if (duplicate) {
          duplicateCount += 1;
          continue;
        }

        const inserted = await insertProduct(transaction, product);
        await insertHistory(transaction, Number(inserted.lastInsertRowId), product.priceHistory);
        addedCount += 1;
        names.add(nameKey);
        if (barcodeKey) barcodes.add(barcodeKey);
        if (amazonKey) amazonUrls.add(amazonKey);
        if (flipkartKey) flipkartUrls.add(flipkartKey);
      }
    });

    return {
      addedCount,
      duplicateCount,
      invalidCount: parsed.invalidCount,
    };
  }

  async createBackupJson(): Promise<string> {
    const rows = await this.database.getAllAsync<InventoryRow>(
      'SELECT * FROM inventory ORDER BY product_name COLLATE NOCASE',
    );
    const products: ImportedProduct[] = [];

    for (const row of rows) {
      const historyRows = await this.database.getAllAsync<HistoryRow>(
        `SELECT retailer, price, checked_at
         FROM price_history
         WHERE inventory_item_id = ?
         ORDER BY checked_at DESC`,
        row.id,
      );
      const { id: _id, ...product } = mapInventoryRow(row);
      products.push({
        ...product,
        priceHistory: historyRows.map((entry) => ({
          retailer: entry.retailer,
          price: entry.price,
          checkedAt: entry.checked_at,
        })),
      });
    }

    return encodeBackupDocument(products);
  }
}

function keySet(values: Array<string | null>): Set<string> {
  return new Set(values.map(normalizeOptionalKey).filter((value): value is string => value != null));
}

function insertProduct(database: SQLiteDatabase, product: ImportedProduct) {
  return database.runAsync(
    `INSERT INTO inventory (
      product_name, barcode, shop_price, purchase_cost, pricebuddy_product_id,
      amazon_url, flipkart_url, image_url, search_count, created_at, updated_at,
      amazon_last_price, amazon_last_checked, flipkart_last_price, flipkart_last_checked
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    product.productName,
    product.barcode,
    product.shopPrice,
    product.purchaseCost,
    product.pricebuddyProductId,
    product.amazonUrl,
    product.flipkartUrl,
    product.imageUrl,
    product.searchCount,
    product.createdAt,
    product.updatedAt,
    product.amazonLastPrice,
    product.amazonLastChecked,
    product.flipkartLastPrice,
    product.flipkartLastChecked,
  );
}

async function insertHistory(
  database: SQLiteDatabase,
  inventoryItemId: number,
  history: PriceObservation[],
): Promise<void> {
  for (const observation of history) {
    await database.runAsync(
      `INSERT OR IGNORE INTO price_history
       (inventory_item_id, retailer, price, checked_at) VALUES (?, ?, ?, ?)`,
      inventoryItemId,
      observation.retailer,
      observation.price,
      observation.checkedAt,
    );
  }
}

function mapInventoryRow(row: InventoryRow): InventoryProduct {
  return {
    id: row.id,
    productName: row.product_name,
    barcode: row.barcode,
    shopPrice: row.shop_price,
    purchaseCost: row.purchase_cost,
    pricebuddyProductId: row.pricebuddy_product_id,
    amazonUrl: row.amazon_url,
    flipkartUrl: row.flipkart_url,
    imageUrl: row.image_url,
    searchCount: row.search_count,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    amazonLastPrice: row.amazon_last_price,
    amazonLastChecked: row.amazon_last_checked,
    flipkartLastPrice: row.flipkart_last_price,
    flipkartLastChecked: row.flipkart_last_checked,
  };
}
