import { describe, expect, it } from 'vitest';

import type { InventoryProduct } from './models';
import { comparePrices, summarizeProductComparison } from './comparison';

describe('price comparison', () => {
  it('uses a one-paise tolerance for matched prices', () => {
    expect(comparePrices(100, 99.995)).toBe('MATCHED');
    expect(comparePrices(100, 99.98)).toBe('ONLINE_LOWER');
    expect(comparePrices(100, 100.02)).toBe('SHOP_LOWER');
  });

  it('selects the lowest saved retailer price and labels review clearly', () => {
    const summary = summarizeProductComparison(product({
      shopPrice: 3_841,
      amazonLastPrice: 3_791,
      flipkartLastPrice: 3_820,
    }));

    expect(summary).toEqual({
      position: 'REVIEW',
      bestOnlinePrice: 3_791,
      bestRetailer: 'AMAZON',
      saving: 50,
    });
  });

  it('treats a shop match or lower price as competitive', () => {
    expect(summarizeProductComparison(product({
      shopPrice: 2_500,
      amazonLastPrice: 2_500,
      flipkartLastPrice: 2_650,
    })).position).toBe('COMPETITIVE');
  });

  it('does not invent a comparison when no valid saved price exists', () => {
    const summary = summarizeProductComparison(product({
      amazonLastPrice: null,
      flipkartLastPrice: Number.NaN,
    }));

    expect(summary.position).toBe('UNCHECKED');
    expect(summary.bestOnlinePrice).toBeNull();
  });
});

function product(overrides: Partial<InventoryProduct>): InventoryProduct {
  return {
    id: 1,
    productName: 'Prestige Cooker',
    barcode: null,
    shopPrice: 3_841,
    purchaseCost: null,
    pricebuddyProductId: null,
    amazonUrl: 'https://amazon.in/example',
    flipkartUrl: 'https://flipkart.com/example',
    imageUrl: null,
    searchCount: 0,
    createdAt: 1,
    updatedAt: 1,
    amazonLastPrice: null,
    amazonLastChecked: null,
    flipkartLastPrice: null,
    flipkartLastChecked: null,
    ...overrides,
  };
}
