export type PriceRetailer = 'AMAZON' | 'FLIPKART';

export interface InventoryProduct {
  id: number;
  productName: string;
  barcode: string | null;
  shopPrice: number;
  purchaseCost: number | null;
  pricebuddyProductId: number | null;
  amazonUrl: string | null;
  flipkartUrl: string | null;
  imageUrl: string | null;
  searchCount: number;
  createdAt: number;
  updatedAt: number;
  amazonLastPrice: number | null;
  amazonLastChecked: number | null;
  flipkartLastPrice: number | null;
  flipkartLastChecked: number | null;
}

export interface PriceObservation {
  retailer: PriceRetailer;
  price: number;
  checkedAt: number;
}

export interface ImportedProduct extends Omit<InventoryProduct, 'id'> {
  priceHistory: PriceObservation[];
}

export interface BackupImportResult {
  addedCount: number;
  duplicateCount: number;
  invalidCount: number;
}
