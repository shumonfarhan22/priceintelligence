package com.supreme.priceintelligence.ui.clipboard

import androidx.compose.runtime.Composable

internal interface PlatformClipboard {
    fun readText(): String?
}

@Composable
internal expect fun rememberPlatformClipboard(): PlatformClipboard
