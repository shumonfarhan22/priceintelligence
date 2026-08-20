package com.supreme.priceintelligence.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Suppress("SpellCheckingInspection")
actual class PriceScraper : PriceFetcher {
    private val amazonUserAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile"
    )

    private val client = HttpClient(Darwin)

    actual override suspend fun fetchPrice(url: String): ScrapeResult {
        if (url.isBlank()) return ScrapeResult()

        val secureUrl = ensureHttps(url)
        val isFlipkart = secureUrl.lowercase().contains("flipkart")

        for (attempt in 1..2) {
            try {
                val response: HttpResponse = client.get(secureUrl) {
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
                    response.status == HttpStatusCode.TooManyRequests ||
                        response.status == HttpStatusCode.ServiceUnavailable -> Unit
                    !response.status.isSuccess() -> return ScrapeResult()
                    else -> return PricePageParser.parse(response.bodyAsText(), secureUrl)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (attempt == 2) return ScrapeResult()
            }

            delay(1500.milliseconds)
        }

        return ScrapeResult()
    }

    actual fun close() {
        client.close()
    }
}
