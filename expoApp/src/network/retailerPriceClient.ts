import { Platform } from 'react-native';

import { normalizeRetailerUrl } from '../data/backup';
import type { PriceRetailer } from '../domain/models';
import { reportThroughputBytes } from '../hooks/useNetworkThroughput';
import { parseRetailerPage } from './pricePageParser';
import { buildRetailerRequestUrl, extractAmazonAsin, recordResolvedAmazonAsin } from './retailerRequestStrategy';
import { requestDeduplicator } from './requestDeduplicator';

const REQUEST_TIMEOUT_MS: Record<PriceRetailer, number> = {
  AMAZON: 10_000,
  FLIPKART: 10_000,
};
const MAX_HTML_CHARACTERS = 5_000_000;

export type RetailerCheckFailureCode =
  | 'BLOCKED'
  | 'HTTP'
  | 'INVALID_URL'
  | 'NETWORK'
  | 'TIMEOUT'
  | 'UNAVAILABLE';

export type RetailerCheckResult =
  | {
      ok: true;
      retailer: PriceRetailer;
      price: number;
      image: string | null;
      checkedAt: number;
    }
  | {
      ok: false;
      retailer: PriceRetailer;
      code: RetailerCheckFailureCode;
      message: string;
    };

export const AMAZON_USER_AGENTS = [
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
  'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36',
];

export const RETRY_DELAY_STEPS = [350, 800];

export interface FetchRetailerPriceOptions {
  skipImage?: boolean;
}

export async function fetchRetailerPrice(
  rawUrl: string,
  retailer: PriceRetailer,
  externalSignal?: AbortSignal,
  options?: FetchRetailerPriceOptions,
): Promise<RetailerCheckResult> {
  const url = normalizeRetailerUrl(rawUrl, retailer);
  if (!url) {
    return failure(retailer, 'INVALID_URL', `The saved ${displayName(retailer)} link is invalid.`);
  }
  const requestUrl = buildRetailerRequestUrl(url, retailer);
  const dedupeKey = `${retailer}:${requestUrl}:${options?.skipImage ? 'noimg' : 'img'}`;

  return requestDeduplicator.coalesce(dedupeKey, async () => {
    const maxAttempts = RETRY_DELAY_STEPS.length + 1;
    let lastResult: RetailerCheckResult | null = null;

    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      if (externalSignal?.aborted) throw abortError();

      lastResult = await fetchStandard(requestUrl, retailer, attempt, externalSignal, url, options);

      if (lastResult.ok) {
        return lastResult;
      }

      const canRetry = attempt < RETRY_DELAY_STEPS.length &&
        !externalSignal?.aborted &&
        lastResult.code !== 'INVALID_URL' &&
        lastResult.code !== 'TIMEOUT';

      if (canRetry) {
        await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY_STEPS[attempt]));
      } else {
        break;
      }
    }

    return lastResult ?? failure(retailer, 'UNAVAILABLE', `Could not fetch ${displayName(retailer)} price.`);
  });
}

async function fetchStandard(
  requestUrl: string,
  retailer: PriceRetailer,
  attempt: number,
  externalSignal?: AbortSignal,
  rawUrl?: string,
  options?: FetchRetailerPriceOptions,
): Promise<RetailerCheckResult> {
  const controller = new AbortController();
  let timedOut = false;
  let rejectCallerAbort: ((reason: Error) => void) | null = null;
  const callerAbort = new Promise<never>((_resolve, reject) => {
    rejectCallerAbort = reject;
  });
  const abortFromCaller = () => {
    controller.abort();
    rejectCallerAbort?.(abortError());
  };
  externalSignal?.addEventListener('abort', abortFromCaller, { once: true });
  if (externalSignal?.aborted) abortFromCaller();

  let deadlineTimer: ReturnType<typeof setTimeout>;
  const deadline = new Promise<never>((_resolve, reject) => {
    deadlineTimer = setTimeout(() => {
      timedOut = true;
      controller.abort();
      reject(new Error('Retailer request deadline reached.'));
    }, REQUEST_TIMEOUT_MS[retailer]);
  });
  const raceRequest = <T>(request: Promise<T>): Promise<T> => (
    Promise.race([request, deadline, callerAbort])
  );

  const startTime = Date.now();

  try {
    const response = await raceRequest(fetch(requestUrl, {
      method: 'GET',
      redirect: 'follow',
      signal: controller.signal,
      headers: requestHeaders(retailer, attempt),
    }));
    if (externalSignal?.aborted) throw abortError();
    if (response.status === 403 || response.status === 429) {
      return failure(retailer, 'BLOCKED', `${displayName(retailer)} temporarily blocked the live check.`);
    }
    if (!response.ok) {
      return failure(retailer, 'HTTP', `${displayName(retailer)} returned HTTP ${response.status}.`);
    }

    const declaredLength = Number(response.headers.get('content-length'));
    if (Number.isFinite(declaredLength) && declaredLength > MAX_HTML_CHARACTERS) {
      return failure(retailer, 'UNAVAILABLE', `${displayName(retailer)} returned a page that was too large to inspect safely.`);
    }

    const { html, parsed: earlyParsed } = await readHtmlFast(response, requestUrl, retailer, raceRequest, options?.skipImage);
    const elapsed = Math.max(1, Date.now() - startTime);
    reportThroughputBytes(html.length, elapsed);

    if (externalSignal?.aborted) throw abortError();
    if (html.length > MAX_HTML_CHARACTERS) {
      return failure(retailer, 'UNAVAILABLE', `${displayName(retailer)} returned a page that was too large to inspect safely.`);
    }
    const parsed = earlyParsed ?? parseRetailerPage(html, response.url || requestUrl);

    if (retailer === 'AMAZON') {
      const finalAsin = extractAmazonAsin(response.url || '');
      if (finalAsin) {
        if (rawUrl) recordResolvedAmazonAsin(rawUrl, finalAsin);
        recordResolvedAmazonAsin(requestUrl, finalAsin);
      }
    }

    if (parsed.blocked) {
      return failure(retailer, 'BLOCKED', `${displayName(retailer)} requested a human verification check.`);
    }
    if (parsed.price == null) {
      return failure(retailer, 'UNAVAILABLE', `No current ${displayName(retailer)} price was found.`);
    }
    return {
      ok: true,
      retailer,
      price: parsed.price,
      image: options?.skipImage ? null : parsed.image,
      checkedAt: Date.now(),
    };
  } catch (error) {
    if (externalSignal?.aborted) throw abortError();
    if (timedOut) {
      return failure(
        retailer,
        'TIMEOUT',
        `${displayName(retailer)} did not respond within ${REQUEST_TIMEOUT_MS[retailer] / 1000} seconds. The saved price is still available.`,
      );
    }
    return failure(
      retailer,
      'NETWORK',
      Platform.OS === 'web'
        ? 'Retailer live checks may be blocked in the browser preview. Test this check on Android or iPhone.'
        : `Could not connect to ${displayName(retailer)}. Check the internet connection and try again.`,
    );
  } finally {
    clearTimeout(deadlineTimer!);
    externalSignal?.removeEventListener('abort', abortFromCaller);
    rejectCallerAbort = null;
  }
}

async function readHtmlFast(
  response: Response,
  requestUrl: string,
  retailer: PriceRetailer,
  raceRequest: <T>(p: Promise<T>) => Promise<T>,
  skipImage?: boolean,
): Promise<{ html: string; parsed: ReturnType<typeof parseRetailerPage> | null }> {
  const body = response.body;
  if (!body || typeof (body as any).getReader !== 'function') {
    const html = await raceRequest(response.text());
    return { html, parsed: null };
  }

  const reader = (body as any).getReader();
  const decoder = new TextDecoder();
  let html = '';
  let parsed: ReturnType<typeof parseRetailerPage> | null = null;
  const finalUrl = response.url || requestUrl;
  const isFlipkart = retailer === 'FLIPKART';

  try {
    while (true) {
      const chunk = (await raceRequest(reader.read())) as { done?: boolean; value?: Uint8Array } | null | undefined;
      if (!chunk || chunk.done) break;
      if (chunk.value) {
        html += decoder.decode(chunk.value, { stream: true });
      }

      // Early parse trigger:
      // For Amazon: price & image are situated in the first 80KB - 300KB of markup.
      // For Flipkart: price and image can appear in embedded JSON (finalPrice/fsp), og:image, or at ~1.4MB in LD+JSON.
      if (
        html.length >= 50_000 &&
        (html.includes('a-price') ||
          html.includes('a-offscreen') ||
          html.includes('apexPriceToPay') ||
          html.includes('application/ld+json') ||
          html.includes('Nx9bqj') ||
          html.includes('_30jeq3') ||
          html.includes('finalPrice') ||
          html.includes('"fsp"'))
      ) {
        const candidate = parseRetailerPage(html, finalUrl);
        if (candidate.blocked) {
          parsed = candidate;
          try { await reader.cancel(); } catch { /* ignore */ }
          break;
        }
        // If caller requested skipImage and price is found, terminate immediately
        if (skipImage && candidate.price != null) {
          parsed = candidate;
          try { await reader.cancel(); } catch { /* ignore */ }
          break;
        }
        // If both price and image are found, we can terminate early
        if (candidate.price != null && candidate.image != null) {
          parsed = candidate;
          try { await reader.cancel(); } catch { /* ignore */ }
          break;
        }
        // If price is found but image is not needed or image already found, terminate
        if (candidate.price != null && (skipImage || candidate.image != null)) {
          parsed = candidate;
          try { await reader.cancel(); } catch { /* ignore */ }
          break;
        }
      }

      // Safety ceiling: allow up to 2.5MB for Flipkart and 1.5MB for Amazon
      const maxStreamBytes = isFlipkart ? 2_500_000 : 1_500_000;
      if (html.length >= maxStreamBytes) {
        try { await reader.cancel(); } catch { /* ignore */ }
        break;
      }
    }
  } catch {
    // If stream reading fails or is aborted early, proceed with the accumulated HTML
    try { await reader.cancel(); } catch { /* ignore */ }
  }

  return { html, parsed };
}

function requestHeaders(retailer: PriceRetailer, userAgentIndex = 0): Record<string, string> {
  const common = {
    Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
    'Accept-Language': 'en-IN,en;q=0.9',
  };
  if (Platform.OS === 'web') return common;
  return {
    ...common,
    'User-Agent': AMAZON_USER_AGENTS[userAgentIndex % AMAZON_USER_AGENTS.length],
    'Upgrade-Insecure-Requests': '1',
    'Sec-Fetch-Dest': 'document',
    'Sec-Fetch-Mode': 'navigate',
    'Sec-Fetch-Site': 'none',
    'Sec-Fetch-User': '?1',
  };
}

function failure(
  retailer: PriceRetailer,
  code: RetailerCheckFailureCode,
  message: string,
): RetailerCheckResult {
  return { ok: false, retailer, code, message };
}

function displayName(retailer: PriceRetailer): string {
  return retailer === 'AMAZON' ? 'Amazon' : 'Flipkart';
}

function abortError(): Error {
  const error = new Error('Price check cancelled.');
  error.name = 'AbortError';
  return error;
}
