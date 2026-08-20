package com.supreme.priceintelligence.network

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * One shared parser keeps Android and iPhone price results identical.
 * Network requests remain platform-specific, but the downloaded HTML is
 * interpreted here and can be covered by fast automated tests.
 */
internal object PricePageParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val nonPriceCharacters = Regex("[^\\d.]")

    fun parse(html: String, url: String): ScrapeResult {
        if (html.isBlank()) return ScrapeResult()

        val document = Ksoup.parse(html = html, baseUri = url)
        val structuredResult = extractStructuredData(document)
        return extractHtmlFallback(document, url, structuredResult)
    }

    private fun extractStructuredData(document: Document): ScrapeResult {
        var image: String? = null

        for (script in document.select("script[type=application/ld+json]")) {
            val root = runCatching {
                json.parseToJsonElement(script.data())
            }.getOrNull() ?: continue

            for (product in root.productObjects()) {
                if (image == null) image = product["image"].imageUrlOrNull()

                val price = product["offers"].offerPriceOrNull()
                if (price != null) return ScrapeResult(price = price, image = image)
            }
        }

        return ScrapeResult(image = image)
    }

    private fun extractHtmlFallback(
        document: Document,
        url: String,
        existing: ScrapeResult
    ): ScrapeResult {
        var price = existing.price
        var image = existing.image
        val lowerUrl = url.lowercase()

        if (lowerUrl.contains("amazon") || lowerUrl.contains("amzn.")) {
            val imageElement = document.selectFirst(
                "img#landingImage, img#imgBlkFront, div#imgTagWrapperId img"
            )

            if (image == null) {
                val dynamicImages = imageElement
                    ?.attr("data-a-dynamic-image")
                    ?.takeIf { it.isNotBlank() }

                if (dynamicImages != null) {
                    image = runCatching {
                        json.parseToJsonElement(dynamicImages)
                            .let { it as? JsonObject }
                            ?.keys
                            ?.firstOrNull()
                    }.getOrNull()
                }

                if (image == null) {
                    image = imageElement?.attr("abs:src")?.ifBlank { null }
                        ?: imageElement?.attr("abs:data-old-hires")?.ifBlank { null }
                }
            }

            if (price == null) {
                price = firstPrice(
                    document,
                    listOf(
                        "span.a-price span.a-offscreen",
                        "span.a-price-whole",
                        "span#priceblock_ourprice",
                        "span.apexPriceToPay span.a-offscreen"
                    )
                )
            }
        } else if (lowerUrl.contains("flipkart")) {
            if (image == null) {
                image = document
                    .selectFirst("img._396cs4, div.CXW8mj img, img.DByuf4, img.vLrBgc")
                    ?.attr("abs:src")
                    ?.ifBlank { null }
            }

            if (price == null) {
                price = firstPrice(
                    document,
                    listOf("div.Nx9bqj", "div._30jeq3", "div.CEmiEU", "span.CEmiEU")
                )
            }
        }

        return ScrapeResult(price = price, image = image)
    }

    private fun firstPrice(document: Document, selectors: List<String>): Double? {
        for (selector in selectors) {
            val text = document.selectFirst(selector)?.text() ?: continue
            val parsed = text.toPriceOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    private fun JsonElement.productObjects(): Sequence<JsonObject> = sequence {
        when (this@productObjects) {
            is JsonArray -> for (item in this@productObjects) yieldAll(item.productObjects())
            is JsonObject -> {
                if (this@productObjects.isProductType()) yield(this@productObjects)
                for (child in values) yieldAll(child.productObjects())
            }
            is JsonPrimitive -> Unit
        }
    }

    private fun JsonObject.isProductType(): Boolean = when (val type = this["@type"]) {
        is JsonPrimitive -> type.contentOrNull.equals("Product", ignoreCase = true)
        is JsonArray -> type.any {
            (it as? JsonPrimitive)?.contentOrNull.equals("Product", ignoreCase = true)
        }
        else -> false
    }

    private fun JsonElement?.offerPriceOrNull(): Double? = when (this) {
        is JsonArray -> firstNotNullOfOrNull { it.offerPriceOrNull() }
        is JsonObject -> {
            this["price"].priceValueOrNull()
                ?: this["lowPrice"].priceValueOrNull()
                ?: this["priceSpecification"].offerPriceOrNull()
        }
        else -> null
    }

    private fun JsonElement?.priceValueOrNull(): Double? =
        (this as? JsonPrimitive)?.contentOrNull?.toPriceOrNull()

    private fun JsonElement?.imageUrlOrNull(): String? = when (this) {
        is JsonPrimitive -> contentOrNull?.ifBlank { null }
        is JsonArray -> firstNotNullOfOrNull { it.imageUrlOrNull() }
        is JsonObject -> this["url"].imageUrlOrNull()
            ?: this["contentUrl"].imageUrlOrNull()
        else -> null
    }

    private fun String.toPriceOrNull(): Double? {
        val clean = replace(",", "").replace(nonPriceCharacters, "")
        return clean.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    }
}
