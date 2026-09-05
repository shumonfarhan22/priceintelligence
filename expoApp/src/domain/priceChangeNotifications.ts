import type { AppCustomization } from './customization';
import type { InventoryProduct, PriceRetailer } from './models';

export type DetectedPriceDirection = 'LOWER' | 'HIGHER';

export interface DetectedPriceChange {
  productId: number;
  productName: string;
  retailer: PriceRetailer;
  oldPrice: number;
  newPrice: number;
  direction: DetectedPriceDirection;
  detectedAt: number;
}

export interface PriceChangeNotificationText {
  title: string;
  body: string;
}

export interface PriceMovementNotificationTarget {
  requestId: string;
  productId: number;
  retailer: PriceRetailer;
  oldPrice: number;
  newPrice: number;
  direction: DetectedPriceDirection;
  detectedAt: number;
}

function priceInPaise(price: number): number {
  return Math.round(price * 100);
}

function formatIndianPrice(amount: number): string {
  const rounded = Math.round(amount);
  return '₹' + rounded.toLocaleString('en-IN');
}

export function detectPriceChanges(
  item: InventoryProduct,
  amazonPrice: number | null | undefined,
  flipkartPrice: number | null | undefined,
  detectedAt: number
): DetectedPriceChange[] {
  const changes: DetectedPriceChange[] = [];

  const checkRetailer = (retailer: PriceRetailer, oldPrice: number | null | undefined, newPrice: number | null | undefined) => {
    if (
      oldPrice == null ||
      newPrice == null ||
      !Number.isFinite(oldPrice) ||
      !Number.isFinite(newPrice) ||
      oldPrice <= 0 ||
      newPrice <= 0 ||
      detectedAt <= 0
    ) {
      return;
    }

    if (priceInPaise(oldPrice) === priceInPaise(newPrice)) {
      return;
    }

    changes.push({
      productId: item.id,
      productName: item.productName,
      retailer,
      oldPrice,
      newPrice,
      direction: newPrice < oldPrice ? 'LOWER' : 'HIGHER',
      detectedAt,
    });
  };

  checkRetailer('AMAZON', item.amazonLastPrice, amazonPrice);
  checkRetailer('FLIPKART', item.flipkartLastPrice, flipkartPrice);

  return changes;
}

export function filterPriceChangesForAlerts(
  changes: DetectedPriceChange[],
  customization: AppCustomization
): DetectedPriceChange[] {
  return changes.filter((change) => {
    let directionAccepted = true;
    switch (customization.priceAlertDirection) {
      case 'BOTH':
        directionAccepted = true;
        break;
      case 'INCREASES_ONLY':
        directionAccepted = change.direction === 'HIGHER';
        break;
      case 'DECREASES_ONLY':
        directionAccepted = change.direction === 'LOWER';
        break;
    }

    const difference = Math.abs(change.newPrice - change.oldPrice);
    const percent = change.oldPrice > 0 ? (difference / change.oldPrice) * 100.0 : 0.0;

    let thresholdAccepted = true;
    switch (customization.priceAlertThreshold) {
      case 'ANY':
        thresholdAccepted = true;
        break;
      case 'RUPEES_50':
        thresholdAccepted = difference >= 50.0;
        break;
      case 'PERCENT_2':
        thresholdAccepted = percent >= 2.0;
        break;
      case 'PERCENT_5':
        thresholdAccepted = percent >= 5.0;
        break;
    }

    return directionAccepted && thresholdAccepted;
  });
}

export function buildPriceChangeNotificationText(
  changes: DetectedPriceChange[]
): PriceChangeNotificationText {
  // Deduplicate changes
  const uniqueMap = new Map<string, DetectedPriceChange>();
  for (const c of changes) {
    const key = `${c.productId}-${c.retailer}-${priceInPaise(c.newPrice)}`;
    if (!uniqueMap.has(key)) uniqueMap.set(key, c);
  }
  const distinctChanges = Array.from(uniqueMap.values());

  if (distinctChanges.length === 1) {
    const single = distinctChanges[0];
    const retailerName = single.retailer === 'AMAZON' ? 'Amazon' : 'Flipkart';
    const movementText = single.direction === 'LOWER' ? 'dropped' : 'increased';

    return {
      title: `${retailerName} price ${movementText}`,
      body: `${single.productName}: ${formatIndianPrice(single.oldPrice)} → ${formatIndianPrice(single.newPrice)}`,
    };
  }

  const lowerCount = distinctChanges.filter((c) => c.direction === 'LOWER').length;
  const higherCount = distinctChanges.filter((c) => c.direction === 'HIGHER').length;
  const productCount = new Set(distinctChanges.map((c) => c.productId)).size;

  const movementParts: string[] = [];
  if (lowerCount > 0) movementParts.push(`${lowerCount} lower`);
  if (higherCount > 0) movementParts.push(`${higherCount} higher`);

  const productLabel = productCount === 1 ? '1 product' : `${productCount} products`;
  const body = movementParts.length > 0 ? `${productLabel} • ${movementParts.join(' • ')}` : productLabel;

  return {
    title: `${distinctChanges.length} online prices changed`,
    body,
  };
}

// Navigation event bus for price alerts
class PriceChangeNotificationNavigationBus {
  private target: PriceMovementNotificationTarget | null = null;
  private listeners = new Set<(target: PriceMovementNotificationTarget | null) => void>();

  getPendingTarget(): PriceMovementNotificationTarget | null {
    return this.target;
  }

  openPriceMovement(
    productId: number,
    retailer: PriceRetailer,
    oldPrice: number,
    newPrice: number,
    direction: DetectedPriceDirection,
    detectedAt: number
  ) {
    if (
      productId <= 0 ||
      !Number.isFinite(oldPrice) ||
      !Number.isFinite(newPrice) ||
      oldPrice <= 0 ||
      newPrice <= 0 ||
      detectedAt <= 0
    ) {
      return;
    }

    this.target = {
      requestId: `${productId}-${retailer}-${detectedAt}`,
      productId,
      retailer,
      oldPrice,
      newPrice,
      direction,
      detectedAt,
    };

    for (const listener of this.listeners) {
      listener(this.target);
    }
  }

  consume(requestId: string) {
    if (this.target?.requestId === requestId) {
      this.target = null;
      for (const listener of this.listeners) {
        listener(null);
      }
    }
  }

  subscribe(listener: (target: PriceMovementNotificationTarget | null) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }
}

export const priceChangeNotificationNavigation = new PriceChangeNotificationNavigationBus();
