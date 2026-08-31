import type { PriceRetailer } from './models';
import { normalizeRetailerUrl } from '../data/backup';

export interface InventoryDraft {
  productName: string;
  purchaseCost: string;
  shopPrice: string;
  barcode: string;
  amazonUrl: string;
  flipkartUrl: string;
}

export interface ValidatedInventoryInput {
  productName: string;
  purchaseCost: number | null;
  shopPrice: number;
  barcode: string | null;
  amazonUrl: string | null;
  flipkartUrl: string | null;
}

export type InventoryValidation =
  | { valid: true; input: ValidatedInventoryInput }
  | { valid: false; message: string; field: keyof InventoryDraft };

export const emptyInventoryDraft: InventoryDraft = {
  productName: '',
  purchaseCost: '',
  shopPrice: '',
  barcode: '',
  amazonUrl: '',
  flipkartUrl: '',
};

export function validateInventoryDraft(draft: InventoryDraft): InventoryValidation {
  const productName = draft.productName.trim();
  if (!productName) {
    return invalid('Product name is required.', 'productName');
  }

  const shopPrice = parsePositivePrice(draft.shopPrice);
  if (shopPrice == null) {
    return invalid(
      draft.shopPrice.trim() ? 'Enter a valid selling price greater than zero.' : 'Selling price is required.',
      'shopPrice',
    );
  }

  const purchaseText = draft.purchaseCost.trim();
  const purchaseCost = purchaseText ? parsePositivePrice(purchaseText) : null;
  if (purchaseText && purchaseCost == null) {
    return invalid('Enter a valid purchase cost greater than zero.', 'purchaseCost');
  }

  const amazonResult = validateRetailerUrl(draft.amazonUrl, 'AMAZON');
  if (!amazonResult.valid) return invalid('Enter a valid Amazon product link.', 'amazonUrl');

  const flipkartResult = validateRetailerUrl(draft.flipkartUrl, 'FLIPKART');
  if (!flipkartResult.valid) return invalid('Enter a valid Flipkart product link.', 'flipkartUrl');

  return {
    valid: true,
    input: {
      productName,
      shopPrice,
      purchaseCost,
      barcode: draft.barcode.trim() || null,
      amazonUrl: amazonResult.value,
      flipkartUrl: flipkartResult.value,
    },
  };
}

function validateRetailerUrl(
  rawValue: string,
  retailer: PriceRetailer,
): { valid: true; value: string | null } | { valid: false } {
  const trimmed = rawValue.trim();
  if (!trimmed) return { valid: true, value: null };
  const normalized = normalizeRetailerUrl(trimmed, retailer);
  return normalized ? { valid: true, value: normalized } : { valid: false };
}

function parsePositivePrice(rawValue: string): number | null {
  const normalized = rawValue.trim();
  if (!normalized) return null;
  const value = Number(normalized);
  return Number.isFinite(value) && value > 0 ? value : null;
}

function invalid(message: string, field: keyof InventoryDraft): InventoryValidation {
  return { valid: false, message, field };
}
