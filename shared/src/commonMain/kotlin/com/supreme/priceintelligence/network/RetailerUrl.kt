package com.supreme.priceintelligence.network

internal enum class Retailer {
    AMAZON,
    FLIPKART
}

/** Validates a retailer host and upgrades old HTTP links to HTTPS. */
internal fun normalizeRetailerUrl(
    value: String?,
    retailer: Retailer
): String? {
    val trimmed = value?.trim()?.ifBlank { null } ?: return null
    val lower = trimmed.lowercase()
    val schemeLength = when {
        lower.startsWith("https://") -> 8
        lower.startsWith("http://") -> 7
        else -> return null
    }

    val authority = lower.substring(schemeLength).substringBefore('/')
    if (authority.isBlank() || '@' in authority) return null

    val host = authority.substringBefore(':').trimEnd('.')
    val allowedDomains = when (retailer) {
        Retailer.AMAZON -> listOf("amazon.in", "amazon.com", "amzn.in", "amzn.to")
        Retailer.FLIPKART -> listOf("flipkart.com")
    }
    val isAllowed = allowedDomains.any { domain ->
        host == domain || host.endsWith(".$domain")
    }
    if (!isAllowed) return null

    return if (lower.startsWith("http://")) {
        "https://${trimmed.substring(7)}"
    } else {
        trimmed
    }
}

internal fun ensureHttps(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.startsWith("http://", ignoreCase = true)) {
        "https://${trimmed.substring(7)}"
    } else {
        trimmed
    }
}

internal fun normalizeRemoteImageUrl(value: String?): String? {
    val secureUrl = value?.trim()?.ifBlank { null }?.let(::ensureHttps) ?: return null
    return secureUrl.takeIf { it.startsWith("https://", ignoreCase = true) }
}
