package com.supreme.priceintelligence.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlin.time.Duration.Companion.milliseconds

/**
 * iOS counterpart to the Android PriceScraper — same JSON-LD-first, CSS-fallback
 * strategy, just speaking Ktor instead of OkHttp and Ksoup instead of Jsoup.
 */
@Suppress("SpellCheckingInspection")
actual class PriceScraper {

    private val amazonUserAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile"
    )

    private val client = HttpClient(Darwin)

    actual suspend fun fetchPrice(url: String): ScrapeResult {
        if (url.isBlank()) return ScrapeResult()

        val isFlipkart = url.lowercase().contains("flipkart")
        val maxAttempts = 2

        for (attempt in 1..maxAttempts) {
            try {
                val response: HttpResponse = client.get(url) {
                    headers {
                        if (isFlipkart) {
                            append("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                            append("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        } else {
                            append("User-Agent", amazonUserAgents.random())
                            append("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                            append("Accept-Language", "en-IN,en;q=0.9")
                        }
                    }
                }

                when {
                    response.status == HttpStatusCode.ServiceUnavailable -> {
                        // Bot-check page — fall through to the retry delay below
                    }
                    !response.status.isSuccess() -> return ScrapeResult()
                    else -> return extractPrice(response.bodyAsText(), url)
                }
            } catch (_: Exception) {
                if (attempt == maxAttempts) return ScrapeResult()
            }

            delay(1500.milliseconds)
        }

        return ScrapeResult()
    }

    private fun extractPrice(html: String, url: String): ScrapeResult {
        val doc = Ksoup.parse(html = html, baseUri = url)
        val fromJsonLd = extractFromJsonLd(doc)
        return extractFallback(doc, url, fromJsonLd)
    }

    // ---------------------------------------------------------
    // 1. JSON-LD extraction (the foolproof SEO method)
    // ---------------------------------------------------------
    private fun extractFromJsonLd(doc: Document): ScrapeResult {
        var price: Double? = null
        var image: String? = null

        for (script in doc.select("script[type=application/ld+json]")) {
            if (price != null) break
            try {
                val root = Json.parseToJsonElement(script.data())
                val items: List<JsonObject> = when (root) {
                    is JsonObject -> listOf(root)
                    is JsonArray -> root.mapNotNull { it as? JsonObject }
                    else -> emptyList()
                }

                for (item in items) {
                    if (item["@type"]?.jsonPrimitive?.contentOrNull != "Product") continue

                    if (image == null && item.containsKey("image")) {
                        image = when (val img = item["image"]) {
                            is JsonArray -> img.firstOrNull()?.jsonPrimitive?.contentOrNull
                            is JsonPrimitive -> img.contentOrNull
                            else -> null
                        }
                    }

                    val offerPrice: Double? = when (val offers = item["offers"]) {
                        is JsonObject -> offers.priceOrNull()
                        is JsonArray -> (offers.firstOrNull() as? JsonObject)?.priceOrNull()
                        else -> null
                    }
                    if (offerPrice != null) {
                        price = offerPrice
                        break
                    }
                }
            } catch (_: Exception) {
                continue
            }
        }
        return ScrapeResult(price = price, image = image)
    }

    // ---------------------------------------------------------
    // 2. Fallback HTML scraping (if JSON-LD is missing)
    // ---------------------------------------------------------
    private fun extractFallback(doc: Document, url: String, existing: ScrapeResult): ScrapeResult {
        var price = existing.price
        var image = existing.image
        val lowerUrl = url.lowercase()
        val priceCleanupRegex = Regex("[^\\d.]")

        if (lowerUrl.contains("amazon")) {
            if (image == null) {
                val imgElem = doc.selectFirst("img#landingImage, img#imgBlkFront, div#imgTagWrapperId img")

                val dynamicImageAttr = imgElem?.attr("data-a-dynamic-image")
                if (!dynamicImageAttr.isNullOrBlank()) {
                    try {
                        val json = Json.parseToJsonElement(dynamicImageAttr).jsonObject
                        image = json.keys.firstOrNull()
                    } catch (_: Exception) {}
                }

                if (image == null) {
                    image = imgElem?.attr("abs:src")?.ifBlank { null }
                        ?: imgElem?.attr("abs:data-old-hires")?.ifBlank { null }
                }
            }
            if (price == null) {
                for (sel in listOf("span.a-price-whole", "span#priceblock_ourprice", "span.apexPriceToPay span.a-offscreen")) {
                    val elem = doc.selectFirst(sel) ?: continue
                    val clean = priceCleanupRegex.replace(elem.text().replace(",", ""), "")
                    if (clean.isNotEmpty()) price = clean.toDoubleOrNull()
                    break
                }
            }
        } else if (lowerUrl.contains("flipkart")) {
            if (image == null) {
                val imgElem = doc.selectFirst("img._396cs4, div.CXW8mj img, img.DByuf4, img.vLrBgc")
                image = imgElem?.attr("abs:src")?.ifBlank { null }
            }
            if (price == null) {
                for (sel in listOf("div.Nx9bqj", "div._30jeq3", "div.CEmiEU", "span.CEmiEU")) {
                    val elem = doc.selectFirst(sel) ?: continue
                    val clean = priceCleanupRegex.replace(elem.text().replace(",", ""), "")
                    if (clean.isNotEmpty()) price = clean.toDoubleOrNull()
                    break
                }
            }
        }

        return ScrapeResult(price = price, image = image)
    }

    private fun JsonObject.priceOrNull(): Double? =
        this["price"]?.jsonPrimitive?.doubleOrNull
}