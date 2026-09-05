import { describe, expect, it } from 'vitest';
import type { InventoryProduct } from './models';
import { DEFAULT_APP_CUSTOMIZATION } from './customization';
import {
  automaticRefreshDayKey,
  buildSmartRefreshPlan,
  hasMeaningfulPriceMovement,
  readAutomaticRefreshAttempts,
  readSmartRefreshProfile,
  smartRefreshPriorityScore,
  updateSmartRefreshOutcome,
  writeAutomaticRefreshAttempts,
  writeSmartRefreshProfile,
} from './smartRefreshPlanner';
import {
  buildPriceChangeNotificationText,
  detectPriceChanges,
  filterPriceChangesForAlerts,
  priceChangeNotificationNavigation,
  type DetectedPriceChange,
} from './priceChangeNotifications';

function makeTestProduct(partial: Partial<InventoryProduct> & { id: number; productName: string }): InventoryProduct {
  return {
    shopPrice: 500,
    purchaseCost: null,
    barcode: null,
    pricebuddyProductId: null,
    amazonUrl: 'https://amazon.in/dp/test',
    flipkartUrl: null,
    imageUrl: null,
    searchCount: 0,
    createdAt: 10000,
    updatedAt: 10000,
    amazonLastPrice: null,
    amazonLastChecked: null,
    flipkartLastPrice: null,
    flipkartLastChecked: null,
    ...partial,
  };
}

describe('smartRefreshPlanner', () => {
  it('calculates day key taking IST offset into account', () => {
    // 0 epoch is 05:30 AM in IST (same day)
    const dayKey = automaticRefreshDayKey(0);
    expect(dayKey).toBe(0);

    const oneDayLater = 24 * 60 * 60 * 1000;
    expect(automaticRefreshDayKey(oneDayLater)).toBe(1);
  });

  it('reads and writes automatic refresh attempts with day rollover', () => {
    const now = 1700000000000;
    const ids = new Set([1, 4, 7]);
    const serialized = writeAutomaticRefreshAttempts(ids, now);
    
    expect(serialized).toContain('|1,4,7');

    const restored = readAutomaticRefreshAttempts(serialized, now);
    expect(restored).toEqual(ids);

    // If day changed (e.g. 2 days later), returns empty set
    const future = now + 48 * 60 * 60 * 1000;
    const rolledOver = readAutomaticRefreshAttempts(serialized, future);
    expect(rolledOver.size).toBe(0);
  });

  it('serializes and parses smart refresh profile records', () => {
    const records = new Map([
      [10, { productId: 10, consecutiveFailures: 2, volatilityPoints: 6, lastOutcomeAt: 5000 }],
      [20, { productId: 20, consecutiveFailures: 0, volatilityPoints: 4, lastOutcomeAt: 6000 }],
    ]);

    const serialized = writeSmartRefreshProfile(records);
    expect(serialized.startsWith('1|')).toBe(true);

    const parsed = readSmartRefreshProfile(serialized);
    expect(parsed.size).toBe(2);
    expect(parsed.get(10)).toEqual(records.get(10));
    expect(parsed.get(20)).toEqual(records.get(20));
  });

  it('updates smart refresh outcome on success and failure', () => {
    const initial = new Map();
    const t1 = 10000;

    // First: success with price movement -> volatility increases
    const s1 = updateSmartRefreshOutcome(initial, 5, true, true, t1);
    expect(s1.get(5)).toEqual({
      productId: 5,
      consecutiveFailures: 0,
      volatilityPoints: 2,
      lastOutcomeAt: t1,
    });

    // Second: failure -> failure count increments, volatility stays
    const t2 = 20000;
    const s2 = updateSmartRefreshOutcome(s1, 5, false, false, t2);
    expect(s2.get(5)).toEqual({
      productId: 5,
      consecutiveFailures: 1,
      volatilityPoints: 2,
      lastOutcomeAt: t2,
    });

    // Third: success with no price movement -> volatility decreases, failures reset to 0
    const t3 = 30000;
    const s3 = updateSmartRefreshOutcome(s2, 5, true, false, t3);
    expect(s3.get(5)).toEqual({
      productId: 5,
      consecutiveFailures: 0,
      volatilityPoints: 1,
      lastOutcomeAt: t3,
    });
  });

  it('detects meaningful price movement (>= 1%)', () => {
    expect(hasMeaningfulPriceMovement(100, 101.5)).toBe(true); // +1.5%
    expect(hasMeaningfulPriceMovement(100, 98)).toBe(true); // -2%
    expect(hasMeaningfulPriceMovement(100, 100.4)).toBe(false); // +0.4%
    expect(hasMeaningfulPriceMovement(null, 100)).toBe(false);
  });

  it('builds smart refresh plan prioritizing unchecked and volatile products', () => {
    const now = 1700000000000;
    const p1 = makeTestProduct({ id: 1, productName: 'Apple iPhone', amazonLastChecked: null }); // never checked
    const p2 = makeTestProduct({ id: 2, productName: 'Samsung Galaxy', amazonLastChecked: now - 100000 }); // checked recently
    const p3 = makeTestProduct({ id: 3, productName: 'No Retailer', amazonUrl: null, flipkartUrl: null }); // no link
    const p4 = makeTestProduct({ id: 4, productName: 'Already Attempted' });

    const profile = new Map([
      [2, { productId: 2, consecutiveFailures: 0, volatilityPoints: 8, lastOutcomeAt: now - 100000 }],
    ]);

    const attempted = new Set([4]);
    const plan = buildSmartRefreshPlan([p1, p2, p3, p4], profile, attempted, now);

    // p3 (no link) and p4 (attempted) must be excluded
    expect(plan.map((p) => p.id)).not.toContain(3);
    expect(plan.map((p) => p.id)).not.toContain(4);

    // p1 has never been checked -> highest score (+120 missing price score)
    expect(plan[0].id).toBe(1);
    expect(plan[1].id).toBe(2);
  });
});

describe('priceChangeNotifications', () => {
  it('detects price changes from retailer price checks', () => {
    const item = makeTestProduct({
      id: 42,
      productName: 'Sony Headphones',
      amazonLastPrice: 2000,
      flipkartLastPrice: 2200,
    });

    const changes = detectPriceChanges(item, 1800, 2200, 50000);
    expect(changes.length).toBe(1);
    expect(changes[0]).toEqual({
      productId: 42,
      productName: 'Sony Headphones',
      retailer: 'AMAZON',
      oldPrice: 2000,
      newPrice: 1800,
      direction: 'LOWER',
      detectedAt: 50000,
    });
  });

  it('filters price changes based on user customization thresholds', () => {
    const changeDown50: DetectedPriceChange = {
      productId: 1,
      productName: 'Item 1',
      retailer: 'AMAZON',
      oldPrice: 1000,
      newPrice: 950,
      direction: 'LOWER',
      detectedAt: 100,
    };
    const changeUp20: DetectedPriceChange = {
      productId: 2,
      productName: 'Item 2',
      retailer: 'FLIPKART',
      oldPrice: 1000,
      newPrice: 1020,
      direction: 'HIGHER',
      detectedAt: 100,
    };

    // 1. DECREASES_ONLY with RUPEES_50 threshold
    const filterDecreases = filterPriceChangesForAlerts([changeDown50, changeUp20], {
      ...DEFAULT_APP_CUSTOMIZATION,
      priceAlertDirection: 'DECREASES_ONLY',
      priceAlertThreshold: 'RUPEES_50',
    });
    expect(filterDecreases).toEqual([changeDown50]);

    // 2. INCREASES_ONLY
    const filterIncreases = filterPriceChangesForAlerts([changeDown50, changeUp20], {
      ...DEFAULT_APP_CUSTOMIZATION,
      priceAlertDirection: 'INCREASES_ONLY',
      priceAlertThreshold: 'ANY',
    });
    expect(filterIncreases).toEqual([changeUp20]);

    // 3. PERCENT_5 threshold: changeDown50 is 5% (1000 -> 950), changeUp20 is 2% (1000 -> 1020)
    const filter5Percent = filterPriceChangesForAlerts([changeDown50, changeUp20], {
      ...DEFAULT_APP_CUSTOMIZATION,
      priceAlertDirection: 'BOTH',
      priceAlertThreshold: 'PERCENT_5',
    });
    expect(filter5Percent).toEqual([changeDown50]);
  });

  it('formats single and multiple price change notification copy', () => {
    const single: DetectedPriceChange[] = [
      {
        productId: 1,
        productName: 'Logitech Mouse',
        retailer: 'AMAZON',
        oldPrice: 999,
        newPrice: 799,
        direction: 'LOWER',
        detectedAt: 100,
      },
    ];

    const singleText = buildPriceChangeNotificationText(single);
    expect(singleText.title).toBe('Amazon price dropped');
    expect(singleText.body).toContain('Logitech Mouse: ₹999 → ₹799');

    const multi: DetectedPriceChange[] = [
      ...single,
      {
        productId: 2,
        productName: 'Keychron Keyboard',
        retailer: 'FLIPKART',
        oldPrice: 4000,
        newPrice: 4500,
        direction: 'HIGHER',
        detectedAt: 100,
      },
    ];

    const multiText = buildPriceChangeNotificationText(multi);
    expect(multiText.title).toBe('2 online prices changed');
    expect(multiText.body).toContain('2 products • 1 lower • 1 higher');
  });

  it('notifies navigation listeners when price movement target is triggered', () => {
    let receivedTarget: any = null;
    const unsubscribe = priceChangeNotificationNavigation.subscribe((t) => {
      receivedTarget = t;
    });

    priceChangeNotificationNavigation.openPriceMovement(
      10,
      'AMAZON',
      500,
      400,
      'LOWER',
      12345
    );

    expect(receivedTarget).not.toBeNull();
    expect(receivedTarget.productId).toBe(10);
    expect(receivedTarget.oldPrice).toBe(500);
    expect(receivedTarget.newPrice).toBe(400);

    priceChangeNotificationNavigation.consume(receivedTarget.requestId);
    expect(priceChangeNotificationNavigation.getPendingTarget()).toBeNull();

    unsubscribe();
  });
});
