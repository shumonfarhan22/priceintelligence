package com.supreme.priceintelligence

import androidx.compose.runtime.Composable
import com.supreme.priceintelligence.network.ensureHttps
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberUrlOpener(): (String) -> Unit {
    return { url ->
        val safeUrl = ensureHttps(url)
        NSURL.URLWithString(safeUrl)
            ?.takeIf { safeUrl.startsWith("https://", ignoreCase = true) }
            ?.let { nsUrl ->
                UIApplication.sharedApplication.openURL(
                    url = nsUrl,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null
                )
            }
    }
}
