@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.supreme.priceintelligence.ui.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
internal actual fun rememberPlatformClipboard(): PlatformClipboard =
    remember {
        IosPlatformClipboard()
    }

private class IosPlatformClipboard : PlatformClipboard {
    override fun readText(): String? =
        UIPasteboard.generalPasteboard.string
}
