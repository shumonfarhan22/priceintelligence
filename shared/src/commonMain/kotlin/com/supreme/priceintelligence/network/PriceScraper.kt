package com.supreme.priceintelligence.network

expect class PriceScraper() {
    suspend fun fetchPrice(url: String): ScrapeResult
}