import { describe, expect, it } from 'vitest';

import { emptyInventoryDraft, validateInventoryDraft } from './inventoryValidation';

describe('inventory input validation', () => {
  it('normalizes a valid product without changing its price meaning', () => {
    const result = validateInventoryDraft({
      productName: '  Prestige Cooker  ',
      purchaseCost: '3200.50',
      shopPrice: '3841',
      barcode: ' 890123 ',
      amazonUrl: 'http://amazon.in/dp/example',
      flipkartUrl: 'https://www.flipkart.com/item',
    });

    expect(result).toEqual({
      valid: true,
      input: {
        productName: 'Prestige Cooker',
        purchaseCost: 3200.5,
        shopPrice: 3841,
        barcode: '890123',
        amazonUrl: 'https://amazon.in/dp/example',
        flipkartUrl: 'https://www.flipkart.com/item',
      },
    });
  });

  it('identifies the exact missing or invalid field', () => {
    expect(validateInventoryDraft(emptyInventoryDraft)).toMatchObject({
      valid: false,
      field: 'productName',
    });
    expect(validateInventoryDraft({
      ...emptyInventoryDraft,
      productName: 'Cooker',
      shopPrice: '-1',
    })).toMatchObject({ valid: false, field: 'shopPrice' });
    expect(validateInventoryDraft({
      ...emptyInventoryDraft,
      productName: 'Cooker',
      shopPrice: '100',
      amazonUrl: 'https://example.com/not-amazon',
    })).toMatchObject({ valid: false, field: 'amazonUrl' });
  });
});
