import { afterEach, describe, expect, it, vi } from 'vitest';

vi.mock('react-native', () => ({
  Platform: { OS: 'android' },
}));

import { fetchRetailerPrice } from './retailerPriceClient';
import {
  buildCanonicalProductUrl,
  buildRetailerRequestUrl,
  extractAmazonAsin,
  extractFlipkartItmId,
} from './retailerRequestStrategy';

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

  it('extracts Flipkart item IDs and generates canonical product URLs', () => {
    expect(extractFlipkartItmId('https://www.flipkart.com/apple-iphone-15/p/itm12345678abcdef?pid=MOB123')).toBe('itm12345678abcdef');
    expect(buildCanonicalProductUrl('https://www.flipkart.com/apple-iphone-15/p/itm12345678abcdef?pid=MOB123', 'FLIPKART')).toBe('https://www.flipkart.com/apple-iphone-15/p/itm12345678abcdef?pid=MOB123');
    expect(buildCanonicalProductUrl('https://www.flipkart.com/p/itm12345678abcdef', 'FLIPKART')).toBe('https://www.flipkart.com/product/p/itm12345678abcdef');
    expect(buildCanonicalProductUrl('https://www.amazon.in/Prestige-Cooker/dp/B0ABC12345/ref=sr_1_1', 'AMAZON')).toBe('https://www.amazon.in/dp/B0ABC12345');
    expect(buildCanonicalProductUrl('https://www.flipkart.com/search?q=phone', 'FLIPKART')).toBeNull();
  });

  it('leaves supported links unchanged when a safe canonical form is unavailable', () => {
    expect(buildRetailerRequestUrl('https://amzn.in/d/example', 'AMAZON')).toBe('https://amzn.in/d/example');
    expect(buildRetailerRequestUrl('https://www.flipkart.com/item/p/itm123', 'FLIPKART')).toBe('https://www.flipkart.com/item/p/itm123');
    expect(extractAmazonAsin('not a link')).toBeNull();
  });

  it('returns an Amazon timeout at the ten-second deadline', async () => {
    vi.useFakeTimers();
    globalThis.fetch = vi.fn(() => new Promise<Response>(() => undefined));

    const pending = fetchRetailerPrice(
      'https://www.amazon.in/dp/B0ABC12345',
      'AMAZON',
    );
    await vi.advanceTimersByTimeAsync(10_000);

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

  it('terminates stream early when price and image are discovered in initial chunks', async () => {
    let cancelCalled = false;
    let readCount = 0;
    const encoder = new TextEncoder();
    const mockBody = {
      getReader: () => ({
        read: async () => {
          readCount++;
          if (readCount === 1) {
            // First chunk with price and main image (padded to 65KB to trigger early check)
            const chunk = '<div id="corePrice_desktop"><span class="a-price-whole">1,885</span></div><img id="landingImage" src="https://m.media-amazon.com/images/I/cooker.jpg" />' + ' '.repeat(65_000);
            return { done: false, value: encoder.encode(chunk) };
          }
          // Subsequent chunks should never be read because reader is cancelled
          return { done: false, value: encoder.encode('trailing data '.repeat(10_000)) };
        },
        cancel: async () => {
          cancelCalled = true;
        },
      }),
    };

    globalThis.fetch = vi.fn(async () => ({
      ok: true,
      status: 200,
      url: 'https://www.amazon.in/dp/B0DVH34776',
      headers: { get: () => null },
      body: mockBody,
    } as unknown as Response));

    const result = await fetchRetailerPrice('https://www.amazon.in/dp/B0DVH34776', 'AMAZON');
    expect(result).toMatchObject({ ok: true, price: 1885, image: 'https://m.media-amazon.com/images/I/cooker._SL500_.jpg' });
    expect(cancelCalled).toBe(true);
    expect(readCount).toBe(1);
  });

  it('terminates stream early for Flipkart when price and image are discovered', async () => {
    let cancelCalled = false;
    let readCount = 0;
    const encoder = new TextEncoder();
    const mockBody = {
      getReader: () => ({
        read: async () => {
          readCount++;
          if (readCount === 1) {
            // First chunk contains embedded finalPrice and og:image, padded past 65KB
            const chunk = '<meta property="og:image" content="https://rukminim2.flixcart.com/image/1500/1500/xif0q/pot/cooker.jpg"><script>var state = {"finalPrice": 1499};</script>' + ' '.repeat(65_000);
            return { done: false, value: encoder.encode(chunk) };
          }
          return { done: false, value: encoder.encode('trailing data '.repeat(10_000)) };
        },
        cancel: async () => {
          cancelCalled = true;
        },
      }),
    };

    globalThis.fetch = vi.fn(async () => ({
      ok: true,
      status: 200,
      url: 'https://www.flipkart.com/cooker/p/itm12345',
      headers: { get: () => null },
      body: mockBody,
    } as unknown as Response));

    const result = await fetchRetailerPrice('https://www.flipkart.com/cooker/p/itm12345', 'FLIPKART');
    expect(result).toMatchObject({ ok: true, price: 1499, image: 'https://rukminim2.flixcart.com/image/500/500/xif0q/pot/cooker.jpg' });
    expect(cancelCalled).toBe(true);
    expect(readCount).toBe(1);
  });

  it('caches resolved Amazon ASIN from redirected short links and uses canonical URL on next check', async () => {
    let callCount = 0;
    const urlsCalled: string[] = [];

    globalThis.fetch = vi.fn(async (input: RequestInfo | URL) => {
      callCount++;
      const url = typeof input === 'string' ? input : input.toString();
      urlsCalled.push(url);
      return {
        ok: true,
        status: 200,
        url: 'https://www.amazon.in/dp/B0DVH34776?ref=share',
        headers: { get: () => null },
        text: async () => '<span class="a-price-whole">1,885</span>',
      } as unknown as Response;
    });

    const shortUrl = 'https://amzn.in/d/0cGVfXg9';
    // First call: requests raw short link, resolves and caches B0DVH34776
    const res1 = await fetchRetailerPrice(shortUrl, 'AMAZON');
    expect(res1.ok).toBe(true);
    expect(urlsCalled[0]).toBe('https://amzn.in/d/0cGVfXg9');

    // Second call: requests canonical ASIN directly without redirection!
    const res2 = await fetchRetailerPrice(shortUrl, 'AMAZON');
    expect(res2.ok).toBe(true);
    expect(urlsCalled[1]).toBe('https://www.amazon.in/dp/B0DVH34776');
  });
});
