import { Platform } from 'react-native';

import { normalizeRetailerUrl } from '../data/backup';
import type { PriceRetailer } from '../domain/models';
import { parseRetailerPage } from './pricePageParser';
import { buildRetailerRequestUrl } from './retailerRequestStrategy';

const REQUEST_TIMEOUT_MS: Record<PriceRetailer, number> = {
  AMAZON: 8_000,
  FLIPKART: 7_000,
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

const AMAZON_DESKTOP_USER_AGENT = Platform.OS === 'ios'
  ? 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
  : 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';

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

  try {
    const response = await raceRequest(fetch(requestUrl, {
      method: 'GET',
      redirect: 'follow',
      signal: controller.signal,
      headers: requestHeaders(retailer),
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
    const html = await raceRequest(response.text());
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
    clearTimeout(deadlineTimer!);
    externalSignal?.removeEventListener('abort', abortFromCaller);
    rejectCallerAbort = null;
  }
}

function requestHeaders(retailer: PriceRetailer): Record<string, string> {
  const common = {
    Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
    'Accept-Language': 'en-IN,en;q=0.9',
  };
  if (Platform.OS === 'web') return common;
  return {
    ...common,
    'User-Agent': retailer === 'FLIPKART'
      ? 'Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)'
      : AMAZON_DESKTOP_USER_AGENT,
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
