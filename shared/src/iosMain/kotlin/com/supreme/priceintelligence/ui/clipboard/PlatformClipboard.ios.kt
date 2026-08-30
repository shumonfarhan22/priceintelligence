@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.supreme.priceintelligence.ui.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

@Composable
internal actual fun rememberPlatformClipboard(): PlatformClipboard =
    remember {
        IosPlatformClipboard()
    }

private class IosPlatformClipboard : PlatformClipboard {
    override fun requestPaste(
        onTextRead: (String) -> Unit
    ) {
        // Compose's native iOS text session owns the focused UITextField.
        // Let UIKit perform its standard paste action so the native editor,
        // selection, and Compose TextFieldState all update through one path.
        val handledByFocusedInput =
            UIApplication.sharedApplication.sendAction(
                action = NSSelectorFromString("paste:"),
                to = null,
                from = null,
                forEvent = null
            )

        // A direct read is only a fallback for a field that is no longer a
        // native first responder (for example during a focus transition).
        if (!handledByFocusedInput) {
            UIPasteboard.generalPasteboard.string
                ?.let(onTextRead)
        }
    }
}
