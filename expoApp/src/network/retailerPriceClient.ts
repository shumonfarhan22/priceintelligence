import { Platform } from 'react-native';

import { normalizeRetailerUrl } from '../data/backup';
import type { PriceRetailer } from '../domain/models';
import { parseRetailerPage } from './pricePageParser';
import { buildRetailerRequestUrl } from './retailerRequestStrategy';

const REQUEST_TIMEOUT_MS: Record<PriceRetailer, number> = {
  AMAZON: 10_000,
  FLIPKART: 8_000,
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

const AMAZON_MOBILE_USER_AGENT = Platform.OS === 'ios'
  ? 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1'
  : 'Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 Chrome/138.0 Mobile Safari/537.36';

export async function fetchRetailerPrice(
  rawUrl: string,
  retailer: PriceRetailer,
  externalSignal?: AbortSignal,
): Promise<RetailerCheckResult> {
  const url = normalizeRetailerUrl(rawUrl, retailer);
  if (!url) {
    return failure(retailer, 'INVALID_URL', `The saved ${displayName(retailer)} link is invalid.`);
  }
  const requestUrl = buildRetailerRequestUrl(url, retailer);

  const controller = new AbortController();
  let timedOut = false;
  const abortFromCaller = () => controller.abort();
  externalSignal?.addEventListener('abort', abortFromCaller, { once: true });
  const timeout = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, REQUEST_TIMEOUT_MS[retailer]);

  try {
    const response = await fetch(requestUrl, {
      method: 'GET',
      redirect: 'follow',
      signal: controller.signal,
      headers: requestHeaders(retailer),
    });
    if (externalSignal?.aborted) throw abortError();
    if (response.status === 403 || response.status === 429) {
      return failure(retailer, 'BLOCKED', `${displayName(retailer)} temporarily blocked the live check.`);
    }
    if (!response.ok) {
      return failure(retailer, 'HTTP', `${displayName(retailer)} returned HTTP ${response.status}.`);
    }

    const html = await response.text();
    if (externalSignal?.aborted) throw abortError();
    if (html.length > MAX_HTML_CHARACTERS) {
      return failure(retailer, 'UNAVAILABLE', `${displayName(retailer)} returned a page that was too large to inspect safely.`);
    }
    const parsed = parseRetailerPage(html, response.url || requestUrl);
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
      image: parsed.image,
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
    clearTimeout(timeout);
    externalSignal?.removeEventListener('abort', abortFromCaller);
  }
}

function requestHeaders(retailer: PriceRetailer): Record<string, string> {
  const common = {
    Accept: 'text/html,application/xhtml+xml;q=0.9,*/*;q=0.7',
    'Accept-Language': 'en-IN,en;q=0.9',
  };
  if (Platform.OS === 'web') return common;
  return {
    ...common,
    'User-Agent': retailer === 'FLIPKART'
      ? 'Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)'
      : AMAZON_MOBILE_USER_AGENT,
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
