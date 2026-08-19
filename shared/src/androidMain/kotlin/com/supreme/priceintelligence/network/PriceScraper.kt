package com.supreme.priceintelligence.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Ported from api_client.py's ScraperClient. Fetches a product page from
 * Amazon or Flipkart and extracts price + image, preferring JSON-LD (the
 * reliable, SEO-oriented data block every product page publishes) and
 * falling back to hand-picked CSS selectors if that's missing.
 */
@Suppress("SpellCheckingInspection")
actual class PriceScraper {

    // Traffic cop: limits us to 10 total active network calls, and max 3 per website
    private val trafficDispatcher = Dispatcher().apply {
        maxRequests = 10
        maxRequestsPerHost = 3
    }

    private val amazonUserAgents = listOf(
        // Modern Desktop User-Agents are much less likely to be blocked or served captchas
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        // Keep a mobile one as a fallback just in case
        "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile"
    )

    // Two separate clients: Flipkart is restricted to HTTP/1.1 because it
    // sometimes flags HTTP/2 Googlebot-flavored traffic as suspicious.
    private val amazonClient = OkHttpClient.Builder()
        .dispatcher(trafficDispatcher)
        .followRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val flipkartClient = OkHttpClient.Builder()
        .dispatcher(trafficDispatcher)
        .followRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .protocols(listOf(Protocol.HTTP_1_1))
        .build()

    actual suspend fun fetchPrice(url: String): ScrapeResult = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext ScrapeResult()

        val isFlipkart = url.lowercase().contains("flipkart")
        val client = if (isFlipkart) flipkartClient else amazonClient
        val maxAttempts = 2

        for (attempt in 1..maxAttempts) {
            val request = Request.Builder().url(url).headers(buildHeaders(isFlipkart)).build()

            try {
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 503 -> {
                            // Bot-check page — fall through to the retry delay below
                        }
                        !response.isSuccessful -> return@withContext ScrapeResult()
                        else -> {
                            // We are going back to the bucket method for maximum speed!
                            val html = response.body.string()
                            return@withContext extractPrice(html, url)
                        }
                    }
                }
            } catch (_: Exception) {
                if (attempt == maxAttempts) return@withContext ScrapeResult()
            }

            delay(1500.milliseconds)
        }

        ScrapeResult()
    }

    private fun buildHeaders(isFlipkart: Boolean): Headers {
        return if (isFlipkart) {
            Headers.Builder()
                .add("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()
        } else {
            Headers.Builder()
                .add("User-Agent", amazonUserAgents.random())
                .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .add("Accept-Language", "en-IN,en;q=0.9")
                .build()
        }
    }

    private fun extractPrice(html: String, url: String): ScrapeResult {
        val doc = Jsoup.parse(html, url)
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
                val root = JSONTokener(script.data()).nextValue()
                val items: List<JSONObject> = when (root) {
                    is JSONObject -> listOf(root)
                    is JSONArray -> (0 until root.length()).mapNotNull { root.optJSONObject(it) }
                    else -> emptyList()
                }

                for (item in items) {
                    if (item.optString("@type") != "Product") continue

                    if (image == null && item.has("image")) {
                        val img = item.opt("image")
                        image = when (img) {
                            is JSONArray -> if (img.length() > 0) img.optString(0) else null
                            is String -> img
                            else -> null
                        }
                    }

                    val offerPrice: Double? = when (val offers = item.opt("offers")) {
                        is JSONObject -> offers.priceOrNull()
                        is JSONArray -> offers.optJSONObject(0)?.priceOrNull()
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
                // Added img#imgBlkFront to support Amazon Book layouts
                val imgElem = doc.selectFirst("img#landingImage, img#imgBlkFront, div#imgTagWrapperId img")

                // Amazon hides high-res images inside a special JSON map attribute to stop simple scrapers!
                val dynamicImageAttr = imgElem?.attr("data-a-dynamic-image")
                if (!dynamicImageAttr.isNullOrBlank()) {
                    try {
                        // Parses {"https://m.media-amazon.com/images/I/71xyz.jpg":[1000,1000]} and extracts the URL key
                        val json = org.json.JSONObject(dynamicImageAttr)
                        val keys = json.keys()
                        if (keys.hasNext()) {
                            image = keys.next()
                        }
                    } catch (_: Exception) {}
                }

                // If they didn't use the JSON map today, fall back to standard attributes
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

    private fun JSONObject.priceOrNull(): Double? =
        if (has("price")) optDouble("price").takeIf { !it.isNaN() } else null
}