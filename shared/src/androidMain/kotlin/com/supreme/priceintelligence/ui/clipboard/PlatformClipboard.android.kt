package com.supreme.priceintelligence.ui.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberPlatformClipboard(): PlatformClipboard {
    val context = LocalContext.current.applicationContext

    return remember(context) {
        AndroidPlatformClipboard(context)
    }
}

private class AndroidPlatformClipboard(
    private val context: Context
) : PlatformClipboard {
    private val clipboardManager =
        context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as? ClipboardManager

    override fun readText(): String? {
        val clip = clipboardManager
            ?.primaryClip
            ?: return null

        if (clip.itemCount <= 0) {
            return null
        }

        return clip
            .getItemAt(0)
            .coerceToText(context)
            ?.toString()
    }
}
