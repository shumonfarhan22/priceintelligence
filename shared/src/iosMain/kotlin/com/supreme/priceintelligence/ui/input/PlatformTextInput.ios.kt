@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class
)

package com.supreme.priceintelligence.ui.input

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.usingNativeTextInput
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonItemStyle
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UIApplication
import platform.UIKit.UIToolbar

internal actual fun KeyboardOptions.withPlatformTextInput(): KeyboardOptions =
    copy(
        platformImeOptions = PlatformImeOptions {
            usingNativeTextInput(true)
        }
    )

internal actual fun dismissPlatformKeyboard() {
    UIApplication.sharedApplication.sendAction(
        action = NSSelectorFromString("resignFirstResponder"),
        to = null,
        from = null,
        forEvent = null
    )
}

@Composable
internal actual fun rememberPlatformTextInputOptions(
    keyboardOptions: KeyboardOptions,
    accessoryAction: KeyboardAccessoryAction,
    onAccessoryAction: () -> Unit
): KeyboardOptions {
    val currentOnAccessoryAction =
        rememberUpdatedState(onAccessoryAction)
    val target = remember {
        KeyboardAccessoryTarget {
            currentOnAccessoryAction.value()
        }
    }
    val toolbar = remember(accessoryAction, target) {
        if (accessoryAction == KeyboardAccessoryAction.NONE) {
            null
        } else {
            UIToolbar().apply {
                val actionSelector =
                    NSSelectorFromString(target::performAction.name)
                val flexibleSpace = UIBarButtonItem(
                    barButtonSystemItem =
                        UIBarButtonSystemItem
                            .UIBarButtonSystemItemFlexibleSpace,
                    target = null,
                    action = null
                )
                val pasteButton =
                    if (
                        accessoryAction ==
                            KeyboardAccessoryAction.PASTE_NEXT
                    ) {
                        UIBarButtonItem(
                            title = "Paste",
                            style = UIBarButtonItemStyle
                                .UIBarButtonItemStyleDone,
                            target = null,
                            action =
                                NSSelectorFromString("paste:")
                        )
                    } else {
                        null
                    }
                val actionButton =
                    if (accessoryAction == KeyboardAccessoryAction.DONE) {
                        UIBarButtonItem(
                            barButtonSystemItem =
                                UIBarButtonSystemItem
                                    .UIBarButtonSystemItemDone,
                            target = target,
                            action = actionSelector
                        )
                    } else {
                        UIBarButtonItem(
                            title = "Next",
                            style = UIBarButtonItemStyle
                                .UIBarButtonItemStyleDone,
                            target = target,
                            action = actionSelector
                        )
                    }

                items = buildList {
                    pasteButton?.let(::add)
                    add(flexibleSpace)
                    add(actionButton)
                }
                sizeToFit()
            }
        }
    }

    return remember(keyboardOptions, toolbar) {
        keyboardOptions.copy(
            platformImeOptions = PlatformImeOptions {
                usingNativeTextInput(true)
                inputAccessoryView(toolbar)
            }
        )
    }
}

private class KeyboardAccessoryTarget(
    private val onAction: () -> Unit
) : NSObject() {
    @ObjCAction
    fun performAction() {
        onAction()
    }
}
