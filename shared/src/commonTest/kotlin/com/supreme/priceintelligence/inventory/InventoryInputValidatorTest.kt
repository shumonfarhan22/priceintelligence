package com.supreme.priceintelligence.inventory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InventoryInputValidatorTest {
    @Test
    fun trimsAndAcceptsValidProduct() {
        val result = validateInventoryInput(
            productName = "  Samsung Galaxy S25  ",
            shopPrice = " 74999.50 ",
            barcode = " 8901234567890 ",
            amazonUrl = " https://www.amazon.in/example ",
            flipkartUrl = " https://dl.flipkart.com/s/example "
        )

        val input = assertNotNull(result.input)
        assertNull(result.errorMessage)
        assertEquals("Samsung Galaxy S25", input.productName)
        assertEquals(74999.5, input.shopPrice)
        assertEquals("8901234567890", input.barcode)
        assertEquals("https://www.amazon.in/example", input.amazonUrl)
        assertEquals("https://dl.flipkart.com/s/example", input.flipkartUrl)
    }

    @Test
    fun requiresProductName() {
        val result = validateInventoryInput("   ", "100", "", "", "")

        assertNull(result.input)
        assertEquals("Product name is required", result.errorMessage)
    }

    @Test
    fun rejectsInvalidAndNonPositivePrices() {
        assertEquals(
            "Enter a valid selling price",
            validateInventoryInput("Phone", "one hundred", "", "", "").errorMessage
        )
        assertEquals(
            "Selling price must be greater than zero",
            validateInventoryInput("Phone", "0", "", "", "").errorMessage
        )
        assertEquals(
            "Selling price must be greater than zero",
            validateInventoryInput("Phone", "-5", "", "", "").errorMessage
        )
    }

    @Test
    fun rejectsLinksFromWrongOrUnsafeDomains() {
        assertEquals(
            "Enter a valid Amazon product link",
            validateInventoryInput(
                "Phone", "100", "", "https://example.com/product", ""
            ).errorMessage
        )
        assertEquals(
            "Enter a valid Flipkart product link",
            validateInventoryInput(
                "Phone", "100", "", "", "javascript:alert(1)"
            ).errorMessage
        )
    }

    @Test
    fun acceptsAmazonShortLinksAndBlankOptionalFields() {
        val result = validateInventoryInput(
            productName = "Book",
            shopPrice = "299",
            barcode = "",
            amazonUrl = "https://amzn.in/d/example",
            flipkartUrl = ""
        )

        val input = assertNotNull(result.input)
        assertNull(input.barcode)
        assertEquals("https://amzn.in/d/example", input.amazonUrl)
        assertNull(input.flipkartUrl)
    }

    @Test
    fun upgradesOldHttpLinksToHttps() {
        val result = validateInventoryInput(
            productName = "Book",
            shopPrice = "299",
            barcode = "",
            amazonUrl = "http://amazon.in/example",
            flipkartUrl = "http://www.flipkart.com/example"
        )

        val input = assertNotNull(result.input)
        assertEquals("https://amazon.in/example", input.amazonUrl)
        assertEquals("https://www.flipkart.com/example", input.flipkartUrl)
    }
}
