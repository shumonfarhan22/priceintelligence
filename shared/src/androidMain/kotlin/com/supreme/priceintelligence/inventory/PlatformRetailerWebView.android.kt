package com.supreme.priceintelligence.inventory

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun PlatformRetailerWebView(
    initialUrl: String,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val urlCallback =
        rememberUpdatedState(onUrlChanged)
    val loadingCallback =
        rememberUpdatedState(onLoadingChanged)

    val webView = remember(initialUrl) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.cacheMode =
                WebSettings.LOAD_NO_CACHE
            settings.javaScriptCanOpenWindowsAutomatically =
                false
            settings.setSupportMultipleWindows(true)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode =
                WebSettings.MIXED_CONTENT_NEVER_ALLOW

            settings.userAgentString =
                settings.userAgentString
                    .replace("; wv", "")
                    .replace("Version/4.0 ", "")

            val cookieManager =
                CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(
                this,
                true
            )

            clearCache(true)
            clearHistory()
            WebStorage.getInstance().deleteAllData()

            webChromeClient =
                object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        val mainWebView =
                            view ?: return false
                        val message =
                            resultMsg ?: return false
                        val transport = message.obj
                            as? WebView.WebViewTransport
                            ?: return false

                        if (!isUserGesture) {
                            return false
                        }

                        val popupWebView =
                            WebView(mainWebView.context)
                        var destinationRouted = false

                        fun routeToMainWebView(
                            destination: Uri?
                        ): Boolean {
                            if (
                                destinationRouted ||
                                destination == null ||
                                !destination.isSafeBrowserScheme()
                            ) {
                                return false
                            }

                            destinationRouted = true
                            mainWebView.loadUrl(
                                destination.toString()
                            )
                            popupWebView.stopLoading()
                            popupWebView.post {
                                popupWebView.destroy()
                            }
                            return true
                        }

                        popupWebView.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode =
                                WebSettings.LOAD_NO_CACHE
                        }

                        popupWebView.webViewClient =
                            object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    popup: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean =
                                    routeToMainWebView(
                                        request?.url
                                    )

                                @Deprecated(
                                    "Used on Android versions before API 24"
                                )
                                override fun shouldOverrideUrlLoading(
                                    popup: WebView?,
                                    url: String?
                                ): Boolean =
                                    routeToMainWebView(
                                        url?.let(Uri::parse)
                                    )

                                override fun onPageStarted(
                                    popup: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    routeToMainWebView(
                                        url?.let(Uri::parse)
                                    )
                                }
                            }

                        transport.webView = popupWebView
                        message.sendToTarget()
                        return true
                    }
                }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {
                settings.safeBrowsingEnabled = true
            }

            webViewClient =
                object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val requestedUrl =
                            request?.url ?: return true

                        return !requestedUrl
                            .isSafeBrowserScheme()
                    }

                    @Deprecated(
                        "Used on Android versions before API 24"
                    )
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        url: String?
                    ): Boolean {
                        val requestedUrl = url
                            ?.let(Uri::parse)
                            ?: return true

                        return !requestedUrl
                            .isSafeBrowserScheme()
                    }

                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: Bitmap?
                    ) {
                        url?.let(urlCallback.value)
                        loadingCallback.value(true)
                    }

                    override fun onPageFinished(
                        view: WebView?,
                        url: String?
                    ) {
                        url?.let(urlCallback.value)
                        loadingCallback.value(false)
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean
                    ) {
                        url?.let(urlCallback.value)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (
                            request?.isForMainFrame != false
                        ) {
                            loadingCallback.value(false)
                        }
                    }
                }

            cookieManager.removeAllCookies {
                loadUrl(initialUrl)
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.clearCache(true)
            webView.webViewClient =
                WebViewClient()
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().apply {
                removeAllCookies(null)
                flush()
            }
            webView.destroy()
        }
    }

    AndroidView(
        factory = {
            webView
        },
        modifier = modifier
    )
}

private fun Uri.isSafeBrowserScheme(): Boolean =
    scheme.equals(
        other = "https",
        ignoreCase = true
    ) ||
            scheme.equals(
                other = "http",
                ignoreCase = true
            )