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
    ?? extractMetaPrice(html)
    ?? extractRetailerPrice(html, url);
  const image = structured.image
    ?? normalizeImageUrl(extractMetaImage(html), url)
    ?? extractRetailerImage(html, url);

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
  const scriptPattern = /<script\b([^>]*)>([\s\S]*?)<\/script\s*>/gi;
  for (const match of html.matchAll(scriptPattern)) {
    const attributes = parseAttributes(match[1] ?? '');
    if (attributes.type?.toLocaleLowerCase() !== 'application/ld+json') continue;
    let root: unknown;
    try {
      root = JSON.parse((match[2] ?? '').trim());
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
    if (property === 'og:image' && attributes.content?.trim()) return attributes.content.trim();
  }
  return null;
}

function extractRetailerPrice(html: string, url: string): number | null {
  const lowerUrl = url.toLocaleLowerCase();
  if (lowerUrl.includes('amazon') || lowerUrl.includes('amzn.')) {
    return firstElementPrice(html, ['a-offscreen', 'a-price-whole'], [
      'priceblock_ourprice',
      'priceblock_dealprice',
    ]);
  }
  if (lowerUrl.includes('flipkart')) {
    return firstElementPrice(html, ['Nx9bqj', '_30jeq3', 'CEmiEU'], []);
  }
  return null;
}

function extractRetailerImage(html: string, url: string): string | null {
  const lowerUrl = url.toLocaleLowerCase();
  const images = tagAttributes(html, 'img');
  if (lowerUrl.includes('amazon') || lowerUrl.includes('amzn.')) {
    for (const attributes of images) {
      const id = attributes.id ?? '';
      const classes = classNames(attributes.class);
      if (id !== 'landingImage' && id !== 'imgBlkFront' && !classes.includes('a-dynamic-image')) continue;
      const dynamic = attributes['data-a-dynamic-image'];
      if (dynamic) {
        try {
          const decoded = JSON.parse(decodeHtml(dynamic)) as unknown;
          if (isRecord(decoded)) {
            const first = Object.keys(decoded)[0];
            const normalized = normalizeImageUrl(first ?? null, url);
            if (normalized) return normalized;
          }
        } catch {
          // Fall back to ordinary image attributes.
        }
      }
      const normalized = normalizeImageUrl(attributes['data-old-hires'] ?? attributes.src, url);
      if (normalized) return normalized;
    }
  }
  if (lowerUrl.includes('flipkart')) {
    const supportedClasses = ['_396cs4', 'DByuf4', 'vLrBgc'];
    for (const attributes of images) {
      if (!classNames(attributes.class).some((name) => supportedClasses.includes(name))) continue;
      const normalized = normalizeImageUrl(attributes.src, url);
      if (normalized) return normalized;
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
    const start = (match.index ?? 0) + match[0].length;
    const close = html.toLocaleLowerCase().indexOf(`</${(match[1] ?? '').toLocaleLowerCase()}`, start);
    const content = close >= 0 ? html.slice(start, close) : '';
    const price = parsePrice(decodeHtml(stripTags(content)));
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

function normalizeImageUrl(value: string | null | undefined, baseUrl: string): string | null {
  if (!value?.trim()) return null;
  try {
    const parsed = new URL(decodeHtml(value.trim()), baseUrl);
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return null;
    if (parsed.protocol === 'http:') parsed.protocol = 'https:';
    return parsed.toString();
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
