import type { SQLiteDatabase } from 'expo-sqlite';

import type { BackupImportResult, ImportedProduct, InventoryProduct, PriceObservation } from '../domain/models';
import type { ValidatedInventoryInput } from '../domain/inventoryValidation';
import {
  encodeBackupDocument,
  normalizeNameKey,
  normalizeOptionalKey,
  parseBackupJson,
} from './backup';
import { getDatabase, withWriteTransaction } from './database';

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

  async listProducts(query = ''): Promise<InventoryProduct[]> {
    const trimmed = query.trim();
    if (!trimmed) {
      const rows = await this.database.getAllAsync<InventoryRow>(
        'SELECT * FROM inventory ORDER BY product_name COLLATE NOCASE, id',
      );
      return rows.map(mapInventoryRow);
    }

    if (/^\d+$/.test(trimmed)) {
      const barcodeRows = await this.database.getAllAsync<InventoryRow>(
        `SELECT * FROM inventory
         WHERE barcode = ? COLLATE NOCASE
         ORDER BY product_name COLLATE NOCASE, id`,
        trimmed,
      );
      if (barcodeRows.length > 0) return barcodeRows.map(mapInventoryRow);
    }

    const words = trimmed.split(/\s+/).filter(Boolean).slice(0, 12);
    const nameClause = words.map(() => 'instr(lower(product_name), lower(?)) > 0').join(' AND ');
    const params: string[] = [
      ...words,
      trimmed,
      trimmed,
      trimmed,
    ];
    const rows = await this.database.getAllAsync<InventoryRow>(
      `SELECT * FROM inventory
       WHERE (${nameClause})
          OR instr(lower(COALESCE(barcode, '')), lower(?)) > 0
          OR instr(lower(COALESCE(amazon_url, '')), lower(?)) > 0
          OR instr(lower(COALESCE(flipkart_url, '')), lower(?)) > 0
       ORDER BY product_name COLLATE NOCASE, id`,
      params,
    );
    return rows.map(mapInventoryRow);
  }

  async saveProduct(input: ValidatedInventoryInput, editingId: number | null): Promise<number> {
    let savedId = editingId ?? 0;
    await withWriteTransaction(this.database, async (transaction) => {
      await assertNoDuplicateIdentifiers(transaction, input, editingId);
      const now = Date.now();
      if (editingId == null) {
        const inserted = await transaction.runAsync(
          `INSERT INTO inventory (
            product_name, barcode, shop_price, purchase_cost, amazon_url, flipkart_url,
            search_count, created_at, updated_at
          ) VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)`,
          input.productName,
          input.barcode,
          input.shopPrice,
          input.purchaseCost,
          input.amazonUrl,
          input.flipkartUrl,
          now,
          now,
        );
        savedId = Number(inserted.lastInsertRowId);
      } else {
        const result = await transaction.runAsync(
          `UPDATE inventory
           SET product_name = ?, barcode = ?, shop_price = ?, purchase_cost = ?,
               amazon_url = ?, flipkart_url = ?, updated_at = ?
           WHERE id = ?`,
          input.productName,
          input.barcode,
          input.shopPrice,
          input.purchaseCost,
          input.amazonUrl,
          input.flipkartUrl,
          now,
          editingId,
        );
        if (result.changes !== 1) throw new Error('This product no longer exists.');
      }
    });
    return savedId;
  }

  async deleteProducts(ids: number[]): Promise<void> {
    const uniqueIds = [...new Set(ids.filter((id) => Number.isSafeInteger(id) && id > 0))];
    if (uniqueIds.length === 0) return;
    const placeholders = uniqueIds.map(() => '?').join(', ');
    await this.database.runAsync(`DELETE FROM inventory WHERE id IN (${placeholders})`, uniqueIds);
  }

  async importBackupJson(contents: string): Promise<BackupImportResult> {
    const parsed = parseBackupJson(contents);
    let addedCount = 0;
    let duplicateCount = 0;

    await withWriteTransaction(this.database, async (transaction) => {
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

export class DuplicateInventoryError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'DuplicateInventoryError';
  }
}

async function assertNoDuplicateIdentifiers(
  database: SQLiteDatabase,
  input: ValidatedInventoryInput,
  editingId: number | null,
): Promise<void> {
  const ignoredId = editingId ?? -1;
  if (input.barcode) {
    const duplicate = await database.getFirstAsync<{ id: number }>(
      'SELECT id FROM inventory WHERE lower(barcode) = lower(?) AND id != ? LIMIT 1',
      input.barcode,
      ignoredId,
    );
    if (duplicate) throw new DuplicateInventoryError('That barcode is already used by another product.');
  }
  if (input.amazonUrl) {
    const duplicate = await database.getFirstAsync<{ id: number }>(
      'SELECT id FROM inventory WHERE lower(amazon_url) = lower(?) AND id != ? LIMIT 1',
      input.amazonUrl,
      ignoredId,
    );
    if (duplicate) throw new DuplicateInventoryError('This Amazon link is already used by another product.');
  }
  if (input.flipkartUrl) {
    const duplicate = await database.getFirstAsync<{ id: number }>(
      'SELECT id FROM inventory WHERE lower(flipkart_url) = lower(?) AND id != ? LIMIT 1',
      input.flipkartUrl,
      ignoredId,
    );
    if (duplicate) throw new DuplicateInventoryError('This Flipkart link is already used by another product.');
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
