import { Platform } from 'react-native';

import { normalizeRetailerUrl } from '../data/backup';
import type { PriceRetailer } from '../domain/models';
import { parseRetailerPage } from './pricePageParser';

const REQUEST_TIMEOUT_MS = 22_000;
const MAX_HTML_CHARACTERS = 8_000_000;

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

const AMAZON_USER_AGENTS = [
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Version/17.5 Mobile/15E148 Safari/604.1',
  'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36',
];

export async function fetchRetailerPrice(
  rawUrl: string,
  retailer: PriceRetailer,
  externalSignal?: AbortSignal,
): Promise<RetailerCheckResult> {
  const url = normalizeRetailerUrl(rawUrl, retailer);
  if (!url) {
    return failure(retailer, 'INVALID_URL', `The saved ${displayName(retailer)} link is invalid.`);
  }

  const controller = new AbortController();
  let timedOut = false;
  const abortFromCaller = () => controller.abort();
  externalSignal?.addEventListener('abort', abortFromCaller, { once: true });
  const timeout = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(url, {
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
    const parsed = parseRetailerPage(html, response.url || url);
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
      return failure(retailer, 'TIMEOUT', `${displayName(retailer)} did not respond in time.`);
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
    Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'en-IN,en;q=0.9',
    'Cache-Control': 'no-cache',
  };
  if (Platform.OS === 'web') return common;
  return {
    ...common,
    'User-Agent': retailer === 'FLIPKART'
      ? 'Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)'
      : AMAZON_USER_AGENTS[Math.floor(Math.random() * AMAZON_USER_AGENTS.length)]!,
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
