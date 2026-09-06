import type { ComparisonSort } from '../domain/comparison';

function looksLikeUrl(value: string): boolean {
  return value.startsWith('http') || value.includes('amazon') || value.includes('flipkart');
}

export function comparisonSearch(query: string): { where: string; params: string[] } {
  const trimmed = query.trim();
  if (!trimmed) return { where: '', params: [] };

  const lowerTrimmed = trimmed.toLowerCase();

  // If query is a URL, search specifically in retailer link columns
  if (looksLikeUrl(lowerTrimmed)) {
    return {
      where: ` WHERE (instr(lower(COALESCE(amazon_url, '')), ?) > 0
        OR instr(lower(COALESCE(flipkart_url, '')), ?) > 0)`,
      params: [lowerTrimmed, lowerTrimmed],
    };
  }

  // Normalize units like 500g -> "500 g" so both "500g" and "500 g" match
  const normalized = lowerTrimmed.replace(/(\d+)\s*(g|kg|ml|l|gm|ltr|pack|pc|pcs)\b/gi, '$1 $2');
  const tokens = normalized.split(/[^\w]+/i).filter(Boolean).slice(0, 10);

  // If tokens exist, require all tokens in product_name
  const nameClause = tokens.length > 0
    ? tokens.map(() => 'instr(lower(product_name), ?) > 0').join(' AND ')
    : '1=1';

  // Barcode matching rules:
  // - If query is purely digits of 4+ characters, allow partial/prefix barcode match
  // - Otherwise (short digits like "1", "12", or queries with letters), only allow exact barcode match
  const isPureDigits = /^\d+$/.test(trimmed);
  let barcodeClause: string;
  let barcodeParam: string;

  if (isPureDigits && trimmed.length >= 4) {
    barcodeClause = 'instr(lower(COALESCE(barcode, \'\')), ?) > 0';
    barcodeParam = lowerTrimmed;
  } else {
    barcodeClause = 'lower(COALESCE(barcode, \'\')) = ?';
    barcodeParam = lowerTrimmed;
  }

  const where = ` WHERE ((${nameClause}) OR (${barcodeClause}))`;
  const params = [...tokens, barcodeParam];

  return { where, params };
}

export function comparisonOrder(sort: ComparisonSort, query = ''): { orderSql: string; orderParams: string[] } {
  const trimmed = query.trim().toLowerCase();
  const priorityPrefix = trimmed
    ? `CASE
        WHEN lower(COALESCE(barcode, '')) = ? THEN 0
        WHEN lower(product_name) = ? THEN 1
        WHEN instr(lower(product_name), ?) = 1 THEN 2
        WHEN instr(lower(product_name), ' ' || ?) > 0 THEN 3
        ELSE 4
      END ASC, `
    : '';
  const orderParams = trimmed ? [trimmed, trimmed, trimmed, trimmed] : [];

  let baseSortSql: string;
  switch (sort) {
    case 'ALPHABETICAL':
      baseSortSql = 'product_name COLLATE NOCASE ASC, id ASC';
      break;
    case 'RECENT':
      baseSortSql = 'updated_at DESC, product_name COLLATE NOCASE ASC, id ASC';
      break;
    case 'BEST_SAVING':
      baseSortSql = `(shop_price - CASE
        WHEN amazon_last_price > 0 AND flipkart_last_price > 0
          THEN CASE WHEN amazon_last_price < flipkart_last_price THEN amazon_last_price ELSE flipkart_last_price END
        WHEN amazon_last_price > 0 THEN amazon_last_price
        WHEN flipkart_last_price > 0 THEN flipkart_last_price
        ELSE shop_price
      END) DESC, product_name COLLATE NOCASE ASC, id ASC`;
      break;
    case 'MOST_VIEWED':
    default:
      baseSortSql = 'search_count DESC, updated_at DESC, product_name COLLATE NOCASE ASC, id ASC';
      break;
  }

  return {
    orderSql: `${priorityPrefix}${baseSortSql}`,
    orderParams,
  };
}
