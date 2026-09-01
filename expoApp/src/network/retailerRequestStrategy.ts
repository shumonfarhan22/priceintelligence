import type { PriceRetailer } from '../domain/models';

/**
 * Removes referral redirects and tracking parameters before a live check. A
 * canonical Amazon product URL plus a stable mobile user agent normally returns
 * a much smaller document than a copied shopping/share URL.
 */
export function buildRetailerRequestUrl(url: string, retailer: PriceRetailer): string {
  if (retailer !== 'AMAZON') return url;
  const asin = extractAmazonAsin(url);
  return asin ? `https://www.amazon.in/dp/${asin}` : url;
}

export function extractAmazonAsin(url: string): string | null {
  try {
    const parsed = new URL(url);
    const pathMatch = parsed.pathname.match(/\/(?:dp|gp\/product|gp\/aw\/d)\/([a-z0-9]{10})(?:[/?]|$)/i);
    const candidate = pathMatch?.[1] ?? parsed.searchParams.get('asin');
    return candidate && /^[a-z0-9]{10}$/i.test(candidate) ? candidate.toUpperCase() : null;
  } catch {
    return null;
  }
}
