export interface ParsedRetailerPage {
  price: number | null;
  image: string | null;
  blocked: boolean;
}

const BLOCK_PAGE_MARKERS = [
  'captcha',
  'robot check',
  'enter the characters you see below',
  'unusual traffic',
  'automated access',
  'access denied',
  'are you a human',
];
const BLOCK_PAGE_MAX_BODY_LENGTH = 600;

export function parseRetailerPage(html: string, url: string): ParsedRetailerPage {
  if (!html.trim()) return emptyResult();
  if (looksLikeBlockPage(html)) return { ...emptyResult(), blocked: true };

  const structured = extractStructuredData(html, url);
  const price = structured.price
    ?? extractRetailerPrice(html, url)
    ?? extractMetaPrice(html);
  const image = structured.image
    ?? extractRetailerImage(html, url)
    ?? normalizeImageUrl(extractMetaImage(html), url);

  return { price, image, blocked: false };
}

function emptyResult(): ParsedRetailerPage {
  return { price: null, image: null, blocked: false };
}

function looksLikeBlockPage(html: string): boolean {
  const title = decodeHtml(stripTags(firstCapture(html, /<title\b[^>]*>([\s\S]*?)<\/title\s*>/i) ?? ''))
    .toLocaleLowerCase();
  if (BLOCK_PAGE_MARKERS.some((marker) => title.includes(marker))) return true;

  const bodyHtml = firstCapture(html, /<body\b[^>]*>([\s\S]*?)<\/body\s*>/i) ?? html;
  const body = decodeHtml(stripTags(bodyHtml)).replace(/\s+/g, ' ').trim().toLocaleLowerCase();
  return body.length <= BLOCK_PAGE_MAX_BODY_LENGTH
    && BLOCK_PAGE_MARKERS.some((marker) => body.includes(marker));
}

function extractStructuredData(
  html: string,
  baseUrl: string,
): { price: number | null; image: string | null } {
  let image: string | null = null;
  let startIndex = 0;
  
  while ((startIndex = html.indexOf('<script', startIndex)) !== -1) {
    const endBracket = html.indexOf('>', startIndex);
    if (endBracket === -1) break;
    
    const tagContent = html.substring(startIndex, endBracket + 1);
    if (!tagContent.toLocaleLowerCase().includes('application/ld+json')) {
      startIndex = endBracket + 1;
      continue;
    }
    
    const endScript = html.indexOf('</script>', endBracket);
    if (endScript === -1) break;
    
    const jsonContent = html.substring(endBracket + 1, endScript);
    startIndex = endScript + 9;
    
    let root: unknown;
    try {
      root = JSON.parse(jsonContent.trim());
    } catch {
      continue;
    }

    for (const product of productObjects(root)) {
      image ??= normalizeImageUrl(readImage(product.image), baseUrl);
      const price = readOfferPrice(product.offers);
      if (price != null) return { price, image };
    }
  }
  return { price: null, image };
}

function* productObjects(value: unknown): Generator<Record<string, unknown>> {
  if (Array.isArray(value)) {
    for (const child of value) yield* productObjects(child);
    return;
  }
  if (!isRecord(value)) return;
  if (isProductType(value['@type'])) yield value;
  for (const child of Object.values(value)) yield* productObjects(child);
}

function isProductType(value: unknown): boolean {
  if (typeof value === 'string') return value.toLocaleLowerCase() === 'product';
  return Array.isArray(value) && value.some(isProductType);
}

function readOfferPrice(value: unknown): number | null {
  if (Array.isArray(value)) {
    for (const offer of value) {
      const price = readOfferPrice(offer);
      if (price != null) return price;
    }
    return null;
  }
  if (!isRecord(value)) return null;
  return parsePrice(value.price)
    ?? parsePrice(value.lowPrice)
    ?? readOfferPrice(value.priceSpecification);
}

function readImage(value: unknown): string | null {
  if (typeof value === 'string') return value.trim() || null;
  if (Array.isArray(value)) {
    for (const child of value) {
      const image = readImage(child);
      if (image) return image;
    }
    return null;
  }
  if (!isRecord(value)) return null;
  return readImage(value.url) ?? readImage(value.contentUrl);
}

function extractMetaPrice(html: string): number | null {
  for (const attributes of tagAttributes(html, 'meta')) {
    const property = (attributes.property ?? attributes.name)?.toLocaleLowerCase();
    if (property === 'product:price:amount' || property === 'og:price:amount') {
      const price = parsePrice(attributes.content);
      if (price != null) return price;
    }
  }
  for (const attributes of allOpeningTagAttributes(html)) {
    if (attributes.itemprop?.toLocaleLowerCase() !== 'price') continue;
    const price = parsePrice(attributes.content ?? attributes.value);
    if (price != null) return price;
  }
  return null;
}

function extractMetaImage(html: string): string | null {
  for (const attributes of tagAttributes(html, 'meta')) {
    const property = (attributes.property ?? attributes.name)?.toLocaleLowerCase();
    if (
      (property === 'og:image' ||
        property === 'og:image:secure_url' ||
        property === 'twitter:image' ||
        property === 'twitter:image:src') &&
      attributes.content?.trim()
    ) {
      return attributes.content.trim();
    }
  }
  for (const attributes of tagAttributes(html, 'link')) {
    const rel = attributes.rel?.toLocaleLowerCase();
    if (rel === 'image_src' && attributes.href?.trim()) {
      return attributes.href.trim();
    }
  }
  return null;
}

function extractRetailerPrice(html: string, url: string): number | null {
  const lowerUrl = url.toLocaleLowerCase();
  if (lowerUrl.includes('amazon') || lowerUrl.includes('amzn.')) {
    return extractAmazonPrice(html);
  }
  if (lowerUrl.includes('flipkart')) {
    return extractFlipkartPrice(html);
  }
  return null;
}

function extractFlipkartPrice(html: string): number | null {
  // 1. Direct CSS classes
  const cssPrice = firstElementPrice(html, ['Nx9bqj', '_30jeq3', 'CEmiEU'], []);
  if (cssPrice != null) return cssPrice;

  // 2. Embedded page data JSON (ppd / finalPrice / fsp)
  const ppdMatch = html.match(/"(?:finalPrice|fsp)"\s*:\s*(\d+(?:\.\d+)?)/i);
  if (ppdMatch) {
    const price = Number(ppdMatch[1]);
    if (Number.isFinite(price) && price > 0) return price;
  }

  const specialPriceMatch = html.match(/"specialPrice"\s*:\s*true[^{}]*?"price"\s*:\s*(\d+(?:\.\d+)?)/i);
  if (specialPriceMatch) {
    const price = Number(specialPriceMatch[1]);
    if (Number.isFinite(price) && price > 0) return price;
  }

  return null;
}

function extractAmazonPrice(html: string): number | null {
  const buyboxIds = [
    'corePriceDisplay_desktop_feature_div',
    'corePrice_desktop',
    'corePrice_mobile_feature_div',
    'price_inside_buybox',
    'priceblock_dealprice',
    'priceblock_ourprice',
    'priceblock_saleprice',
  ];

  for (const id of buyboxIds) {
    const containerRegex = new RegExp(`<div\\b[^>]*id=["']${id}["'][^>]*>([\\s\\S]*?)<\\/div>`, 'i');
    const match = html.match(containerRegex);
    if (match) {
      const price = firstElementPrice(match[1], ['a-offscreen', 'a-price-whole'], []);
      if (price != null) return price;
    }
  }

  const priceToPayRegex = /<(?:span|div)\b[^>]*class=["'][^"']*(?:priceToPay|apexPriceToPay)[^"']*["'][^>]*>([\s\S]*?)<\/(?:span|div)>/gi;
  for (const match of html.matchAll(priceToPayRegex)) {
    const price = firstElementPrice(match[0], ['a-offscreen', 'a-price-whole'], []);
    if (price != null) return price;
  }

  return firstElementPrice(html, ['a-offscreen', 'a-price-whole'], [
    'priceblock_ourprice',
    'priceblock_dealprice',
    'priceblock_saleprice',
  ]);
}

function extractRetailerImage(html: string, url: string): string | null {
  const lowerUrl = url.toLocaleLowerCase();
  const isAmazon = lowerUrl.includes('amazon') || lowerUrl.includes('amzn.');
  const isFlipkart = lowerUrl.includes('flipkart');
  const images = tagAttributes(html, 'img');

  if (isAmazon) {
    for (const attributes of images) {
      const id = (attributes.id ?? '').toLocaleLowerCase();
      const isMainId = id === 'landingimage' || id === 'imgblkfront' || id === 'main-image';
      if (!isMainId) continue;

      const dynamic = attributes['data-a-dynamic-image'];
      if (dynamic) {
        try {
          const decoded = JSON.parse(decodeHtml(dynamic)) as unknown;
          if (isRecord(decoded)) {
            const entries = Object.entries(decoded)
              .filter(([imgUrl]) => isValidProductImageUrl(imgUrl, url))
              .map(([imgUrl, dims]) => {
                let area = 0;
                if (Array.isArray(dims) && dims.length >= 2) {
                  area = (Number(dims[0]) || 0) * (Number(dims[1]) || 0);
                }
                return { url: imgUrl, area };
              })
              .sort((a, b) => b.area - a.area);

            if (entries.length > 0) {
              const normalized = normalizeImageUrl(entries[0].url, url);
              if (normalized) return normalized;
            }
          }
        } catch {
          // Fall back to direct attributes.
        }
      }

      const oldHires = attributes['data-old-hires'];
      if (oldHires && isValidProductImageUrl(oldHires, url)) {
        const normalized = normalizeImageUrl(oldHires, url);
        if (normalized) return normalized;
      }

      const src = attributes.src;
      if (src && isValidProductImageUrl(src, url)) {
        const normalized = normalizeImageUrl(src, url);
        if (normalized) return normalized;
      }
    }

    // Check main image container wrappers
    const wrapperPattern = /<div\b[^>]*(?:id=["'](?:imgTagWrapperId|main-image-container)["'])[^>]*>([\s\S]*?)<\/div>/gi;
    for (const match of html.matchAll(wrapperPattern)) {
      for (const attributes of tagAttributes(match[1], 'img')) {
        const candidate = attributes['data-old-hires'] ?? attributes.src;
        if (candidate && isValidProductImageUrl(candidate, url)) {
          const normalized = normalizeImageUrl(candidate, url);
          if (normalized) return normalized;
        }
      }
    }

    // Embedded colorImages fallback in scripts
    const colorImagesMatch = html.match(/['"]colorImages['"]\s*:\s*\{\s*['"]initial['"]\s*:\s*(\[[^\]]+\])/i);
    if (colorImagesMatch) {
      try {
        const parsed = JSON.parse(colorImagesMatch[1]) as unknown;
        if (Array.isArray(parsed)) {
          for (const item of parsed) {
            if (isRecord(item)) {
              const candidate = (typeof item.hiRes === 'string' && item.hiRes)
                || (typeof item.large === 'string' && item.large)
                || (typeof item.main === 'string' && item.main);
              if (candidate && isValidProductImageUrl(candidate, url)) {
                const normalized = normalizeImageUrl(candidate, url);
                if (normalized) return normalized;
              }
            }
          }
        }
      } catch {
        // Ignore JSON parse error
      }
    }
  }

  if (isFlipkart) {
    const supportedClasses = ['_396cs4', 'DByuf4', 'vLrBgc', '_53XmG7', 'q6DClP', '_2r_T1I'];
    for (const attributes of images) {
      if (!classNames(attributes.class).some((name) => supportedClasses.includes(name))) continue;
      const candidate = attributes.src ?? attributes['data-src'];
      if (candidate && isValidProductImageUrl(candidate, url)) {
        const normalized = normalizeImageUrl(candidate, url);
        if (normalized) return normalized;
      }
    }

    for (const attributes of images) {
      const candidate = attributes.src ?? attributes['data-src'];
      if (
        candidate &&
        (candidate.includes('flixcart.com/image/') || candidate.includes('flixcart.com/dl/image/')) &&
        candidate.includes('-original-') &&
        isValidProductImageUrl(candidate, url)
      ) {
        const normalized = normalizeImageUrl(candidate, url);
        if (normalized) return normalized;
      }
    }
  }

  return null;
}

function firstElementPrice(html: string, classes: string[], ids: string[]): number | null {
  const openingPattern = /<(span|div)\b([^>]*)>/gi;
  for (const match of html.matchAll(openingPattern)) {
    const attributes = parseAttributes(match[2] ?? '');
    const matchingClass = classNames(attributes.class).some((name) => classes.includes(name));
    if (!matchingClass && !ids.includes(attributes.id ?? '')) continue;

    // Skip strikethrough original prices and rating texts
    const preceding = html.slice(Math.max(0, (match.index ?? 0) - 250), match.index ?? 0).toLocaleLowerCase();
    if (
      preceding.includes('a-text-price') ||
      preceding.includes('data-a-strike="true"') ||
      preceding.includes('basisprice')
    ) {
      continue;
    }

    const start = (match.index ?? 0) + match[0].length;
    const close = html.toLocaleLowerCase().indexOf(`</${(match[1] ?? '').toLocaleLowerCase()}`, start);
    const content = close >= 0 ? html.slice(start, close) : '';
    const text = decodeHtml(stripTags(content));
    if (text.toLocaleLowerCase().includes('out of 5 stars')) continue;

    const price = parsePrice(text);
    if (price != null) return price;
  }
  return null;
}

function tagAttributes(html: string, tagName: string): Array<Record<string, string>> {
  const result: Array<Record<string, string>> = [];
  const pattern = new RegExp(`<${tagName}\\b([^>]*)>`, 'gi');
  for (const match of html.matchAll(pattern)) result.push(parseAttributes(match[1] ?? ''));
  return result;
}

function allOpeningTagAttributes(html: string): Array<Record<string, string>> {
  const result: Array<Record<string, string>> = [];
  const pattern = /<[a-z][a-z0-9:-]*\b([^>]*)>/gi;
  for (const match of html.matchAll(pattern)) result.push(parseAttributes(match[1] ?? ''));
  return result;
}

function parseAttributes(raw: string): Record<string, string> {
  const attributes: Record<string, string> = {};
  const pattern = /([^\s=/>]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+))/g;
  for (const match of raw.matchAll(pattern)) {
    const name = (match[1] ?? '').toLocaleLowerCase();
    attributes[name] = match[2] ?? match[3] ?? match[4] ?? '';
  }
  return attributes;
}

function classNames(value: string | undefined): string[] {
  return value?.split(/\s+/).filter(Boolean) ?? [];
}

function parsePrice(value: unknown): number | null {
  if (typeof value !== 'string' && typeof value !== 'number') return null;
  const clean = String(value).replace(/,/g, '').replace(/[^\d.]/g, '');
  if (!clean) return null;
  const price = Number(clean);
  return Number.isFinite(price) && price > 0 ? price : null;
}

export function isValidProductImageUrl(url: string | null | undefined, baseUrl?: string): boolean {
  if (!url || typeof url !== 'string') return false;
  const trimmed = url.trim();
  if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://')) return false;
  const lower = trimmed.toLowerCase();

  const genericBad = [
    'spacer',
    'pixel',
    '1x1',
    'blank.gif',
    'loading',
    'spinner',
    'placeholder',
    'data:image',
  ];
  if (genericBad.some((k) => lower.includes(k))) return false;
  if (
    lower.includes('/transparent.') ||
    lower.includes('transparent.gif') ||
    lower.includes('transparent_pixel') ||
    lower.includes('1x1_transparent') ||
    lower.includes('transparent-pixel') ||
    lower.endsWith('/transparent.png') ||
    lower.endsWith('/transparent.jpg')
  ) {
    return false;
  }

  const isAmazon = (baseUrl && (baseUrl.toLowerCase().includes('amazon') || baseUrl.toLowerCase().includes('amzn.')))
    || lower.includes('amazon')
    || lower.includes('media-amazon.com')
    || lower.includes('ssl-images-amazon.com');

  if (isAmazon) {
    if (lower.includes('/images/g/')) return false;

    const amazonBad = [
      'prime',
      'badge',
      'logo',
      'sprite',
      'fba',
      'check_box',
      'rating',
      'stars',
      'nav-',
      'play-button',
      'overlay',
      'icon',
      'cb542734830',
    ];
    if (amazonBad.some((k) => lower.includes(k))) return false;

    if (lower.includes('media-amazon.com') || lower.includes('ssl-images-amazon.com')) {
      if (!lower.includes('/images/i/')) return false;
    }
  }

  const isFlipkart = (baseUrl && baseUrl.toLowerCase().includes('flipkart'))
    || lower.includes('flipkart')
    || lower.includes('flixcart');

  if (isFlipkart) {
    const flipkartBad = ['plus', 'badge', 'logo', 'sprite', 'icon', 'fk-header', 'fk-cp-zion'];
    if (flipkartBad.some((k) => lower.includes(k))) return false;
  }

  return true;
}

export function normalizeMediumQualityImageUrl(url: string | null | undefined, baseUrl?: string): string | null {
  if (!url || typeof url !== 'string') return null;
  const trimmed = url.trim();
  if (!trimmed) return null;

  let result = trimmed;
  const lower = result.toLowerCase();

  // Flipkart CDN: normalize to balanced 500x500 medium quality (~14KB)
  if (lower.includes('flixcart.com') || lower.includes('flipkart.com')) {
    result = result.replace('rukmini1.flixcart.com', 'rukminim2.flixcart.com');
    result = result.replace(/\/image\/\d+\/\d+\//, '/image/500/500/');
    return result;
  }

  // Amazon CDN: normalize to balanced 500px medium quality (~23KB)
  if (
    lower.includes('media-amazon.com') ||
    lower.includes('ssl-images-amazon.com') ||
    lower.includes('images-amazon.com')
  ) {
    const modifierRegex = /\._(?:AC_)?[A-Z0-9_,]+_\.(jpe?g|png|webp)/i;
    if (modifierRegex.test(result)) {
      result = result.replace(modifierRegex, (_match, ext) => `._SL500_.${ext}`);
    } else {
      result = result.replace(/\.(jpe?g|png|webp)($|\?)/i, (_match, ext, query) => `._SL500_.${ext}${query || ''}`);
    }
    return result;
  }

  return result;
}

function normalizeImageUrl(value: string | null | undefined, baseUrl: string): string | null {
  if (!value?.trim()) return null;
  try {
    const parsed = new URL(decodeHtml(value.trim()), baseUrl);
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return null;
    if (parsed.protocol === 'http:') parsed.protocol = 'https:';
    let result = parsed.toString();
    if (!isValidProductImageUrl(result, baseUrl)) return null;
    const mediumQuality = normalizeMediumQualityImageUrl(result, baseUrl);
    return mediumQuality && isValidProductImageUrl(mediumQuality, baseUrl) ? mediumQuality : result;
  } catch {
    return null;
  }
}

function decodeHtml(value: string): string {
  return value
    .replace(/&nbsp;/gi, ' ')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;|&apos;/gi, "'")
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&amp;/gi, '&')
    .replace(/&#(\d+);/g, (_match, code: string) => String.fromCodePoint(Number(code)))
    .replace(/&#x([0-9a-f]+);/gi, (_match, code: string) => String.fromCodePoint(Number.parseInt(code, 16)));
}

function stripTags(value: string): string {
  return value
    .replace(/<script\b[^>]*>[\s\S]*?<\/script\s*>/gi, ' ')
    .replace(/<style\b[^>]*>[\s\S]*?<\/style\s*>/gi, ' ')
    .replace(/<!--([\s\S]*?)-->/g, ' ')
    .replace(/<[^>]+>/g, ' ');
}

function firstCapture(value: string, pattern: RegExp): string | null {
  return pattern.exec(value)?.[1] ?? null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value != null && !Array.isArray(value);
}
