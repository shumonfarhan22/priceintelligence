import type { ImportedProduct, PriceObservation, PriceRetailer } from '../domain/models';

export const CURRENT_BACKUP_FORMAT = 2;
export const MAX_BACKUP_BYTES = 20 * 1024 * 1024;
const MAX_BACKUP_PRODUCTS = 25_000;
const MAX_HISTORY_PER_RETAILER = 60;

export interface ParsedBackup {
  exportedAt: number;
  products: ImportedProduct[];
  invalidCount: number;
}

interface BackupDocument {
  formatVersion: number;
  exportedAt: number;
  products: unknown[];
}

export function parseBackupJson(contents: string, now = Date.now()): ParsedBackup {
  if (utf8ByteLength(contents) > MAX_BACKUP_BYTES) {
    throw new Error('This backup is too large to import safely.');
  }

  let decoded: unknown;
  try {
    decoded = JSON.parse(contents);
  } catch {
    throw new Error('This is not a valid Price Intelligence backup.');
  }

  const document = readBackupDocument(decoded);
  if (document.formatVersion < 1 || document.formatVersion > CURRENT_BACKUP_FORMAT) {
    throw new Error('This backup was created by a newer version of Price Intelligence.');
  }
  if (document.products.length > MAX_BACKUP_PRODUCTS) {
    throw new Error('This backup contains too many products to import safely.');
  }

  const products: ImportedProduct[] = [];
  let invalidCount = 0;
  for (const rawProduct of document.products) {
    const product = normalizeProduct(rawProduct, now);
    if (product) {
      products.push(product);
    } else {
      invalidCount += 1;
    }
  }

  return {
    exportedAt: positiveInteger(document.exportedAt) ?? now,
    products,
    invalidCount,
  };
}

export function normalizeNameKey(value: string): string {
  return value.trim().toLocaleLowerCase().replace(/\s+/g, ' ');
}

export function normalizeOptionalKey(value: string | null): string | null {
  const normalized = value?.trim().toLocaleLowerCase();
  return normalized ? normalized : null;
}

export function encodeBackupDocument(
  products: Array<Omit<ImportedProduct, 'priceHistory'> & { priceHistory: PriceObservation[] }>,
  exportedAt = Date.now(),
): string {
  return JSON.stringify(
    {
      formatVersion: CURRENT_BACKUP_FORMAT,
      exportedAt,
      products,
    },
    null,
    2,
  );
}

function readBackupDocument(value: unknown): BackupDocument {
  if (!isRecord(value) || !Array.isArray(value.products)) {
    throw new Error('This is not a valid Price Intelligence backup.');
  }

  const formatVersion = finiteNumber(value.formatVersion) ?? CURRENT_BACKUP_FORMAT;
  if (!Number.isInteger(formatVersion)) {
    throw new Error('This is not a valid Price Intelligence backup.');
  }

  return {
    formatVersion,
    exportedAt: finiteNumber(value.exportedAt) ?? 0,
    products: value.products,
  };
}

function normalizeProduct(value: unknown, now: number): ImportedProduct | null {
  if (!isRecord(value)) return null;

  const productName = optionalString(value.productName)?.trim();
  const shopPrice = finiteNumber(value.shopPrice);
  if (!productName || shopPrice == null || shopPrice <= 0) return null;

  const amazonLastPrice = positiveNumber(value.amazonLastPrice);
  const amazonLastChecked = positiveInteger(value.amazonLastChecked);
  const flipkartLastPrice = positiveNumber(value.flipkartLastPrice);
  const flipkartLastChecked = positiveInteger(value.flipkartLastChecked);
  const rawHistory = Array.isArray(value.priceHistory) ? value.priceHistory : [];
  const parsedHistory = rawHistory
    .map(normalizeObservation)
    .filter((entry): entry is PriceObservation => entry != null);
  const priceHistory = parsedHistory.length > 0
    ? boundHistory(parsedHistory)
    : boundHistory([
        legacyObservation('AMAZON', amazonLastPrice, amazonLastChecked),
        legacyObservation('FLIPKART', flipkartLastPrice, flipkartLastChecked),
      ].filter((entry): entry is PriceObservation => entry != null));

  return {
    productName,
    barcode: optionalString(value.barcode)?.trim() || null,
    shopPrice,
    purchaseCost: positiveNumber(value.purchaseCost),
    pricebuddyProductId: positiveInteger(value.pricebuddyProductId),
    amazonUrl: normalizeRetailerUrl(optionalString(value.amazonUrl), 'AMAZON'),
    flipkartUrl: normalizeRetailerUrl(optionalString(value.flipkartUrl), 'FLIPKART'),
    imageUrl: normalizeImageUrl(optionalString(value.imageUrl)),
    searchCount: Math.max(0, Math.trunc(finiteNumber(value.searchCount) ?? 0)),
    createdAt: positiveInteger(value.createdAt) ?? now,
    updatedAt: positiveInteger(value.updatedAt) ?? now,
    amazonLastPrice,
    amazonLastChecked,
    flipkartLastPrice,
    flipkartLastChecked,
    priceHistory,
  };
}

function normalizeObservation(value: unknown): PriceObservation | null {
  if (!isRecord(value)) return null;
  const retailer = value.retailer;
  const price = positiveNumber(value.price);
  const checkedAt = positiveInteger(value.checkedAt);
  if (!isRetailer(retailer) || price == null || checkedAt == null) return null;
  return { retailer, price, checkedAt };
}

function legacyObservation(
  retailer: PriceRetailer,
  price: number | null,
  checkedAt: number | null,
): PriceObservation | null {
  return price != null && checkedAt != null ? { retailer, price, checkedAt } : null;
}

function boundHistory(history: PriceObservation[]): PriceObservation[] {
  const newest = (retailer: PriceRetailer) => history
    .filter((entry) => entry.retailer === retailer)
    .sort((left, right) => right.checkedAt - left.checkedAt)
    .slice(0, MAX_HISTORY_PER_RETAILER);
  return [...newest('AMAZON'), ...newest('FLIPKART')];
}

export function normalizeRetailerUrl(value: string | undefined, retailer: PriceRetailer): string | null {
  const trimmed = value?.trim();
  if (!trimmed) return null;
  const match = /^(https?):\/\/([^/]+)(\/.*)?$/i.exec(trimmed);
  if (!match || match[2].includes('@')) return null;

  const host = match[2].split(':')[0].toLocaleLowerCase().replace(/\.$/, '');
  const domains = retailer === 'AMAZON'
    ? ['amazon.in', 'amazon.com', 'amzn.in', 'amzn.to']
    : ['flipkart.com'];
  if (!domains.some((domain) => host === domain || host.endsWith(`.${domain}`))) return null;

  return match[1].toLocaleLowerCase() === 'http'
    ? `https://${trimmed.slice(7)}`
    : trimmed;
}

function normalizeImageUrl(value: string | undefined): string | null {
  const trimmed = value?.trim();
  if (!trimmed) return null;
  if (/^http:\/\//i.test(trimmed)) return `https://${trimmed.slice(7)}`;
  return /^https:\/\//i.test(trimmed) ? trimmed : null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function finiteNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function positiveNumber(value: unknown): number | null {
  const parsed = finiteNumber(value);
  return parsed != null && parsed > 0 ? parsed : null;
}

function positiveInteger(value: unknown): number | null {
  const parsed = finiteNumber(value);
  return parsed != null && parsed > 0 ? Math.trunc(parsed) : null;
}

function optionalString(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}

function isRetailer(value: unknown): value is PriceRetailer {
  return value === 'AMAZON' || value === 'FLIPKART';
}

function utf8ByteLength(value: string): number {
  let bytes = 0;
  for (const character of value) {
    const codePoint = character.codePointAt(0) ?? 0;
    bytes += codePoint <= 0x7f ? 1 : codePoint <= 0x7ff ? 2 : codePoint <= 0xffff ? 3 : 4;
  }
  return bytes;
}
