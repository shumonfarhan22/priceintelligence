package com.supreme.priceintelligence.ui.input

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformTextInputTest {
    @Test
    fun decimalInputAllowsIncompleteButPotentiallyValidValues() {
        listOf("", ".", "0", "12", "12.", "12.50").forEach { value ->
            assertTrue(isValidDecimalInput(value), value)
        }

        listOf("12..5", "1,25", "₹12", "12a").forEach { value ->
            assertFalse(isValidDecimalInput(value), value)
        }
    }

    @Test
    fun barcodeInputAcceptsOnlyBoundedDigits() {
        assertTrue(isValidBarcodeInput(""))
        assertTrue(isValidBarcodeInput("8901234567890"))
        assertFalse(isValidBarcodeInput("890-123"))
        assertFalse(isValidBarcodeInput("1".repeat(65)))
    }

    @Test
    fun hexInputAllowsProgressiveTypingWithoutInvalidCharacters() {
        listOf("", "#", "#1", "#10B981", "10b981").forEach { value ->
            assertTrue(isValidHexColorInput(value), value)
        }

        listOf("##10B9", "#10BG81", "#10B9810").forEach { value ->
            assertFalse(isValidHexColorInput(value), value)
        }
    }
}
