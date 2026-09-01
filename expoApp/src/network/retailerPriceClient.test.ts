import { afterEach, describe, expect, it, vi } from 'vitest';

vi.mock('react-native', () => ({
  Platform: { OS: 'android' },
}));

import { fetchRetailerPrice } from './retailerPriceClient';
import { buildRetailerRequestUrl, extractAmazonAsin } from './retailerRequestStrategy';

const originalFetch = globalThis.fetch;

afterEach(() => {
  vi.useRealTimers();
  globalThis.fetch = originalFetch;
});

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

  it('returns an Amazon timeout at the hard eight-second deadline', async () => {
    vi.useFakeTimers();
    globalThis.fetch = vi.fn(() => new Promise<Response>(() => undefined));

    const pending = fetchRetailerPrice(
      'https://www.amazon.in/dp/B0ABC12345',
      'AMAZON',
    );
    await vi.advanceTimersByTimeAsync(8_000);

    await expect(pending).resolves.toMatchObject({
      ok: false,
      code: 'TIMEOUT',
    });
  });

  it('uses the original desktop-page request profile for Amazon', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      url: 'https://www.amazon.in/dp/B0ABC12345',
      headers: { get: () => null },
      text: async () => '<span class="a-price-whole">3,841</span>',
    } as unknown as Response));
    globalThis.fetch = fetchMock;

    await expect(fetchRetailerPrice(
      'https://www.amazon.in/example/dp/B0ABC12345?ref=test',
      'AMAZON',
    )).resolves.toMatchObject({ ok: true, price: 3_841 });

    expect(fetchMock).toHaveBeenCalledWith(
      'https://www.amazon.in/dp/B0ABC12345',
      expect.objectContaining({
        headers: expect.objectContaining({
          'User-Agent': expect.stringContaining('Windows NT 10.0'),
        }),
      }),
    );
  });
});
