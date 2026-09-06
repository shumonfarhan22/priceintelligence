import { describe, expect, it } from 'vitest';
import { comparisonOrder, comparisonSearch } from './inventorySearch';

describe('inventoryRepository search mechanism', () => {
  it('returns empty where clause for blank or whitespace query', () => {
    expect(comparisonSearch('')).toEqual({ where: '', params: [] });
    expect(comparisonSearch('   ')).toEqual({ where: '', params: [] });
  });

  it('handles numeric queries like "500" by searching product name and exact barcode', () => {
    const result = comparisonSearch('500');
    expect(result.where).toContain('instr(lower(product_name), ?)');
    expect(result.where).toContain("lower(COALESCE(barcode, '')) = ?");
    expect(result.params).toContain('500');
  });

  it('handles multi-word queries with punctuation and units like "Tea, 250g"', () => {
    const result = comparisonSearch('Tea, 250g');
    expect(result.where).toContain('instr(lower(product_name), ?)');
    expect(result.params).toContain('tea');
    expect(result.params).toContain('250');
    expect(result.params).toContain('g');
  });

  it('handles barcode queries with 4+ digits using partial match', () => {
    const result = comparisonSearch('8901030001234');
    expect(result.where).toContain("instr(lower(COALESCE(barcode, '')), ?)");
    expect(result.params).toContain('8901030001234');
  });

  it('handles URLs by searching retailer links specifically', () => {
    const result = comparisonSearch('https://www.amazon.in/dp/B00EXAMPLE');
    expect(result.where).toContain('amazon_url');
    expect(result.where).toContain('flipkart_url');
    expect(result.where).not.toContain('product_name');
  });

  it('orders exact barcode match and exact/prefix name matches first when query is provided', () => {
    const order = comparisonOrder('ALPHABETICAL', '500');
    expect(order.orderSql).toMatch(/CASE\s+WHEN/);
    expect(order.orderSql).toContain("WHEN lower(COALESCE(barcode, '')) = ? THEN 0");
    expect(order.orderSql).toContain('WHEN lower(product_name) = ? THEN 1');
    expect(order.orderSql).toContain('WHEN instr(lower(product_name), ?) = 1 THEN 2');
    expect(order.orderParams).toEqual(['500', '500', '500', '500']);
  });

  it('uses standard sort order without priority prefix when query is empty', () => {
    const order = comparisonOrder('MOST_VIEWED', '');
    expect(order.orderSql).not.toMatch(/CASE\s+WHEN/);
    expect(order.orderSql).toContain('search_count DESC');
    expect(order.orderParams).toEqual([]);
  });
});
