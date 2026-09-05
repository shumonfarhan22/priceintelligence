import { Platform } from 'react-native';
import type { AppCustomization } from '../domain/customization';
import type { InventoryProduct } from '../domain/models';
import {
  buildPriceChangeNotificationText,
  detectPriceChanges,
  filterPriceChangesForAlerts,
  type DetectedPriceChange,
  type PriceChangeNotificationText,
} from '../domain/priceChangeNotifications';
import {
  buildSmartRefreshPlan,
  hasMeaningfulPriceMovement,
  readAutomaticRefreshAttempts,
  readSmartRefreshProfile,
  smartRefreshSpacingMillis,
  updateSmartRefreshOutcome,
  writeAutomaticRefreshAttempts,
  writeSmartRefreshProfile,
} from '../domain/smartRefreshPlanner';
import { fetchRetailerPrice } from '../network/retailerPriceClient';
import type { InventoryRepository } from './inventoryRepository';

const LEDGER_STORAGE_KEY = 'supreme_price_intelligence_refresh_ledger';
const PROFILE_STORAGE_KEY = 'supreme_price_intelligence_smart_profile';

let memoryLedger = '';
let memoryProfile = '';

export interface DailyPriceRefreshBatchResult {
  checkedProducts: number;
  remainingProducts: number;
  completed: boolean;
}

export interface PriceAlertCallback {
  (changes: DetectedPriceChange[], text: PriceChangeNotificationText): void;
}

export class DailyBackgroundPriceRefresh {
  private isRunning = false;

  constructor(
    private readonly repository: InventoryRepository,
    private readonly getCustomization: () => AppCustomization,
    private readonly isNotificationsEnabled: () => boolean,
    private readonly onAlert?: PriceAlertCallback
  ) {}

  private async readStorage(key: string): Promise<string> {
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        const val = window.localStorage.getItem(key);
        if (val != null) return val;
      } catch {}
    }
    const dbVal = await this.repository.getMetadata(key);
    if (dbVal != null) return dbVal;
    return key === LEDGER_STORAGE_KEY ? memoryLedger : memoryProfile;
  }

  private async writeStorage(key: string, value: string): Promise<void> {
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        window.localStorage.setItem(key, value);
      } catch {}
    }
    if (key === LEDGER_STORAGE_KEY) {
      memoryLedger = value;
    } else {
      memoryProfile = value;
    }
    await this.repository.setMetadata(key, value);
  }

  async runBatch(
    maximumProducts = 4,
    maximumRuntimeMillis = 6 * 60 * 1000,
    enforceDelay = false
  ): Promise<DailyPriceRefreshBatchResult> {
    if (this.isRunning) {
      return { checkedProducts: 0, remainingProducts: 0, completed: false };
    }

    const customization = this.getCustomization();
    if (!customization.automaticPriceChecksEnabled) {
      return { checkedProducts: 0, remainingProducts: 0, completed: true };
    }

    this.isRunning = true;
    try {
      const safeMaximumProducts = Math.max(1, Math.min(8, maximumProducts));
      const safeMaximumRuntime = Math.max(10_000, Math.min(8 * 60 * 1000, maximumRuntimeMillis));
      const startedAt = Date.now();

      let storedLedger = await this.readStorage(LEDGER_STORAGE_KEY);
      let attempted = readAutomaticRefreshAttempts(storedLedger, startedAt);

      let storedProfile = await this.readStorage(PROFILE_STORAGE_KEY);
      let profile = readSmartRefreshProfile(storedProfile);

      const allProducts = await this.repository.listProducts('');
      const plan = buildSmartRefreshPlan(allProducts, profile, attempted, startedAt);

      let checkedCount = 0;

      for (const item of plan) {
        if (!this.getCustomization().automaticPriceChecksEnabled) break;
        if (checkedCount >= safeMaximumProducts) break;

        const now = Date.now();
        if (now - startedAt >= safeMaximumRuntime) break;

        // Check if attempted in concurrent run
        if (attempted.has(item.id)) continue;

        const outcome = await this.refreshOneProduct(item);
        checkedCount++;

        // Update smart refresh profile
        profile = updateSmartRefreshOutcome(
          profile,
          item.id,
          outcome.succeeded,
          outcome.priceMoved,
          outcome.checkedAt
        );
        await this.writeStorage(PROFILE_STORAGE_KEY, writeSmartRefreshProfile(profile));

        // Mark product attempted
        attempted.add(item.id);
        await this.writeStorage(LEDGER_STORAGE_KEY, writeAutomaticRefreshAttempts(attempted, outcome.checkedAt));

        // Publish alerts if enabled
        if (this.isNotificationsEnabled() && outcome.changes.length > 0 && this.onAlert) {
          try {
            const currentCustomization = this.getCustomization();
            const alertChanges = filterPriceChangesForAlerts(outcome.changes, currentCustomization);
            if (alertChanges.length > 0) {
              const alertText = buildPriceChangeNotificationText(alertChanges);
              this.onAlert(alertChanges, alertText);
            }
          } catch {}
        }

        // Polite delay between products
        if (enforceDelay && checkedCount < safeMaximumProducts) {
          const afterCheck = Date.now();
          const spacing = smartRefreshSpacingMillis(item.id, afterCheck);
          if (afterCheck - startedAt + spacing < safeMaximumRuntime) {
            await new Promise((resolve) => setTimeout(resolve, spacing));
          }
        }
      }

      const finishTime = Date.now();
      const updatedAttempted = readAutomaticRefreshAttempts(
        await this.readStorage(LEDGER_STORAGE_KEY),
        finishTime
      );

      const remaining = allProducts.filter((p) => {
        const hasRetailer = Boolean(
          (p.amazonUrl && p.amazonUrl.trim().length > 0) ||
          (p.flipkartUrl && p.flipkartUrl.trim().length > 0)
        );
        return hasRetailer && !updatedAttempted.has(p.id);
      }).length;

      return {
        checkedProducts: checkedCount,
        remainingProducts: remaining,
        completed: remaining === 0,
      };
    } finally {
      this.isRunning = false;
    }
  }

  private async refreshOneProduct(item: InventoryProduct): Promise<{
    succeeded: boolean;
    priceMoved: boolean;
    changes: DetectedPriceChange[];
    checkedAt: number;
  }> {
    const checkedAt = Date.now();
    try {
      const hasImage = Boolean(item.imageUrl);
      const amazonPromise = item.amazonUrl && item.amazonUrl.trim()
        ? fetchRetailerPrice(item.amazonUrl.trim(), 'AMAZON', undefined, { skipImage: hasImage })
        : Promise.resolve(null);

      const flipkartPromise = item.flipkartUrl && item.flipkartUrl.trim()
        ? fetchRetailerPrice(item.flipkartUrl.trim(), 'FLIPKART', undefined, { skipImage: hasImage })
        : Promise.resolve(null);

      const [amazonRes, flipkartRes] = await Promise.all([amazonPromise, flipkartPromise]);

      const amazonPrice = amazonRes && amazonRes.ok ? amazonRes.price : null;
      const flipkartPrice = flipkartRes && flipkartRes.ok ? flipkartRes.price : null;
      const amazonImage = amazonRes && amazonRes.ok ? amazonRes.image : null;
      const flipkartImage = flipkartRes && flipkartRes.ok ? flipkartRes.image : null;

      const detectedChanges = detectPriceChanges(
        item,
        amazonPrice,
        flipkartPrice,
        checkedAt
      );

      const savedChanges: DetectedPriceChange[] = [];

      if (amazonPrice != null && amazonPrice > 0) {
        const saved = await this.repository.recordRetailerPrice(
          item.id,
          'AMAZON',
          amazonPrice,
          checkedAt,
          amazonImage
        );
        if (saved) {
          const matched = detectedChanges.find((c) => c.retailer === 'AMAZON');
          if (matched) savedChanges.push(matched);
        }
      }

      if (flipkartPrice != null && flipkartPrice > 0) {
        const saved = await this.repository.recordRetailerPrice(
          item.id,
          'FLIPKART',
          flipkartPrice,
          checkedAt,
          flipkartImage
        );
        if (saved) {
          const matched = detectedChanges.find((c) => c.retailer === 'FLIPKART');
          if (matched) savedChanges.push(matched);
        }
      }

      const succeeded = (amazonPrice != null && amazonPrice > 0) ||
                        (flipkartPrice != null && flipkartPrice > 0);

      const priceMoved =
        hasMeaningfulPriceMovement(item.amazonLastPrice, amazonPrice) ||
        hasMeaningfulPriceMovement(item.flipkartLastPrice, flipkartPrice);

      return {
        succeeded,
        priceMoved,
        changes: savedChanges,
        checkedAt,
      };
    } catch {
      return {
        succeeded: false,
        priceMoved: false,
        changes: [],
        checkedAt,
      };
    }
  }
}