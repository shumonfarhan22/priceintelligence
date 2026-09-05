import { InventoryProduct, PriceHistoryEntry } from './models';
import { InsightGroup, InsightPosition, InsightProduct, productNeedsPriceCheck } from './insights';
import { formatRupees } from './formatting';

export type AnalysisTone = 'GOOD' | 'BAD' | 'WARNING' | 'NEUTRAL';

export interface AnalysisMessage {
  label: string;
  headline: string;
  explanation: string;
  recommendation: string;
  tone: AnalysisTone;
}

export interface OnlineEvidence {
  retailer: string;
  price: number;
}

function validPrice(price: number | null | undefined): number | null {
  if (price === null || price === undefined || !Number.isFinite(price) || price <= 0) return null;
  return price;
}

export function lowestOnline(item: InventoryProduct): OnlineEvidence | null {
  const options: OnlineEvidence[] = [];
  const az = validPrice(item.amazonLastPrice);
  if (az) options.push({ retailer: 'Amazon', price: az });
  const fk = validPrice(item.flipkartLastPrice);
  if (fk) options.push({ retailer: 'Flipkart', price: fk });

  if (options.length === 0) return null;
  return options.reduce((prev, current) => (prev.price < current.price ? prev : current));
}

function missingPriceRetailers(item: InventoryProduct): string[] {
  const missing: string[] = [];
  if (!!item.amazonUrl?.trim() && !validPrice(item.amazonLastPrice)) missing.push('Amazon');
  if (!!item.flipkartUrl?.trim() && !validPrice(item.flipkartLastPrice)) missing.push('Flipkart');
  return missing;
}

function formatPercent(value: number): string {
  return value.toFixed(1) + '%';
}

function competitiveHeadline(item: InventoryProduct, online: OnlineEvidence | null, gapText: string): string {
  if (!online) return 'The shop price is currently competitive';
  const difference = item.shopPrice - online.price;
  if (difference < -0.01) {
    return `Shop is ${gapText} below ${online.retailer}`;
  } else {
    return 'Shop matches the best saved online price';
  }
}

function retailerPressureMessage(item: InventoryProduct, retailer: string, price: number | null | undefined): AnalysisMessage {
  const usable = validPrice(price);
  let gap: number | null = null;
  if (usable) {
    gap = Math.abs(item.shopPrice - usable);
  }

  return {
    label: `${retailer.toUpperCase()} PRICE PRESSURE`,
    headline: `${retailer} is below the shop price`,
    explanation: gap ? `${retailer}'s saved price is ${formatRupees(gap)} lower than the shop price.` : `${retailer} does not currently have a usable saved price.`,
    recommendation: 'Check freshness and margin before changing the shop price.',
    tone: 'BAD',
  };
}

function getProductBasicReason(product: InsightProduct): string {
  const positionText = product.position === 'COMPETITIVE' ? 'Shop price is competitive' : product.position === 'REVIEW' ? 'Online price is lower' : 'No usable online comparison';
  return product.needsCheck ? `${positionText} • Price check due` : `${positionText} • Price is fresh`;
}

export function buildAnalysisMessage(
  product: InsightProduct,
  group: InsightGroup | null,
  brand: string | null,
  groupSize: number
): AnalysisMessage {
  const item = product.item;
  const online = lowestOnline(item);
  let gap: number | null = null;
  if (online) gap = Math.abs(item.shopPrice - online.price);

  let gapText = 'an unknown amount';
  if (gap !== null) {
    const percent = item.shopPrice > 0 ? (gap / item.shopPrice) * 100 : 0;
    gapText = `${formatRupees(gap)} (${formatPercent(percent)})`;
  }

  if (brand && !group) {
    switch (product.position) {
      case 'COMPETITIVE':
        return {
          label: `${brand} BRAND HEALTH`.toUpperCase(),
          headline: "This product supports the brand's competitive position",
          explanation: `It is one of ${groupSize} ${brand} products in this analysis and is not above the best saved online price.`,
          recommendation: product.needsCheck ? "Refresh the retailer prices before relying on this position." : "Keep monitoring the product and confirm that its margin remains healthy.",
          tone: 'GOOD'
        };
      case 'REVIEW':
        return {
          label: `${brand} BRAND HEALTH`.toUpperCase(),
          headline: "This product weakens the brand's price position",
          explanation: `${online?.retailer || 'An online retailer'} is ${gapText} lower than the shop price.`,
          recommendation: "Review the selling price and margin. Refresh first if the retailer price is old.",
          tone: 'BAD'
        };
      case 'NO_COMPARISON':
      default:
        return {
          label: `${brand} BRAND HEALTH`.toUpperCase(),
          headline: "This product has no reliable brand comparison",
          explanation: "A usable online price is not currently available.",
          recommendation: "Add or check retailer links, then refresh this product.",
          tone: 'WARNING'
        };
    }
  }

  switch (group) {
    case 'COMPETITIVE_FRESH':
      return {
        label: "COMPETITIVE • FRESH",
        headline: competitiveHeadline(item, online, gapText),
        explanation: "Recent saved retailer prices support this competitive result.",
        recommendation: "Keep monitoring it and confirm that the purchase cost leaves a healthy margin.",
        tone: 'GOOD'
      };
    case 'COMPETITIVE_DUE':
      return {
        label: "COMPETITIVE • CHECK DUE",
        headline: "The last saved result was competitive",
        explanation: "The retailer evidence is old enough that the market position may have changed.",
        recommendation: "Refresh live prices before making a pricing decision.",
        tone: 'WARNING'
      };
    case 'REVIEW_FRESH':
      return {
        label: "REVIEW • FRESH",
        headline: `${online?.retailer || 'Online'} is ${gapText} lower`,
        explanation: "A recent saved online price is below the shop price.",
        recommendation: "Review the selling price together with purchase cost and margin. Do not reduce it automatically.",
        tone: 'BAD'
      };
    case 'REVIEW_DUE':
      return {
        label: "REVIEW • CHECK DUE",
        headline: "The saved comparison needs review, but its evidence is old",
        explanation: `${online?.retailer || 'An online retailer'} was ${gapText} lower at the last usable check.`,
        recommendation: "Refresh first, then review the selling price only if the disadvantage remains.",
        tone: 'WARNING'
      };
    case 'AMAZON_PRESSURE':
      return retailerPressureMessage(item, 'Amazon', item.amazonLastPrice);
    case 'FLIPKART_PRESSURE':
      return retailerPressureMessage(item, 'Flipkart', item.flipkartLastPrice);
    case 'ONLINE_LOWER':
      return {
        label: "ONLINE PRICE LOWER",
        headline: `${online?.retailer || 'Online'} is ${gapText} lower`,
        explanation: "This saved gap placed the product in the online-lower group.",
        recommendation: "Compare the gap with your margin and refresh if the retailer evidence is old.",
        tone: 'BAD'
      };
    case 'NEAR_MATCH':
      return {
        label: "NEAR MARKET MATCH",
        headline: "Shop price is within 5% of the online best",
        explanation: `${online?.retailer || 'The online'} gap is ${gapText}.`,
        recommendation: "This is usually acceptable for retail customers. Monitor to ensure the gap doesn't widen.",
        tone: 'WARNING'
      };
    case 'SHOP_LOWER':
      return {
        label: "SHOP PRICE LOWER",
        headline: competitiveHeadline(item, online, gapText),
        explanation: "This saved gap placed the product in the shop-lower group.",
        recommendation: "Ensure this isn't due to an old saved price or a mistaken online product link.",
        tone: 'GOOD'
      };
    case 'NEEDS_CHECK':
      return {
        label: "PRICE CHECK DUE",
        headline: "A retailer price hasn't been refreshed recently",
        explanation: "The saved data is older than the required freshness window.",
        recommendation: "Run a manual live-price refresh before using this product for a decision.",
        tone: 'WARNING'
      };
    case 'MISSING_LINKS':
      return {
        label: "MISSING RETAILER LINKS",
        headline: "No Amazon or Flipkart link is saved",
        explanation: "The app cannot fetch an online price without a retailer link.",
        recommendation: "Edit the product in Inventory and add at least one valid retailer link.",
        tone: 'WARNING'
      };
    case 'MISSING_PRICES': {
      const missingRetailers = missingPriceRetailers(item);
      return {
        label: "MISSING SAVED PRICES",
        headline: "A linked retailer has no usable saved price",
        explanation: missingRetailers.length === 0 ? "The saved retailer data is incomplete." : `${missingRetailers.join(' and ')} has no usable saved price.`,
        recommendation: "Refresh the linked retailer. If it still fails, verify the saved product link.",
        tone: 'WARNING'
      };
    }
    case 'MISSING_COSTS':
      return {
        label: "MISSING PURCHASE COST",
        headline: "Profit and margin cannot be calculated",
        explanation: "The selling price exists, but purchase cost is missing or invalid.",
        recommendation: "Edit the product in Inventory and enter its purchase cost.",
        tone: 'WARNING'
      };
    default:
      return {
        label: "PRODUCT INSIGHT",
        headline: getProductBasicReason(product),
        explanation: "This analysis uses the latest safe prices stored by the app.",
        recommendation: "Review the evidence and refresh if any retailer price is old.",
        tone: product.position === 'COMPETITIVE' ? 'GOOD' : product.position === 'REVIEW' ? 'BAD' : 'WARNING'
      };
  }
}

export type PriceMovement = 'LOWER' | 'HIGHER' | 'UNCHANGED' | 'UNKNOWN';

export interface RetailerPriceHistorySummary {
  retailer: 'AMAZON' | 'FLIPKART';
  latestPrice: number;
  previousPrice: number | null;
  lowestSavedPrice: number;
  highestSavedPrice: number;
  latestCheckedAt: number;
  observationCount: number;
  movement: PriceMovement;
  movementAmount: number | null;
  movementPercent: number | null;
  recentPrices: number[];
}

export function summarizePriceHistory(
  entries: PriceHistoryEntry[],
  retailer: 'AMAZON' | 'FLIPKART',
): RetailerPriceHistorySummary | null {
  const valid = entries
    .filter(
      (e) =>
        e.retailer === retailer &&
        Number.isFinite(e.price) &&
        e.price > 0 &&
        e.checkedAt > 0,
    )
    .sort((a, b) => b.checkedAt - a.checkedAt || b.id - a.id);

  if (valid.length === 0) return null;
  const latest = valid[0];
  const previous = valid.length > 1 ? valid[1] : null;
  const change = previous ? latest.price - previous.price : null;

  let movement: PriceMovement = 'UNKNOWN';
  if (change !== null) {
    if (Math.abs(change) <= 0.01) movement = 'UNCHANGED';
    else if (change < 0) movement = 'LOWER';
    else movement = 'HIGHER';
  }

  const percent =
    previous && previous.price > 0 && change !== null
      ? (change / previous.price) * 100
      : null;

  const prices = valid.map((v) => v.price);
  return {
    retailer,
    latestPrice: latest.price,
    previousPrice: previous ? previous.price : null,
    lowestSavedPrice: Math.min(...prices),
    highestSavedPrice: Math.max(...prices),
    latestCheckedAt: latest.checkedAt,
    observationCount: valid.length,
    movement,
    movementAmount: change !== null ? Math.abs(change) : null,
    movementPercent: percent !== null ? Math.abs(percent) : null,
    recentPrices: prices.slice(0, 4),
  };
}
