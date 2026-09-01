import { describe, expect, it } from 'vitest';

import { buildRetailerRequestUrl, extractAmazonAsin } from './retailerRequestStrategy';

describe('retailer request URL strategy', () => {
  it('extracts Amazon ASINs from common copied product links', () => {
    expect(extractAmazonAsin('https://www.amazon.in/Prestige-Cooker/dp/B0ABC12345/ref=sr_1_1')).toBe('B0ABC12345');
    expect(extractAmazonAsin('https://amazon.in/gp/product/b09xyz1234?th=1')).toBe('B09XYZ1234');
    expect(extractAmazonAsin('https://www.amazon.in/gp/aw/d/B012345678?psc=1')).toBe('B012345678');
  });

  it('uses a short canonical Amazon URL without referral parameters', () => {
    expect(buildRetailerRequestUrl(
      'https://www.amazon.in/Prestige-Cooker/dp/B0ABC12345/ref=sr_1_1?keywords=cooker',
      'AMAZON',
    )).toBe('https://www.amazon.in/dp/B0ABC12345');
  });

  it('leaves supported links unchanged when a safe canonical form is unavailable', () => {
    expect(buildRetailerRequestUrl('https://amzn.in/d/example', 'AMAZON')).toBe('https://amzn.in/d/example');
    expect(buildRetailerRequestUrl('https://www.flipkart.com/item/p/itm123', 'FLIPKART')).toBe('https://www.flipkart.com/item/p/itm123');
    expect(extractAmazonAsin('not a link')).toBeNull();
  });
});
