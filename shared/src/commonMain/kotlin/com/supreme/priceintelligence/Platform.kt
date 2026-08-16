package com.supreme.priceintelligence

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform