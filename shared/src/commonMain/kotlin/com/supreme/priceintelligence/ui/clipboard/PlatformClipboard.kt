package com.supreme.priceintelligence.ui.clipboard

import androidx.compose.runtime.Composable

internal interface PlatformClipboard {
    /**
     * Requests a paste from the platform's currently focused text input.
     * Platforms without a native paste action deliver the clipboard text
     * through [onTextRead] instead.
     */
    fun requestPaste(onTextRead: (String) -> Unit)
}

@Composable
internal expect fun rememberPlatformClipboard(): PlatformClipboard
