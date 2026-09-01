import type { SQLiteDatabase } from 'expo-sqlite';

import type { ComparisonSort } from '../domain/comparison';
import { summarizeProductComparison } from '../domain/comparison';
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

export interface ComparisonPage {
  products: InventoryProduct[];
  total: number;
  page: number;
  totalPages: number;
}

export interface ComparisonOverview {
  productCount: number;
  competitiveCount: number;
  reviewCount: number;
  uncheckedCount: number;
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

  async listComparisonProducts(
    query: string,
    sort: ComparisonSort,
    requestedPage: number,
    pageSize = 10,
  ): Promise<ComparisonPage> {
    const safePageSize = Math.min(50, Math.max(1, Math.trunc(pageSize)));
    const search = comparisonSearch(query);
    const countRow = await this.database.getFirstAsync<{ count: number }>(
      `SELECT COUNT(*) AS count FROM inventory${search.where}`,
      search.params,
    );
    const total = countRow?.count ?? 0;
    const totalPages = Math.max(1, Math.ceil(total / safePageSize));
    const page = Math.min(totalPages, Math.max(1, Math.trunc(requestedPage)));
    const rows = await this.database.getAllAsync<InventoryRow>(
      `SELECT * FROM inventory${search.where}
       ORDER BY ${comparisonOrder(sort)}
       LIMIT ? OFFSET ?`,
      [...search.params, safePageSize, (page - 1) * safePageSize],
    );
    return { products: rows.map(mapInventoryRow), total, page, totalPages };
  }

  async listProductSuggestions(query: string, limit = 5): Promise<InventoryProduct[]> {
    const page = await this.listComparisonProducts(query, 'ALPHABETICAL', 1, limit);
    return page.products;
  }

  async getProduct(id: number): Promise<InventoryProduct | null> {
    const row = await this.database.getFirstAsync<InventoryRow>(
      'SELECT * FROM inventory WHERE id = ? LIMIT 1',
      id,
    );
    return row ? mapInventoryRow(row) : null;
  }

  async incrementProductView(id: number): Promise<void> {
    await this.database.runAsync(
      'UPDATE inventory SET search_count = search_count + 1 WHERE id = ?',
      id,
    );
  }

  async getComparisonOverview(): Promise<ComparisonOverview> {
    const rows = await this.database.getAllAsync<InventoryRow>('SELECT * FROM inventory');
    let competitiveCount = 0;
    let reviewCount = 0;
    let uncheckedCount = 0;
    for (const row of rows) {
      const summary = summarizeProductComparison(mapInventoryRow(row));
      if (summary.position === 'COMPETITIVE') competitiveCount += 1;
      else if (summary.position === 'REVIEW') reviewCount += 1;
      else uncheckedCount += 1;
    }
    return {
      productCount: rows.length,
      competitiveCount,
      reviewCount,
      uncheckedCount,
    };
  }

  async recordRetailerPrice(
    itemId: number,
    retailer: 'AMAZON' | 'FLIPKART',
    price: number,
    checkedAt: number,
    imageUrl: string | null,
  ): Promise<boolean> {
    if (!Number.isSafeInteger(itemId) || itemId <= 0) return false;
    if (!Number.isFinite(price) || price <= 0 || !Number.isSafeInteger(checkedAt) || checkedAt <= 0) {
      throw new Error('The live retailer result was invalid and was not saved.');
    }
    const priceColumn = retailer === 'AMAZON' ? 'amazon_last_price' : 'flipkart_last_price';
    const checkedColumn = retailer === 'AMAZON' ? 'amazon_last_checked' : 'flipkart_last_checked';
    const safeImage = normalizeRemoteImage(imageUrl);
    let saved = false;
    await withWriteTransaction(this.database, async (transaction) => {
      const update = await transaction.runAsync(
        `UPDATE inventory
         SET ${priceColumn} = ?, ${checkedColumn} = ?, image_url = COALESCE(?, image_url)
         WHERE id = ?`,
        price,
        checkedAt,
        safeImage,
        itemId,
      );
      if (update.changes !== 1) return;
      await transaction.runAsync(
        `INSERT OR IGNORE INTO price_history
         (inventory_item_id, retailer, price, checked_at) VALUES (?, ?, ?, ?)`,
        itemId,
        retailer,
        price,
        checkedAt,
      );
      saved = true;
    });
    return saved;
  }

  async listPriceHistory(itemId: number): Promise<PriceObservation[]> {
    const rows = await this.database.getAllAsync<HistoryRow>(
      `SELECT retailer, price, checked_at
       FROM price_history
       WHERE inventory_item_id = ?
       ORDER BY checked_at DESC`,
      itemId,
    );
    return rows.map((row) => ({
      retailer: row.retailer,
      price: row.price,
      checkedAt: row.checked_at,
    }));
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

function comparisonSearch(query: string): { where: string; params: string[] } {
  const trimmed = query.trim();
  if (!trimmed) return { where: '', params: [] };
  if (/^\d+$/.test(trimmed)) {
    return {
      where: ' WHERE lower(COALESCE(barcode, \'\')) = lower(?)',
      params: [trimmed],
    };
  }
  const words = trimmed.split(/\s+/).filter(Boolean).slice(0, 12);
  const nameClause = words.map(() => 'instr(lower(product_name), lower(?)) > 0').join(' AND ');
  return {
    where: ` WHERE ((${nameClause})
      OR instr(lower(COALESCE(barcode, '')), lower(?)) > 0
      OR instr(lower(COALESCE(amazon_url, '')), lower(?)) > 0
      OR instr(lower(COALESCE(flipkart_url, '')), lower(?)) > 0)`,
    params: [...words, trimmed, trimmed, trimmed],
  };
}

function comparisonOrder(sort: ComparisonSort): string {
  switch (sort) {
    case 'ALPHABETICAL':
      return 'product_name COLLATE NOCASE ASC, id ASC';
    case 'RECENT':
      return 'updated_at DESC, product_name COLLATE NOCASE ASC, id ASC';
    case 'BEST_SAVING':
      return `(shop_price - CASE
        WHEN amazon_last_price > 0 AND flipkart_last_price > 0
          THEN CASE WHEN amazon_last_price < flipkart_last_price THEN amazon_last_price ELSE flipkart_last_price END
        WHEN amazon_last_price > 0 THEN amazon_last_price
        WHEN flipkart_last_price > 0 THEN flipkart_last_price
        ELSE shop_price
      END) DESC, product_name COLLATE NOCASE ASC, id ASC`;
    case 'MOST_VIEWED':
    default:
      return 'search_count DESC, updated_at DESC, product_name COLLATE NOCASE ASC, id ASC';
  }
}

function normalizeRemoteImage(value: string | null): string | null {
  const trimmed = value?.trim();
  if (!trimmed) return null;
  try {
    const parsed = new URL(trimmed);
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return null;
    if (parsed.protocol === 'http:') parsed.protocol = 'https:';
    return parsed.toString();
  } catch {
    return null;
  }
}
