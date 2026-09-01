import type { InventoryProduct, PriceRetailer } from './models';

export const PRICE_MATCH_TOLERANCE = 0.01;

export type ComparisonSort = 'MOST_VIEWED' | 'ALPHABETICAL' | 'RECENT' | 'BEST_SAVING';
export type PriceRelationship = 'ONLINE_LOWER' | 'SHOP_LOWER' | 'MATCHED';
export type ProductPosition = 'REVIEW' | 'COMPETITIVE' | 'UNCHECKED';

export interface SavedRetailerPrice {
  retailer: PriceRetailer;
  price: number;
  checkedAt: number | null;
}

export interface ProductComparisonSummary {
  position: ProductPosition;
  bestOnlinePrice: number | null;
  bestRetailer: PriceRetailer | null;
  saving: number;
}

export function comparePrices(shopPrice: number, onlinePrice: number): PriceRelationship {
  const difference = shopPrice - onlinePrice;
  if (difference > PRICE_MATCH_TOLERANCE) return 'ONLINE_LOWER';
  if (difference < -PRICE_MATCH_TOLERANCE) return 'SHOP_LOWER';
  return 'MATCHED';
}

export function savedRetailerPrice(
  product: InventoryProduct,
  retailer: PriceRetailer,
): SavedRetailerPrice | null {
  const price = retailer === 'AMAZON' ? product.amazonLastPrice : product.flipkartLastPrice;
  const checkedAt = retailer === 'AMAZON' ? product.amazonLastChecked : product.flipkartLastChecked;
  if (price == null || !Number.isFinite(price) || price <= 0) return null;
  return { retailer, price, checkedAt };
}

export function summarizeProductComparison(product: InventoryProduct): ProductComparisonSummary {
  const prices = [
    savedRetailerPrice(product, 'AMAZON'),
    savedRetailerPrice(product, 'FLIPKART'),
  ].filter((entry): entry is SavedRetailerPrice => entry != null);

  if (prices.length === 0) {
    return {
      position: 'UNCHECKED',
      bestOnlinePrice: null,
      bestRetailer: null,
      saving: 0,
    };
  }

  const best = prices.reduce((lowest, candidate) => (
    candidate.price < lowest.price ? candidate : lowest
  ));
  const saving = Math.max(0, product.shopPrice - best.price);
  return {
    position: comparePrices(product.shopPrice, best.price) === 'ONLINE_LOWER'
      ? 'REVIEW'
      : 'COMPETITIVE',
    bestOnlinePrice: best.price,
    bestRetailer: best.retailer,
    saving,
  };
}

export function retailerDisplayName(retailer: PriceRetailer): string {
  return retailer === 'AMAZON' ? 'Amazon' : 'Flipkart';
}
