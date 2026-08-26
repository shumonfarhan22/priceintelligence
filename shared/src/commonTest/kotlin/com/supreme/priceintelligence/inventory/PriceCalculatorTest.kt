package com.supreme.priceintelligence.inventory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PriceCalculatorTest {

    @Test
    fun calculatorUsesNormalOperatorPrecedence() {
        assertEquals(
            250.0,
            evaluatePriceExpression(
                "100+50×3"
            ).getOrThrow()
        )
    }

    @Test
    fun calculatorSupportsPercentagesAndDecimals() {
        assertEquals(
            1250.0,
            evaluatePriceExpression(
                "1000+25%"
            ).getOrThrow()
        )

        assertEquals(
            74.63,
            evaluatePriceExpression(
                "99.5×0.75"
            ).getOrThrow()
        )
    }

    @Test
    fun calculatorRejectsDivisionByZero() {
        assertTrue(
            evaluatePriceExpression(
                "100÷0"
            ).isFailure
        )
    }

    @Test
    fun calculatorFormatsCurrencyWithoutUnneededZeroes() {
        assertEquals(
            "125",
            formatCalculatorValue(125.0)
        )

        assertEquals(
            "125.5",
            formatCalculatorValue(125.50)
        )

        assertEquals(
            "125.55",
            formatCalculatorValue(125.549)
        )
    }

    @Test
    fun keypadReplacesAdjacentOperatorsAndAvoidsTwoDecimals() {
        assertEquals(
            "12×",
            appendCalculatorKey(
                expression = "12+",
                key = "×"
            )
        )

        assertEquals(
            "12.5",
            appendCalculatorKey(
                expression = "12.5",
                key = "."
            )
        )
    }
}