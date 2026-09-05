import type { InventoryProduct } from './models';

export const AUTOMATIC_REFRESH_DAY_MILLIS = 24 * 60 * 60 * 1000;
export const INDIA_TIME_OFFSET_MILLIS = 5.5 * 60 * 60 * 1000;

const SMART_REFRESH_PROFILE_VERSION = '1';
const SMART_REFRESH_MAX_PROFILE_RECORDS = 120;
const SMART_REFRESH_MINIMUM_DELAY_MILLIS = 16_000;
const SMART_REFRESH_DELAY_VARIATION_MILLIS = 9_000;

export interface SmartRefreshRecord {
  productId: number;
  consecutiveFailures: number;
  volatilityPoints: number;
  lastOutcomeAt: number;
}

export function automaticRefreshDayKey(nowMillis: number): number {
  return Math.floor((nowMillis + INDIA_TIME_OFFSET_MILLIS) / AUTOMATIC_REFRESH_DAY_MILLIS);
}

export function readAutomaticRefreshAttempts(storedValue: string, nowMillis: number): Set<number> {
  if (!storedValue) return new Set();
  const parts = storedValue.split('|', 2);
  if (parts.length !== 2) return new Set();

  const storedDay = parseInt(parts[0], 10);
  if (isNaN(storedDay) || storedDay !== automaticRefreshDayKey(nowMillis)) {
    return new Set();
  }

  const result = new Set<number>();
  for (const raw of parts[1].split(',')) {
    const id = parseInt(raw.trim(), 10);
    if (!isNaN(id) && id > 0) {
      result.add(id);
    }
  }
  return result;
}

export function writeAutomaticRefreshAttempts(productIds: Set<number>, nowMillis: number): string {
  const safeIds = Array.from(productIds)
    .filter((id) => id > 0)
    .sort((a, b) => a - b)
    .join(',');
  return `${automaticRefreshDayKey(nowMillis)}|${safeIds}`;
}

export function readSmartRefreshProfile(storedValue: string): Map<number, SmartRefreshRecord> {
  const records = new Map<number, SmartRefreshRecord>();
  if (!storedValue) return records;

  const separatorIndex = storedValue.indexOf('|');
  if (separatorIndex <= 0 || storedValue.substring(0, separatorIndex) !== SMART_REFRESH_PROFILE_VERSION) {
    return records;
  }

  const rawRecords = storedValue.substring(separatorIndex + 1).split(';');
  for (const encoded of rawRecords) {
    const parts = encoded.split(',');
    if (parts.length !== 4) continue;

    const productId = parseInt(parts[0], 10);
    const failures = parseInt(parts[1], 10);
    const volatility = parseInt(parts[2], 10);
    const lastOutcomeAt = parseInt(parts[3], 10);

    if (isNaN(productId) || productId <= 0) continue;
    if (isNaN(failures) || isNaN(volatility) || isNaN(lastOutcomeAt)) continue;

    records.set(productId, {
      productId,
      consecutiveFailures: Math.max(0, Math.min(5, failures)),
      volatilityPoints: Math.max(0, Math.min(10, volatility)),
      lastOutcomeAt: Math.max(0, lastOutcomeAt),
    });
  }

  return records;
}

export function writeSmartRefreshProfile(records: Map<number, SmartRefreshRecord>): string {
  const sorted = Array.from(records.values())
    .filter((r) => r.productId > 0)
    .sort((a, b) => b.lastOutcomeAt - a.lastOutcomeAt)
    .slice(0, SMART_REFRESH_MAX_PROFILE_RECORDS)
    .sort((a, b) => a.productId - b.productId);

  const encoded = sorted
    .map(
      (r) =>
        `${r.productId},${Math.max(0, Math.min(5, r.consecutiveFailures))},${Math.max(0, Math.min(10, r.volatilityPoints))},${Math.max(0, r.lastOutcomeAt)}`
    )
    .join(';');

  return `${SMART_REFRESH_PROFILE_VERSION}|${encoded}`;
}

export function updateSmartRefreshOutcome(
  records: Map<number, SmartRefreshRecord>,
  productId: number,
  succeeded: boolean,
  priceMoved: boolean,
  nowMillis: number
): Map<number, SmartRefreshRecord> {
  if (productId <= 0 || nowMillis <= 0) return records;

  const updated = new Map(records);
  const current = updated.get(productId) ?? {
    productId,
    consecutiveFailures: 0,
    volatilityPoints: 0,
    lastOutcomeAt: 0,
  };

  if (succeeded) {
    updated.set(productId, {
      productId,
      consecutiveFailures: 0,
      volatilityPoints: priceMoved
        ? Math.min(10, current.volatilityPoints + 2)
        : Math.max(0, current.volatilityPoints - 1),
      lastOutcomeAt: nowMillis,
    });
  } else {
    updated.set(productId, {
      productId,
      consecutiveFailures: Math.min(5, current.consecutiveFailures + 1),
      volatilityPoints: current.volatilityPoints,
      lastOutcomeAt: nowMillis,
    });
  }

  return updated;
}

export function hasMeaningfulPriceMovement(
  oldPrice: number | null | undefined,
  newPrice: number | null | undefined
): boolean {
  if (
    oldPrice == null ||
    newPrice == null ||
    !Number.isFinite(oldPrice) ||
    !Number.isFinite(newPrice) ||
    oldPrice <= 0 ||
    newPrice <= 0
  ) {
    return false;
  }

  const percentageDifference = Math.abs(newPrice - oldPrice) / Math.max(oldPrice, 1.0);
  return percentageDifference >= 0.01;
}

export function smartRefreshSpacingMillis(productId: number, nowMillis: number): number {
  const dayKey = automaticRefreshDayKey(nowMillis);
  const rawJitter = Math.abs((productId * 37 + dayKey * 17) % (SMART_REFRESH_DELAY_VARIATION_MILLIS + 1));
  return SMART_REFRESH_MINIMUM_DELAY_MILLIS + rawJitter;
}

function hasLinkedRetailer(item: InventoryProduct): boolean {
  return Boolean(
    (item.amazonUrl && item.amazonUrl.trim().length > 0) ||
    (item.flipkartUrl && item.flipkartUrl.trim().length > 0)
  );
}

function linkedRetailerCount(item: InventoryProduct): number {
  let count = 0;
  if (item.amazonUrl && item.amazonUrl.trim().length > 0) count++;
  if (item.flipkartUrl && item.flipkartUrl.trim().length > 0) count++;
  return count;
}

function oldestLinkedRetailerCheck(item: InventoryProduct): number {
  let oldestCheck = Infinity;
  let hasRetailer = false;

  if (item.amazonUrl && item.amazonUrl.trim().length > 0) {
    hasRetailer = true;
    const checkedAt = item.amazonLastChecked;
    if (checkedAt != null && checkedAt > 0) {
      oldestCheck = Math.min(oldestCheck, checkedAt);
    } else {
      return 0;
    }
  }

  if (item.flipkartUrl && item.flipkartUrl.trim().length > 0) {
    hasRetailer = true;
    const checkedAt = item.flipkartLastChecked;
    if (checkedAt != null && checkedAt > 0) {
      oldestCheck = Math.min(oldestCheck, checkedAt);
    } else {
      return 0;
    }
  }

  return hasRetailer && oldestCheck !== Infinity ? oldestCheck : 0;
}

function daysBetween(earlierMillis: number, nowMillis: number): number {
  if (earlierMillis <= 0 || nowMillis <= 0) return 0;
  return Math.max(0, automaticRefreshDayKey(nowMillis) - automaticRefreshDayKey(earlierMillis));
}

function savedOnlineReviewPriority(item: InventoryProduct): number {
  if (!Number.isFinite(item.shopPrice) || item.shopPrice <= 0) return 0;

  const validPrices: number[] = [];
  if (item.amazonLastPrice != null && Number.isFinite(item.amazonLastPrice) && item.amazonLastPrice > 0) {
    validPrices.push(item.amazonLastPrice);
  }
  if (item.flipkartLastPrice != null && Number.isFinite(item.flipkartLastPrice) && item.flipkartLastPrice > 0) {
    validPrices.push(item.flipkartLastPrice);
  }

  if (validPrices.length === 0) return 0;
  const bestOnlinePrice = Math.min(...validPrices);
  const shopDifference = item.shopPrice - bestOnlinePrice;
  if (shopDifference <= 0.01) return 0;

  return Math.min(30, (shopDifference / Math.max(item.shopPrice, 1.0)) * 100);
}

export function smartRefreshPriorityScore(
  item: InventoryProduct,
  record: SmartRefreshRecord | undefined,
  nowMillis: number
): number {
  const oldestCheck = oldestLinkedRetailerCheck(item);
  const missingPriceScore = oldestCheck <= 0 ? 120.0 : 0.0;
  const ageScore = oldestCheck <= 0 ? 0.0 : Math.min(30, daysBetween(oldestCheck, nowMillis)) * 6.0;
  const usageScore = Math.min(50, Math.max(0, item.searchCount)) * 2.0;
  const volatilityScore = Math.min(10, Math.max(0, record?.volatilityPoints ?? 0)) * 10.0;
  const reviewScore = Math.min(30, savedOnlineReviewPriority(item));
  const retailerScore = linkedRetailerCount(item) * 3.0;
  const failurePenalty = Math.min(5, Math.max(0, record?.consecutiveFailures ?? 0)) * 8.0;

  const dayKey = automaticRefreshDayKey(nowMillis);
  const tieBreaker = (Math.abs(item.id * 31 + dayKey) % 10) / 10.0;

  return (
    missingPriceScore +
    ageScore +
    usageScore +
    volatilityScore +
    reviewScore +
    retailerScore -
    failurePenalty +
    tieBreaker
  );
}

export function buildSmartRefreshPlan(
  products: InventoryProduct[],
  profile: Map<number, SmartRefreshRecord>,
  attemptedProductIds: Set<number>,
  nowMillis: number
): InventoryProduct[] {
  if (nowMillis <= 0) return [];

  return products
    .filter((item) => hasLinkedRetailer(item))
    .filter((item) => !attemptedProductIds.has(item.id))
    .sort((a, b) => {
      const scoreA = smartRefreshPriorityScore(a, profile.get(a.id), nowMillis);
      const scoreB = smartRefreshPriorityScore(b, profile.get(b.id), nowMillis);
      if (scoreB !== scoreA) {
        return scoreB - scoreA;
      }
      const nameCompare = a.productName.toLowerCase().localeCompare(b.productName.toLowerCase());
      if (nameCompare !== 0) {
        return nameCompare;
      }
      return a.id - b.id;
    });
}
