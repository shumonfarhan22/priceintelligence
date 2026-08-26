package com.supreme.priceintelligence.inventory

import kotlin.math.absoluteValue
import kotlin.math.roundToLong

internal fun evaluatePriceExpression(
    expression: String
): Result<Double> = runCatching {
    val normalized = expression
        .replace('×', '*')
        .replace('÷', '/')
        .replace(" ", "")

    require(normalized.isNotBlank()) {
        "Enter a calculation"
    }

    val parser = PriceExpressionParser(normalized)
    val result = parser.parse()

    require(result.isFinite()) {
        "The result is too large"
    }
    require(result.absoluteValue <= MAX_CALCULATOR_VALUE) {
        "The result is too large"
    }

    roundToCurrency(result)
}

internal fun formatCalculatorValue(value: Double): String {
    val roundedCents = (roundToCurrency(value) * 100.0).roundToLong()
    val sign = if (roundedCents < 0L) "-" else ""
    val positiveCents = roundedCents.absoluteValue
    val whole = positiveCents / 100L
    val cents = positiveCents % 100L

    return if (cents == 0L) {
        "$sign$whole"
    } else {
        val fraction = cents
            .toString()
            .padStart(2, '0')
            .trimEnd('0')
        "$sign$whole.$fraction"
    }
}

private fun roundToCurrency(value: Double): Double =
    (value * 100.0).roundToLong() / 100.0

private class PriceExpressionParser(
    private val expression: String
) {
    private var index = 0

    fun parse(): Double {
        val value = parseAdditionAndSubtraction()
        require(index == expression.length) {
            "Check the calculation"
        }
        return value
    }

    private fun parseAdditionAndSubtraction(): Double {
        var value = parseMultiplicationAndDivision().value

        while (index < expression.length) {
            value = when (expression[index]) {
                '+' -> {
                    index += 1
                    val right = parseMultiplicationAndDivision()
                    value + if (right.isPercentage) {
                        value * right.value
                    } else {
                        right.value
                    }
                }

                '-' -> {
                    index += 1
                    val right = parseMultiplicationAndDivision()
                    value - if (right.isPercentage) {
                        value * right.value
                    } else {
                        right.value
                    }
                }

                else -> return value
            }
        }

        return value
    }

    private fun parseMultiplicationAndDivision(): ParsedCalculatorValue {
        var parsedValue = parseSignedValue()

        while (index < expression.length) {
            parsedValue = when (expression[index]) {
                '*' -> {
                    index += 1
                    ParsedCalculatorValue(
                        value = parsedValue.value * parseSignedValue().value,
                        isPercentage = false
                    )
                }

                '/' -> {
                    index += 1
                    val divisor = parseSignedValue().value
                    require(divisor.absoluteValue > DIVISION_EPSILON) {
                        "Cannot divide by zero"
                    }
                    ParsedCalculatorValue(
                        value = parsedValue.value / divisor,
                        isPercentage = false
                    )
                }

                else -> return parsedValue
            }
        }

        return parsedValue
    }

    private fun parseSignedValue(): ParsedCalculatorValue {
        val negative = if (
            index < expression.length &&
            expression[index] == '-'
        ) {
            index += 1
            true
        } else {
            false
        }

        var value = parseNumber()
        if (negative) {
            value = -value
        }

        var isPercentage = false
        while (
            index < expression.length &&
            expression[index] == '%'
        ) {
            value /= 100.0
            isPercentage = true
            index += 1
        }

        return ParsedCalculatorValue(
            value = value,
            isPercentage = isPercentage
        )
    }

    private fun parseNumber(): Double {
        val start = index
        var decimalSeen = false

        while (index < expression.length) {
            val character = expression[index]
            when {
                character.isDigit() -> index += 1
                character == '.' && !decimalSeen -> {
                    decimalSeen = true
                    index += 1
                }

                else -> break
            }
        }

        require(index > start) {
            "Check the calculation"
        }

        return expression
            .substring(start, index)
            .toDoubleOrNull()
            ?: error("Check the calculation")
    }
}

private data class ParsedCalculatorValue(
    val value: Double,
    val isPercentage: Boolean
)

private const val MAX_CALCULATOR_VALUE = 999_999_999.99
private const val DIVISION_EPSILON = 0.000_000_1