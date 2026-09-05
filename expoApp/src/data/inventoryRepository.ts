import type { SQLiteDatabase } from 'expo-sqlite';

import type { ComparisonSort } from '../domain/comparison';
import { summarizeProductComparison } from '../domain/comparison';
import type { BackupImportResult, ImportedProduct, InventoryProduct, PriceObservation, PriceHistoryEntry, PriceRetailer } from '../domain/models';
import { fetchRetailerPrice } from '../network/retailerPriceClient';
import type { ValidatedInventoryInput } from '../domain/inventoryValidation';
import {
  encodeBackupDocument,
  normalizeNameKey,
  normalizeOptionalKey,
  parseBackupJson,
} from './backup';
import { getDatabase, withWriteTransaction } from './database';
import { isValidProductImageUrl, normalizeMediumQualityImageUrl } from '../network/pricePageParser';

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

export interface ShopPricePoint {
  price: number;
  checkedAt: number;
}

export interface ShopPriceChange {
  productId: number;
  retailer: 'AMAZON' | 'FLIPKART';
  oldPrice: number;
  newPrice: number;
  checkedAt: number;
  direction: 'HIGHER' | 'LOWER';
  percentage: number;
}

export interface ShopProductMovement {
  item: InventoryProduct;
  amazonHistory: ShopPricePoint[];
  flipkartHistory: ShopPricePoint[];
  changes: ShopPriceChange[];
}

export interface ShopPriceMovementSnapshot {
  products: ShopProductMovement[];
  generatedAt: number;
}

export interface PriorityProductSummary {
  id: number;
  productName: string;
  shopPrice: number;
  onlinePrice: number;
  gap: number;
  purchaseCost: number | null;
  onlineRetailer: 'Amazon' | 'Flipkart';
  marginRisk: boolean;
  item: InventoryProduct;
}

export class InventoryRepository {
  private constructor(private readonly database: SQLiteDatabase) {}

  static async create(): Promise<InventoryRepository> {
    return new InventoryRepository(await getDatabase());
  }

  async getPriceMovementSnapshot(days: number): Promise<ShopPriceMovementSnapshot> {
    const cutoff = Date.now() - days * 24 * 60 * 60 * 1000;
    const inventoryRows = await this.database.getAllAsync<InventoryRow>('SELECT * FROM inventory');
    const items = inventoryRows.map(mapInventoryRow);

    const historyRows = await this.database.getAllAsync<HistoryRow & { inventory_item_id: number }>(
      'SELECT inventory_item_id, retailer, price, checked_at FROM price_history WHERE checked_at >= ? ORDER BY checked_at ASC',
      cutoff
    );

    const pointsByKey = new Map<string, ShopPricePoint[]>();
    for (const row of historyRows) {
      const key = `${row.inventory_item_id}-${row.retailer}`;
      if (!pointsByKey.has(key)) pointsByKey.set(key, []);
      pointsByKey.get(key)!.push({ price: row.price, checkedAt: row.checked_at });
    }

    const buildChanges = (productId: number, retailer: 'AMAZON' | 'FLIPKART', points: ShopPricePoint[]) => {
      const changes: ShopPriceChange[] = [];
      for (let i = 1; i < points.length; i++) {
        const oldPoint = points[i - 1];
        const newPoint = points[i];
        if (oldPoint.price !== newPoint.price) {
          changes.push({
            productId,
            retailer,
            oldPrice: oldPoint.price,
            newPrice: newPoint.price,
            checkedAt: newPoint.checkedAt,
            direction: newPoint.price < oldPoint.price ? 'LOWER' : 'HIGHER',
            percentage: oldPoint.price > 0 ? (Math.abs(newPoint.price - oldPoint.price) / oldPoint.price) * 100 : 0
          });
        }
      }
      return changes;
    };

    const products: ShopProductMovement[] = [];
    for (const item of items) {
      const amazonHistory = pointsByKey.get(`${item.id}-AMAZON`) || [];
      const flipkartHistory = pointsByKey.get(`${item.id}-FLIPKART`) || [];
      
      const changes = [
        ...buildChanges(item.id, 'AMAZON', amazonHistory),
        ...buildChanges(item.id, 'FLIPKART', flipkartHistory)
      ].sort((a, b) => b.checkedAt - a.checkedAt);

      if (amazonHistory.length > 0 || flipkartHistory.length > 0) {
        products.push({
          item,
          amazonHistory,
          flipkartHistory,
          changes
        });
      }
    }

    return {
      products,
      generatedAt: Date.now()
    };
  }

  async getTopPriorityProducts(
    limit = 3,
    sortMode: 'RUPEE_GAP' | 'PERCENTAGE_GAP' = 'RUPEE_GAP'
  ): Promise<PriorityProductSummary[]> {
    const rows = await this.database.getAllAsync<InventoryRow>(
      `SELECT * FROM inventory WHERE (amazon_last_price IS NOT NULL AND amazon_last_price < shop_price) OR (flipkart_last_price IS NOT NULL AND flipkart_last_price < shop_price)`
    );
    const summaries: PriorityProductSummary[] = [];
    for (const row of rows) {
      const item = mapInventoryRow(row);
      const azPrice = item.amazonLastPrice;
      const fkPrice = item.flipkartLastPrice;
      let onlinePrice: number | null = null;
      let onlineRetailer: 'Amazon' | 'Flipkart' = 'Amazon';
      if (azPrice != null && fkPrice != null) {
        if (azPrice <= fkPrice) {
          onlinePrice = azPrice;
          onlineRetailer = 'Amazon';
        } else {
          onlinePrice = fkPrice;
          onlineRetailer = 'Flipkart';
        }
      } else if (azPrice != null) {
        onlinePrice = azPrice;
        onlineRetailer = 'Amazon';
      } else if (fkPrice != null) {
        onlinePrice = fkPrice;
        onlineRetailer = 'Flipkart';
      }
      if (onlinePrice != null && onlinePrice < item.shopPrice) {
        const gap = item.shopPrice - onlinePrice;
        const marginRisk = item.purchaseCost != null && onlinePrice <= item.purchaseCost + 0.01;
        summaries.push({
          id: item.id,
          productName: item.productName,
          shopPrice: item.shopPrice,
          onlinePrice,
          gap,
          purchaseCost: item.purchaseCost,
          onlineRetailer,
          marginRisk,
          item,
        });
      }
    }
    return summaries
      .sort((a, b) => {
        if (sortMode === 'PERCENTAGE_GAP') {
          const aPct = a.gap / (a.shopPrice || 1);
          const bPct = b.gap / (b.shopPrice || 1);
          return bPct - aPct;
        }
        return b.gap - a.gap;
      })
      .slice(0, limit);
  }

  async getMetadata(key: string): Promise<string | null> {
    try {
      await this.database.execAsync(
        'CREATE TABLE IF NOT EXISTS app_metadata (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)'
      );
      const row = await this.database.getFirstAsync<{ value: string }>(
        'SELECT value FROM app_metadata WHERE key = ?',
        key,
      );
      return row?.value ?? null;
    } catch {
      return null;
    }
  }

  async setMetadata(key: string, value: string): Promise<void> {
    try {
      await this.database.execAsync(
        'CREATE TABLE IF NOT EXISTS app_metadata (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)'
      );
      await this.database.runAsync(
        'INSERT OR REPLACE INTO app_metadata (key, value) VALUES (?, ?)',
        key,
        value,
      );
    } catch {}
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
         SET ${priceColumn} = ?,
             ${checkedColumn} = ?,
             image_url = CASE
               WHEN image_url IS NOT NULL AND image_url != '' AND image_url NOT LIKE '%/images/G/%' AND lower(image_url) NOT LIKE '%prime%' AND lower(image_url) NOT LIKE '%badge%' THEN image_url
               WHEN ? IS NOT NULL THEN ?
               WHEN image_url LIKE '%/images/G/%' OR lower(image_url) LIKE '%prime%' OR lower(image_url) LIKE '%badge%' THEN NULL
               ELSE image_url
             END
         WHERE id = ?`,
        price,
        checkedAt,
        safeImage,
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

  async getPriceHistory(productId: number): Promise<PriceHistoryEntry[]> {
    const rows = await this.database.getAllAsync<{
      id: number;
      inventory_item_id: number;
      retailer: PriceRetailer;
      price: number;
      checked_at: number;
    }>(
      `SELECT id, inventory_item_id, retailer, price, checked_at
       FROM price_history
       WHERE inventory_item_id = ?
       ORDER BY checked_at ASC, id ASC`,
      productId,
    );
    return rows.map((r) => ({
      id: r.id,
      inventoryItemId: r.inventory_item_id,
      retailer: r.retailer,
      price: r.price,
      checkedAt: r.checked_at,
    }));
  }

  async refreshProductPrices(productId: number): Promise<{ success: boolean; message?: string }> {
    const product = await this.getProduct(productId);
    if (!product) {
      return { success: false, message: 'Product not found.' };
    }
    const linkedRetailers: Array<{ retailer: PriceRetailer; url: string }> = [];
    if (product.amazonUrl) linkedRetailers.push({ retailer: 'AMAZON', url: product.amazonUrl });
    if (product.flipkartUrl) linkedRetailers.push({ retailer: 'FLIPKART', url: product.flipkartUrl });
    if (linkedRetailers.length === 0) {
      return { success: false, message: 'No retailer links configured for this product.' };
    }

    let successCount = 0;
    const errors: string[] = [];
    for (const { retailer, url } of linkedRetailers) {
      try {
        const result = await fetchRetailerPrice(url, retailer, undefined, {
          skipImage: Boolean(product.imageUrl),
        });
        if (result.ok) {
          await this.recordRetailerPrice(productId, retailer, result.price, result.checkedAt, result.image);
          successCount++;
        } else {
          errors.push(`${retailer}: ${result.message}`);
        }
      } catch (err) {
        errors.push(`${retailer}: ${err instanceof Error ? err.message : 'Check failed'}`);
      }
    }

    if (successCount > 0) {
      return { success: true };
    }
    return { success: false, message: errors.join('; ') || 'Failed to refresh prices.' };
  }

  async refreshPrices(productId: number): Promise<{ success: boolean; message?: string }> {
    return this.refreshProductPrices(productId);
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
    imageUrl: normalizeRemoteImage(row.image_url),
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
    const result = parsed.toString();
    if (!isValidProductImageUrl(result)) return null;
    const medium = normalizeMediumQualityImageUrl(result);
    return medium && isValidProductImageUrl(medium) ? medium : result;
  } catch {
    return null;
  }
}
