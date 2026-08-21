package com.supreme.priceintelligence.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PricePageParserTest {
    @Test
    fun readsAmazonProductFromJsonLdGraph() {
        val html = """
            <html><head>
            <script type="application/ld+json">
            {
              "@context": "https://schema.org",
              "@graph": [
                {
                  "@type": "Product",
                  "image": ["https://images.example/phone.jpg"],
                  "offers": {
                    "@type": "Offer",
                    "price": "12,499.00"
                  }
                }
              ]
            }
            </script>
            </head></html>
        """.trimIndent()

        val result = PricePageParser.parse(html, "https://www.amazon.in/example")

        assertEquals(12499.0, result.price)
        assertEquals("https://images.example/phone.jpg", result.image)
    }

    @Test
    fun readsOfferArrayAndPriceSpecification() {
        val html = """
            <script type="application/ld+json">
            {
              "@type": ["Thing", "Product"],
              "image": {"contentUrl": "https://images.example/item.png"},
              "offers": [
                {"priceSpecification": {"price": "₹899"}}
              ]
            }
            </script>
        """.trimIndent()

        val result = PricePageParser.parse(html, "https://www.flipkart.com/example")

        assertEquals(899.0, result.price)
        assertEquals("https://images.example/item.png", result.image)
    }

    @Test
    fun usesAmazonHtmlFallbackWhenStructuredDataIsBroken() {
        val html = """
            <html>
              <script type="application/ld+json">{broken json</script>
              <img id="landingImage"
                   src="/small.jpg"
                   data-a-dynamic-image='{"https://images.example/large.jpg":[1200,1200]}' />
              <span class="a-price"><span class="a-offscreen">₹1,299.50</span></span>
            </html>
        """.trimIndent()

        val result = PricePageParser.parse(html, "https://www.amazon.in/product/1")

        assertEquals(1299.5, result.price)
        assertEquals("https://images.example/large.jpg", result.image)
    }

    @Test
    fun usesFlipkartHtmlFallback() {
        val html = """
            <html>
              <img class="DByuf4" src="https://images.example/flipkart.jpg" />
              <div class="Nx9bqj">₹2,599</div>
            </html>
        """.trimIndent()

        val result = PricePageParser.parse(html, "https://www.flipkart.com/product/1")

        assertEquals(2599.0, result.price)
        assertEquals("https://images.example/flipkart.jpg", result.image)
    }

    @Test
    fun flagsBlockedWhenPageIsACaptchaChallenge() {
        val html = """
            <html><head><title>Robot Check</title></head>
            <body>Enter the characters you see below</body></html>
        """.trimIndent()

        val result = PricePageParser.parse(html, "https://www.amazon.in/product/2")

        assertEquals(true, result.blocked)
        assertNull(result.price)
    }

    @Test
    fun doesNotFlagBlockedForAnOrdinaryLongPageMentioningRobot() {
        val longBody = "This robot vacuum cleaner review page. ".repeat(40)
        val html = "<html><body>$longBody</body></html>"

        val result = PricePageParser.parse(html, "https://example.com/review")

        assertEquals(false, result.blocked)
    }

    @Test
    fun usesMetaTagFallbackWhenStructuredDataAndKnownSelectorsAreMissing() {
        val html = """
            <html><head>
              <meta property="product:price:amount" content="4,499.00" />
              <meta property="og:image" content="https://images.example/meta.jpg" />
            </head><body>No matching CSS selector on this page</body></html>
        """.trimIndent()

        val result = PricePageParser.parse(html, "https://www.amazon.in/product/3")

        assertEquals(4499.0, result.price)
        assertEquals("https://images.example/meta.jpg", result.image)
    }

    @Test
    fun returnsEmptyResultForBlankOrUnrecognizedPage() {
        assertEquals(ScrapeResult(), PricePageParser.parse("", "https://www.amazon.in/product"))

        val result = PricePageParser.parse("<html><body>Unavailable</body></html>", "https://example.com")
        assertNull(result.price)
        assertNull(result.image)
    }
}
