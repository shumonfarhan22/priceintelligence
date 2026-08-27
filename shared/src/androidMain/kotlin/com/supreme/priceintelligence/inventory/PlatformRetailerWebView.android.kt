package com.supreme.priceintelligence.inventory

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.roundToInt

@Composable
internal actual fun PlatformRetailerWebView(
    initialUrl: String,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onUseLink: (String) -> Unit,
    onBrowserClosed: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) {
        context.findActivity()
    }

    val useLinkCallback =
        rememberUpdatedState(onUseLink)
    val browserClosedCallback =
        rememberUpdatedState(onBrowserClosed)
    val loadingCallback =
        rememberUpdatedState(onLoadingChanged)

    val providerPackage = remember(context) {
        findEphemeralBrowserProvider(context)
    }

    val resultAction = remember(initialUrl) {
        "${context.packageName}.USE_RETAILER_LINK.${System.nanoTime()}"
    }

    var browserLaunched by remember(initialUrl) {
        mutableStateOf(false)
    }
    var activityPausedForBrowser by remember(initialUrl) {
        mutableStateOf(false)
    }
    var linkSelected by remember(initialUrl) {
        mutableStateOf(false)
    }

    val resultReceiver = remember(resultAction) {
        object : BroadcastReceiver() {
            override fun onReceive(
                receiverContext: Context?,
                intent: Intent?
            ) {
                val selectedUrl =
                    intent?.dataString
                        ?.takeIf { value ->
                            value.startsWith(
                                prefix = "https://",
                                ignoreCase = true
                            )
                        }
                        ?: return

                linkSelected = true
                useLinkCallback.value(selectedUrl)
                context.bringApplicationToFront()
            }
        }
    }

    DisposableEffect(
        context,
        resultAction,
        resultReceiver
    ) {
        ContextCompat.registerReceiver(
            context,
            resultReceiver,
            IntentFilter(resultAction),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            runCatching {
                context.unregisterReceiver(resultReceiver)
            }
        }
    }

    DisposableEffect(activity) {
        val lifecycleOwner =
            activity as? LifecycleOwner

        val observer = LifecycleEventObserver {
                _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (browserLaunched) {
                        activityPausedForBrowser = true
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (
                        browserLaunched &&
                        activityPausedForBrowser &&
                        !linkSelected
                    ) {
                        browserLaunched = false
                        activityPausedForBrowser = false
                        browserClosedCallback.value()
                    }
                }

                else -> Unit
            }
        }

        lifecycleOwner?.lifecycle?.addObserver(observer)

        onDispose {
            lifecycleOwner?.lifecycle
                ?.removeObserver(observer)
        }
    }

    val toolbarColor =
        MaterialTheme.colorScheme.surface.toArgb()
    val navigationColor =
        MaterialTheme.colorScheme.background.toArgb()

    LaunchedEffect(
        initialUrl,
        providerPackage,
        resultAction
    ) {
        val provider = providerPackage

        if (provider == null) {
            loadingCallback.value(false)
            Toast.makeText(
                context,
                "Private browser needs Chrome 137 or newer. Please update Chrome.",
                Toast.LENGTH_LONG
            ).show()
            browserClosedCallback.value()
            return@LaunchedEffect
        }

        val actionIntent = Intent(resultAction)
            .setPackage(context.packageName)

        val actionPendingIntent =
            PendingIntent.getBroadcast(
                context,
                resultAction.hashCode(),
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_MUTABLE
            )

        val colorParams =
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolbarColor)
                .setNavigationBarColor(navigationColor)
                .setNavigationBarDividerColor(
                    navigationColor
                )
                .build()

        val initialHeight =
            (context.resources.displayMetrics.heightPixels *
                    0.94f).roundToInt()

        val customTab =
            CustomTabsIntent.Builder()
                .setEphemeralBrowsingEnabled(true)
                .setDefaultColorSchemeParams(colorParams)
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .setShareState(
                    CustomTabsIntent.SHARE_STATE_OFF
                )
                .setBookmarksButtonEnabled(false)
                .setDownloadButtonEnabled(false)
                .setOpenInBrowserButtonState(
                    CustomTabsIntent.OPEN_IN_BROWSER_STATE_OFF
                )
                .setCloseButtonPosition(
                    CustomTabsIntent.CLOSE_BUTTON_POSITION_START
                )
                .setInitialActivityHeightPx(
                    initialHeight,
                    CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE
                )
                .setToolbarCornerRadiusDp(16)
                .setActionButton(
                    createCheckIcon(context),
                    "Use this product link",
                    actionPendingIntent,
                    true
                )
                .build()
                .apply {
                    intent.setPackage(provider)
                }

        runCatching {
            browserLaunched = true
            loadingCallback.value(false)
            customTab.launchUrl(
                context,
                Uri.parse(initialUrl)
            )
        }.onFailure {
            browserLaunched = false
            Toast.makeText(
                context,
                "The private browser could not be opened.",
                Toast.LENGTH_LONG
            ).show()
            browserClosedCallback.value()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Opening private browser…",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun findEphemeralBrowserProvider(
    context: Context
): String? {
    val candidates = buildList {
        CustomTabsClient.getPackageName(
            context,
            null
        )?.let(::add)

        add("com.android.chrome")
        add("com.chrome.beta")
        add("com.chrome.dev")
    }.distinct()

    return candidates.firstOrNull { packageName ->
        runCatching {
            CustomTabsClient.isEphemeralBrowsingSupported(
                context,
                packageName
            )
        }.getOrDefault(false)
    }
}

private fun createCheckIcon(
    context: Context
): Bitmap {
    val density =
        context.resources.displayMetrics.density
    val size =
        (24f * density).roundToInt()
            .coerceAtLeast(24)

    return Bitmap.createBitmap(
        size,
        size,
        Bitmap.Config.ARGB_8888
    ).apply {
        val scale = size / 24f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.7f * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        Canvas(this).drawLines(
            floatArrayOf(
                4.5f * scale,
                12.5f * scale,
                9.5f * scale,
                17.5f * scale,
                19.5f * scale,
                6.5f * scale
            ),
            paint
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper ->
            baseContext.findActivity()
        else -> null
    }

private fun Context.bringApplicationToFront() {
    packageManager
        .getLaunchIntentForPackage(packageName)
        ?.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        ?.let(::startActivity)
}