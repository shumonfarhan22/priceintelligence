import type { ComparisonSort } from '../domain/comparison';

export function comparisonSearch(query: string): { where: string; params: string[] } {
  const trimmed = query.trim();
  if (!trimmed) return { where: '', params: [] };

  const lowerTrimmed = trimmed.toLowerCase();
  // Split query into alphanumeric tokens, stripping punctuation (commas, hyphens, brackets, quotes)
  const tokens = lowerTrimmed.split(/[^\w]+/i).filter(Boolean).slice(0, 10);

  // If tokens exist, require all tokens in product_name
  const nameClause = tokens.length > 0
    ? tokens.map(() => 'instr(lower(product_name), ?) > 0').join(' AND ')
    : '1=1';

  // Barcode can match either lowerTrimmed or digitsOnly if query has digits
  const digitsOnly = trimmed.replace(/\D/g, '');
  const barcodeConditions = ['instr(lower(COALESCE(barcode, \'\')), ?) > 0'];
  const barcodeParams = [lowerTrimmed];
  if (digitsOnly && digitsOnly !== lowerTrimmed) {
    barcodeConditions.push('instr(lower(COALESCE(barcode, \'\')), ?) > 0');
    barcodeParams.push(digitsOnly);
  }

  const where = ` WHERE ((${nameClause})
    OR (${barcodeConditions.join(' OR ')})
    OR instr(lower(COALESCE(amazon_url, \'\')), ?) > 0
    OR instr(lower(COALESCE(flipkart_url, \'\')), ?) > 0)`;

  const params = [
    ...tokens,
    ...barcodeParams,
    lowerTrimmed,
    lowerTrimmed,
  ];

  return { where, params };
}

export function comparisonOrder(sort: ComparisonSort, query = ''): { orderSql: string; orderParams: string[] } {
  const trimmed = query.trim().toLowerCase();
  const priorityPrefix = trimmed
    ? `CASE
        WHEN lower(COALESCE(barcode, '')) = ? THEN 0
        WHEN lower(product_name) = ? THEN 1
        WHEN instr(lower(product_name), ?) = 1 THEN 2
        WHEN instr(lower(COALESCE(barcode, '')) , ?) = 1 THEN 3
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
