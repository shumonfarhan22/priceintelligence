import type { PriceRetailer } from '../domain/models';

const amazonAsinCache = new Map<string, string>();

export function recordResolvedAmazonAsin(rawUrl: string, asin: string): void {
  if (rawUrl && asin && /^[a-z0-9]{10}$/i.test(asin)) {
    amazonAsinCache.set(rawUrl, asin.toUpperCase());
  }
}

export function getCachedAmazonAsin(rawUrl: string): string | null {
  return amazonAsinCache.get(rawUrl) ?? null;
}

export function clearAmazonAsinCache(): void {
  amazonAsinCache.clear();
}

/**
 * Removes referral redirects and tracking parameters before a live check. A
 * canonical Amazon product URL plus a stable mobile user agent normally returns
 * a much smaller document than a copied shopping/share URL.
 */
export function buildRetailerRequestUrl(url: string, retailer: PriceRetailer): string {
  if (retailer === 'AMAZON') {
    const asin = extractAmazonAsin(url) ?? amazonAsinCache.get(url);
    return asin ? `https://www.amazon.in/dp/${asin}` : url;
  }
  if (retailer === 'FLIPKART') {
    const canonical = buildCanonicalProductUrl(url, 'FLIPKART');
    if (canonical) return canonical;
    try {
      const parsed = new URL(url);
      if (parsed.hostname.includes('flipkart')) {
        parsed.hostname = 'www.flipkart.com';
        return parsed.toString();
      }
    } catch {
      // return url as fallback
    }
  }
  return url;
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

export function extractFlipkartItmId(url: string): string | null {
  try {
    const parsed = new URL(url);
    const match = parsed.pathname.match(/\/p\/(itm[a-z0-9]+)/i);
    return match ? match[1] : null;
  } catch {
    return null;
  }
}

export function extractFlipkartProductDetails(url: string): {
  slug: string | null;
  itm: string | null;
  pid: string | null;
} {
  try {
    const parsed = new URL(url);
    const pid = parsed.searchParams.get('pid') || null;
    const match = parsed.pathname.match(/(?:\/([^/]+))?\/p\/(itm[a-z0-9]+)/i);
    if (!match) return { slug: null, itm: null, pid };
    const rawSlug = match[1];
    const slug = rawSlug && rawSlug.toLowerCase() !== 'p' ? rawSlug : null;
    const itm = match[2];
    return { slug, itm, pid };
  } catch {
    return { slug: null, itm: null, pid: null };
  }
}

export function buildCanonicalProductUrl(url: string, retailer: PriceRetailer): string | null {
  if (retailer === 'AMAZON') {
    const asin = extractAmazonAsin(url);
    return asin ? `https://www.amazon.in/dp/${asin}` : null;
  }
  if (retailer === 'FLIPKART') {
    const { slug, itm, pid } = extractFlipkartProductDetails(url);
    if (!itm) return null;
    // Flipkart routing requires a path prefix before /p/ (e.g. /slug/p/itm... or /product/p/itm...)
    // requesting bare /p/itm... returns HTTP 404.
    const resolvedSlug = slug || 'product';
    let canonical = `https://www.flipkart.com/${resolvedSlug}/p/${itm}`;
    if (pid) {
      canonical += `?pid=${pid}`;
    }
    return canonical;
  }
  return null;
}

