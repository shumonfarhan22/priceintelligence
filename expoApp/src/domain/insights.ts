import { InventoryProduct } from './models';

export const PRICE_FRESHNESS_WINDOW_MILLIS = 1000 * 60 * 60 * 24 * 7; // 7 days

export type InsightGroup =
  | 'COMPETITIVE_FRESH'
  | 'COMPETITIVE_DUE'
  | 'REVIEW_FRESH'
  | 'REVIEW_DUE'
  | 'AMAZON_PRESSURE'
  | 'FLIPKART_PRESSURE'
  | 'ONLINE_LOWER'
  | 'NEAR_MATCH'
  | 'SHOP_LOWER'
  | 'NEEDS_CHECK'
  | 'MISSING_LINKS'
  | 'MISSING_PRICES'
  | 'MISSING_COSTS';

export type InsightPosition = 'COMPETITIVE' | 'REVIEW' | 'NO_COMPARISON';

export type PriceGapBand = 'ONLINE_LOWER' | 'NEAR_MATCH' | 'SHOP_LOWER' | 'NO_COMPARISON';

export interface InsightProduct {
  item: InventoryProduct;
  position: InsightPosition;
  gapBand: PriceGapBand;
  needsCheck: boolean;
  amazonAlert: boolean;
  flipkartAlert: boolean;
  amazonFresh: boolean;
  flipkartFresh: boolean;
  missingSavedPrice: boolean;
}

export interface BrandInsight {
  name: string;
  total: number;
  competitive: number;
  review: number;
  unresolved: number;
}

export interface PricingInsightsSnapshot {
  products: InsightProduct[];
  competitiveFresh: number;
  competitiveDue: number;
  reviewFresh: number;
  reviewDue: number;
  noComparison: number;
  amazonAlerts: number;
  flipkartAlerts: number;
  amazonFresh: number;
  flipkartFresh: number;
  onlineLower: number;
  nearMatch: number;
  shopLower: number;
  brands: BrandInsight[];
  needsCheck: number;
  missingLinks: number;
  missingPrices: number;
  missingCosts: number;
}

function retailerPriceIsFresh(
  url: string | null | undefined,
  price: number | null | undefined,
  checkedAt: number | null | undefined,
  nowMillis: number,
): boolean {
  if (!url || !url.trim()) return false;
  if (price === null || price === undefined || !Number.isFinite(price) || price <= 0) return false;
  if (!checkedAt || checkedAt <= 0) return false;
  return nowMillis - checkedAt < PRICE_FRESHNESS_WINDOW_MILLIS;
}

export function insightBrand(item: InventoryProduct): string {
  const firstWord = item.productName.trim().split(' ')[0];
  if (!firstWord) return 'Other';
  return firstWord.toLowerCase().replace(/^./, (c) => c.toUpperCase());
}

export function productNeedsPriceCheck(item: InventoryProduct, nowMillis: number): boolean {
  const needsAmazon = !!item.amazonUrl?.trim() && (!item.amazonLastChecked || nowMillis - item.amazonLastChecked >= PRICE_FRESHNESS_WINDOW_MILLIS);
  const needsFlipkart = !!item.flipkartUrl?.trim() && (!item.flipkartLastChecked || nowMillis - item.flipkartLastChecked >= PRICE_FRESHNESS_WINDOW_MILLIS);
  return needsAmazon || needsFlipkart;
}

export function buildPricingInsightsSnapshot(items: InventoryProduct[], nowMillis: number): PricingInsightsSnapshot {
  const products: InsightProduct[] = items.map((item) => {
    let onlineLowest: number | null = null;
    const amazon = (item.amazonLastPrice ?? 0) > 0 ? item.amazonLastPrice : null;
    const flipkart = (item.flipkartLastPrice ?? 0) > 0 ? item.flipkartLastPrice : null;

    if (amazon && flipkart) onlineLowest = Math.min(amazon, flipkart);
    else if (amazon) onlineLowest = amazon;
    else if (flipkart) onlineLowest = flipkart;

    let gapPercent: number | null = null;
    if (onlineLowest && item.shopPrice > 0) {
      gapPercent = (Math.abs(item.shopPrice - onlineLowest) / item.shopPrice) * 100;
    }

    let position: InsightPosition;
    if (!onlineLowest) position = 'NO_COMPARISON';
    else if (item.shopPrice > onlineLowest) position = 'REVIEW';
    else position = 'COMPETITIVE';

    let gapBand: PriceGapBand;
    if (position === 'REVIEW') gapBand = 'ONLINE_LOWER';
    else if (gapPercent !== null && gapPercent <= 5.0) gapBand = 'NEAR_MATCH';
    else if (position === 'COMPETITIVE') gapBand = 'SHOP_LOWER';
    else gapBand = 'NO_COMPARISON';

    return {
      item,
      position,
      gapBand,
      needsCheck: productNeedsPriceCheck(item, nowMillis),
      amazonAlert: !!amazon && item.shopPrice > amazon,
      flipkartAlert: !!flipkart && item.shopPrice > flipkart,
      amazonFresh: retailerPriceIsFresh(item.amazonUrl, item.amazonLastPrice, item.amazonLastChecked, nowMillis),
      flipkartFresh: retailerPriceIsFresh(item.flipkartUrl, item.flipkartLastPrice, item.flipkartLastChecked, nowMillis),
      missingSavedPrice:
        (!!item.amazonUrl?.trim() && !amazon) || (!!item.flipkartUrl?.trim() && !flipkart),
    };
  });

  const brandsMap = new Map<string, InsightProduct[]>();
  for (const p of products) {
    const brand = insightBrand(p.item);
    const existing = brandsMap.get(brand) || [];
    existing.push(p);
    brandsMap.set(brand, existing);
  }

  const brands: BrandInsight[] = Array.from(brandsMap.entries())
    .map(([name, brandProducts]) => ({
      name,
      total: brandProducts.length,
      competitive: brandProducts.filter((p) => p.position === 'COMPETITIVE').length,
      review: brandProducts.filter((p) => p.position === 'REVIEW').length,
      unresolved: brandProducts.filter((p) => p.position === 'NO_COMPARISON').length,
    }))
    .sort((a, b) => b.review - a.review || b.unresolved - a.unresolved || b.total - a.total || a.name.localeCompare(b.name));

  return {
    products,
    competitiveFresh: products.filter((p) => p.position === 'COMPETITIVE' && !p.needsCheck).length,
    competitiveDue: products.filter((p) => p.position === 'COMPETITIVE' && p.needsCheck).length,
    reviewFresh: products.filter((p) => p.position === 'REVIEW' && !p.needsCheck).length,
    reviewDue: products.filter((p) => p.position === 'REVIEW' && p.needsCheck).length,
    noComparison: products.filter((p) => p.position === 'NO_COMPARISON').length,
    amazonAlerts: products.filter((p) => p.amazonAlert).length,
    flipkartAlerts: products.filter((p) => p.flipkartAlert).length,
    amazonFresh: products.filter((p) => p.amazonFresh).length,
    flipkartFresh: products.filter((p) => p.flipkartFresh).length,
    onlineLower: products.filter((p) => p.gapBand === 'ONLINE_LOWER').length,
    nearMatch: products.filter((p) => p.gapBand === 'NEAR_MATCH').length,
    shopLower: products.filter((p) => p.gapBand === 'SHOP_LOWER').length,
    brands,
    needsCheck: products.filter((p) => p.needsCheck).length,
    missingLinks: products.filter((p) => !p.item.amazonUrl?.trim() && !p.item.flipkartUrl?.trim()).length,
    missingPrices: products.filter((p) => p.missingSavedPrice).length,
    missingCosts: products.filter((p) => !p.item.purchaseCost || p.item.purchaseCost <= 0).length,
  };
}

export function productMatchesGroup(product: InsightProduct, group: InsightGroup): boolean {
  switch (group) {
    case 'COMPETITIVE_FRESH': return product.position === 'COMPETITIVE' && !product.needsCheck;
    case 'COMPETITIVE_DUE': return product.position === 'COMPETITIVE' && product.needsCheck;
    case 'REVIEW_FRESH': return product.position === 'REVIEW' && !product.needsCheck;
    case 'REVIEW_DUE': return product.position === 'REVIEW' && product.needsCheck;
    case 'AMAZON_PRESSURE': return product.amazonAlert;
    case 'FLIPKART_PRESSURE': return product.flipkartAlert;
    case 'ONLINE_LOWER': return product.gapBand === 'ONLINE_LOWER';
    case 'NEAR_MATCH': return product.gapBand === 'NEAR_MATCH';
    case 'SHOP_LOWER': return product.gapBand === 'SHOP_LOWER';
    case 'NEEDS_CHECK': return product.needsCheck;
    case 'MISSING_LINKS': return !product.item.amazonUrl?.trim() && !product.item.flipkartUrl?.trim();
    case 'MISSING_PRICES': return product.missingSavedPrice;
    case 'MISSING_COSTS': return !product.item.purchaseCost || product.item.purchaseCost <= 0;
    default: return false;
  }
}

export function getGroupTitle(group: InsightGroup | null): string {
  switch (group) {
    case 'COMPETITIVE_FRESH': return 'Competitive and fresh';
    case 'COMPETITIVE_DUE': return 'Competitive but due';
    case 'REVIEW_FRESH': return 'Review with fresh prices';
    case 'REVIEW_DUE': return 'Review and check due';
    case 'AMAZON_PRESSURE': return 'Amazon price alerts';
    case 'FLIPKART_PRESSURE': return 'Flipkart price alerts';
    case 'ONLINE_LOWER': return 'Online price is lower';
    case 'NEAR_MATCH': return 'Prices within 5%';
    case 'SHOP_LOWER': return 'Shop price is lower';
    case 'NEEDS_CHECK': return 'Prices needing a check';
    case 'MISSING_LINKS': return 'Missing retailer links';
    case 'MISSING_PRICES': return 'Missing saved prices';
    case 'MISSING_COSTS': return 'Missing purchase costs';
    default: return 'Products';
  }
}

export function getProductBasicReason(product: InsightProduct): string {
  const positionText =
    product.position === 'COMPETITIVE' ? 'Shop price is competitive' :
    product.position === 'REVIEW' ? 'Online price is lower' :
    'No usable online comparison';
  return product.needsCheck ? `${positionText} • Price check due` : `${positionText} • Price is fresh`;
}
