package com.supreme.priceintelligence.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@Suppress("SpellCheckingInspection")
actual class PriceScraper : PriceFetcher {
    private val trafficDispatcher = Dispatcher().apply {
        maxRequests = 10
        maxRequestsPerHost = 3
    }

    private val amazonUserAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile"
    )

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

    actual override suspend fun fetchPrice(url: String): ScrapeResult = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext ScrapeResult()

        val secureUrl = ensureHttps(url)
        val isFlipkart = secureUrl.lowercase().contains("flipkart")
        val client = if (isFlipkart) flipkartClient else amazonClient

        for (attempt in 1..2) {
            try {
                val request = Request.Builder()
                    .url(secureUrl)
                    .headers(buildHeaders(isFlipkart))
                    .build()

                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 429 || response.code == 503 -> Unit
                        !response.isSuccessful -> return@withContext ScrapeResult()
                        else -> return@withContext PricePageParser.parse(
                            html = response.body.string(),
                            url = secureUrl
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (attempt == 2) return@withContext ScrapeResult()
            }

            delay(1500.milliseconds)
        }

        ScrapeResult()
    }

    actual fun close() {
        trafficDispatcher.cancelAll()
        trafficDispatcher.executorService.shutdown()
        amazonClient.connectionPool.evictAll()
        flipkartClient.connectionPool.evictAll()
    }

    private fun buildHeaders(isFlipkart: Boolean): Headers =
        if (isFlipkart) {
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
