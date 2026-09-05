const MAX_CALCULATOR_VALUE = 999_999_999.99;
const DIVISION_EPSILON = 0.000_000_1;

export function evaluatePriceExpression(expression: string): number {
  const normalized = expression
    .replaceAll('×', '*')
    .replaceAll('÷', '/')
    .replaceAll('-', '-')
    .replaceAll(' ', '');
  if (!normalized) throw new Error('Enter a calculation');

  const parser = new PriceExpressionParser(normalized);
  const result = parser.parse();
  if (!Number.isFinite(result) || Math.abs(result) > MAX_CALCULATOR_VALUE) {
    throw new Error('The result is too large');
  }
  return roundToCurrency(result);
}

export function formatCalculatorValue(value: number): string {
  return Number(roundToCurrency(value).toFixed(2)).toString();
}

export function appendCalculatorKey(expression: string, key: string): string {
  const current = expression || '0';
  const operators = new Set(['+', '−', '×', '÷']);

  if (operators.has(key)) {
    return operators.has(current.at(-1) ?? '') ? `${current.slice(0, -1)}${key}` : `${current}${key}`;
  }

  if (key === '.') {
    const segment = current.split(/[+−×÷%]/).at(-1) ?? '';
    if (segment.includes('.')) return current;
  }

  return current === '0' && /^\d/.test(key) ? key : `${current}${key}`;
}

function roundToCurrency(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

interface ParsedValue {
  value: number;
  isPercentage: boolean;
}

class PriceExpressionParser {
  private index = 0;

  constructor(private readonly expression: string) {}

  parse(): number {
    const value = this.parseAdditionAndSubtraction();
    if (this.index !== this.expression.length) throw new Error('Check the calculation');
    return value;
  }

  private parseAdditionAndSubtraction(): number {
    let value = this.parseMultiplicationAndDivision().value;

    while (this.index < this.expression.length) {
      const operator = this.expression[this.index];
      if (operator !== '+' && operator !== '-') return value;
      this.index += 1;
      const right = this.parseMultiplicationAndDivision();
      const operand = right.isPercentage ? value * right.value : right.value;
      value = operator === '+' ? value + operand : value - operand;
    }

    return value;
  }

  private parseMultiplicationAndDivision(): ParsedValue {
    let parsed = this.parseSignedValue();

    while (this.index < this.expression.length) {
      const operator = this.expression[this.index];
      if (operator !== '*' && operator !== '/') return parsed;
      this.index += 1;
      const right = this.parseSignedValue().value;
      if (operator === '/' && Math.abs(right) <= DIVISION_EPSILON) {
        throw new Error('Cannot divide by zero');
      }
      parsed = { value: operator === '*' ? parsed.value * right : parsed.value / right, isPercentage: false };
    }

    return parsed;
  }

  private parseSignedValue(): ParsedValue {
    let negative = false;
    if (this.expression[this.index] === '-') {
      negative = true;
      this.index += 1;
    }

    let value = this.parseNumber();
    if (negative) value = -value;

    let isPercentage = false;
    while (this.expression[this.index] === '%') {
      value /= 100;
      isPercentage = true;
      this.index += 1;
    }
    return { value, isPercentage };
  }

  private parseNumber(): number {
    const start = this.index;
    let decimalSeen = false;

    while (this.index < this.expression.length) {
      const character = this.expression[this.index];
      if (/\d/.test(character)) {
        this.index += 1;
      } else if (character === '.' && !decimalSeen) {
        decimalSeen = true;
        this.index += 1;
      } else {
        break;
      }
    }

    if (this.index === start) throw new Error('Check the calculation');
    const value = Number(this.expression.slice(start, this.index));
    if (!Number.isFinite(value)) throw new Error('Check the calculation');
    return value;
  }
}
