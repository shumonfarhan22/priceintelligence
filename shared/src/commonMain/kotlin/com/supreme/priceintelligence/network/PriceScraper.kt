package com.supreme.priceintelligence.network

interface PriceFetcher {
    suspend fun fetchPrice(url: String): ScrapeResult
}

expect class PriceScraper() : PriceFetcher {
    override suspend fun fetchPrice(url: String): ScrapeResult
    fun close()
}
