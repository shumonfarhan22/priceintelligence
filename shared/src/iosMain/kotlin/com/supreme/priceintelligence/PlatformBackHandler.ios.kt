@file:OptIn(
    kotlinx.cinterop.BetaInteropApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class
)

package com.supreme.priceintelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIRectEdgeLeft
import platform.UIKit.UIScreenEdgePanGestureRecognizer
import platform.UIKit.UIViewController
import platform.darwin.NSObject

/**
 * The Compose controller is registered after creation so iOS navigation can
 * use UIKit's edge-pan recognizer instead of competing with scrollable Compose
 * content through a pointer-input approximation.
 */
internal object IosNativeBackGestureHost {
    var controller: UIViewController? by mutableStateOf(null)
}

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    val currentOnBack = rememberUpdatedState(onBack)
    val controller = IosNativeBackGestureHost.controller

    DisposableEffect(enabled, controller) {
        val rootView = controller?.view

        if (!enabled || rootView == null) {
            onDispose { }
        } else {
            val target = NativeBackGestureTarget {
                currentOnBack.value.invoke()
            }

            val recognizer =
                UIScreenEdgePanGestureRecognizer(
                    target = target,
                    action = NSSelectorFromString(
                        "handleEdgePan:"
                    )
                ).apply {
                    edges = UIRectEdgeLeft
                    cancelsTouchesInView = false
                }

            rootView.addGestureRecognizer(recognizer)

            onDispose {
                rootView.removeGestureRecognizer(recognizer)
                target.dispose()
            }
        }
    }
}

@Composable
actual fun Modifier.platformBackSwipe(
    enabled: Boolean,
    onBack: () -> Unit
): Modifier = this

private class NativeBackGestureTarget(
    onBack: () -> Unit
) : NSObject() {
    private var onBack: (() -> Unit)? = onBack

    fun dispose() {
        onBack = null
    }

    @ObjCAction
    fun handleEdgePan(
        recognizer: UIScreenEdgePanGestureRecognizer
    ) {
        if (
            recognizer.state !=
            UIGestureRecognizerStateEnded
        ) {
            return
        }

        val view = recognizer.view ?: return
        val translation =
            recognizer.translationInView(view)
                .useContents { x }
        val velocity =
            recognizer.velocityInView(view)
                .useContents { x }
        val viewWidth = view.bounds.useContents {
            size.width
        }
        val requiredDistance =
            (viewWidth * 0.22)
                .coerceIn(64.0, 104.0)

        if (
            translation >= requiredDistance ||
            velocity >= 720.0
        ) {
            onBack?.invoke()
        }
    }
}
