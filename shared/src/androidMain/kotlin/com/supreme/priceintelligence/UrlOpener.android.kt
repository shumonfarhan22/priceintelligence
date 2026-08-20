package com.supreme.priceintelligence

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.supreme.priceintelligence.network.ensureHttps

@Composable
actual fun rememberUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return { url ->
        val safeUrl = ensureHttps(url)
        if (safeUrl.startsWith("https://", ignoreCase = true)) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, safeUrl.toUri()))
            }
        }
    }
}
