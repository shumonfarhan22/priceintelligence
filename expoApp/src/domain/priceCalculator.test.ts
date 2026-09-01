import { describe, expect, it } from 'vitest';

import { appendCalculatorKey, evaluatePriceExpression, formatCalculatorValue } from './priceCalculator';

describe('price calculator', () => {
  it('uses normal operator precedence', () => {
    expect(evaluatePriceExpression('100+25×2')).toBe(150);
    expect(evaluatePriceExpression('100÷4+5')).toBe(30);
  });

  it('applies percentages like the original calculator', () => {
    expect(evaluatePriceExpression('100+10%')).toBe(110);
    expect(evaluatePriceExpression('500×10%')).toBe(50);
  });

  it('reports invalid calculations', () => {
    expect(() => evaluatePriceExpression('12÷0')).toThrow('Cannot divide by zero');
    expect(() => evaluatePriceExpression('12+')).toThrow('Check the calculation');
  });

  it('builds and formats expressions without duplicate decimals', () => {
    expect(appendCalculatorKey('0', '00')).toBe('00');
    expect(appendCalculatorKey('12.3', '.')).toBe('12.3');
    expect(appendCalculatorKey('12+', '×')).toBe('12×');
    expect(formatCalculatorValue(12.5)).toBe('12.5');
  });
});
