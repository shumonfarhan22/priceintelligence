package com.supreme.priceintelligence.inventory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore

@OptIn(
    ExperimentalForeignApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
internal actual fun PlatformRetailerWebView(
    initialUrl: String,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    modifier: Modifier
) {
    val urlCallback =
        rememberUpdatedState(onUrlChanged)

    val loadingCallback =
        rememberUpdatedState(onLoadingChanged)

    var activeWebView by remember {
        mutableStateOf<WKWebView?>(null)
    }

    LaunchedEffect(activeWebView) {
        val webView =
            activeWebView ?: return@LaunchedEffect

        var previousUrl: String? = null
        var previousLoading: Boolean? = null

        while (
            currentCoroutineContext().isActive
        ) {
            val currentUrl =
                webView.URL?.absoluteString

            if (
                !currentUrl.isNullOrBlank() &&
                currentUrl != previousUrl
            ) {
                previousUrl = currentUrl
                urlCallback.value(currentUrl)
            }

            val loading = webView.loading

            if (loading != previousLoading) {
                previousLoading = loading
                loadingCallback.value(loading)
            }

            delay(150.milliseconds)
        }
    }

    UIKitView(
        factory = {
            val configuration =
                WKWebViewConfiguration()

            configuration.websiteDataStore =
                WKWebsiteDataStore.nonPersistentDataStore()

            configuration.defaultWebpagePreferences
                .allowsContentJavaScript = true

            WKWebView(
                frame = CGRectZero.readValue(),
                configuration = configuration
            ).apply {
                val request =
                    NSMutableURLRequest.requestWithURL(
                        URL = NSURL(
                            string = initialUrl
                        )
                    )

                loadRequest(request)
                activeWebView = this
            }
        },
        modifier = modifier,
        onRelease = { webView ->
            webView.stopLoading()

            if (activeWebView === webView) {
                activeWebView = null
            }
        },
        properties = UIKitInteropProperties(
            interactionMode =
                UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true
        )
    )
}