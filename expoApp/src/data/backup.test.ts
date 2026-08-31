import { describe, expect, it } from 'vitest';

import { CURRENT_BACKUP_FORMAT, parseBackupJson } from './backup';

describe('Price Intelligence backup compatibility', () => {
  it('accepts the current format and keeps only supported secure retailer links', () => {
    const parsed = parseBackupJson(JSON.stringify({
      formatVersion: CURRENT_BACKUP_FORMAT,
      exportedAt: 1_700_000_000_000,
      products: [{
        productName: ' Prestige Cooker ',
        barcode: '12345',
        shopPrice: 3841,
        purchaseCost: 3200,
        amazonUrl: 'http://www.amazon.in/dp/example',
        flipkartUrl: 'https://evil.example/item',
        imageUrl: 'http://images.example/cooker.jpg',
        priceHistory: [
          { retailer: 'AMAZON', price: 3791, checkedAt: 1_700_000_000_001 },
          { retailer: 'UNKNOWN', price: 1, checkedAt: 1_700_000_000_002 },
        ],
      }],
    }), 1_800_000_000_000);

    expect(parsed.invalidCount).toBe(0);
    expect(parsed.products).toHaveLength(1);
    expect(parsed.products[0]).toMatchObject({
      productName: 'Prestige Cooker',
      amazonUrl: 'https://www.amazon.in/dp/example',
      flipkartUrl: null,
      imageUrl: 'https://images.example/cooker.jpg',
    });
    expect(parsed.products[0].priceHistory).toEqual([
      { retailer: 'AMAZON', price: 3791, checkedAt: 1_700_000_000_001 },
    ]);
  });

  it('restores legacy version 1 last prices as history', () => {
    const parsed = parseBackupJson(JSON.stringify({
      formatVersion: 1,
      exportedAt: 1_700_000_000_000,
      products: [{
        productName: 'Hawkins Pan',
        shopPrice: 2100,
        amazonLastPrice: 2050,
        amazonLastChecked: 1_700_000_000_100,
        flipkartLastPrice: 2080,
        flipkartLastChecked: 1_700_000_000_200,
      }],
    }));

    expect(parsed.products[0].priceHistory).toEqual(expect.arrayContaining([
      { retailer: 'AMAZON', price: 2050, checkedAt: 1_700_000_000_100 },
      { retailer: 'FLIPKART', price: 2080, checkedAt: 1_700_000_000_200 },
    ]));
  });

  it('counts invalid products without discarding valid products', () => {
    const parsed = parseBackupJson(JSON.stringify({
      formatVersion: 2,
      exportedAt: 1,
      products: [
        { productName: '', shopPrice: 100 },
        { productName: 'No price', shopPrice: 0 },
        { productName: 'Valid', shopPrice: 250 },
      ],
    }));

    expect(parsed.invalidCount).toBe(2);
    expect(parsed.products.map((product) => product.productName)).toEqual(['Valid']);
  });

  it('keeps only the newest 60 observations for each retailer', () => {
    const history = Array.from({ length: 75 }, (_, index) => ({
      retailer: 'AMAZON',
      price: 100 + index,
      checkedAt: 1_000 + index,
    }));
    const parsed = parseBackupJson(JSON.stringify({
      formatVersion: 2,
      exportedAt: 1,
      products: [{ productName: 'Bounded', shopPrice: 500, priceHistory: history }],
    }));

    expect(parsed.products[0].priceHistory).toHaveLength(60);
    expect(parsed.products[0].priceHistory[0].checkedAt).toBe(1_074);
    expect(parsed.products[0].priceHistory.at(-1)?.checkedAt).toBe(1_015);
  });

  it('rejects backups from unsupported future versions', () => {
    expect(() => parseBackupJson(JSON.stringify({
      formatVersion: CURRENT_BACKUP_FORMAT + 1,
      exportedAt: 1,
      products: [],
    }))).toThrow('newer version');
  });
});
