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
import platform.Foundation.NSItemProvider
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonItemStyle
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteConfiguration
import platform.UIKit.UIPasteConfigurationSupportingProtocol
import platform.UIKit.UIPasteControl
import platform.UIKit.UIToolbar
import platform.UniformTypeIdentifiers.UTTypePlainText

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
    onAccessoryAction: () -> Unit,
    onPaste: (String) -> Unit
): KeyboardOptions {
    val currentOnAccessoryAction =
        rememberUpdatedState(onAccessoryAction)
    val currentOnPaste = rememberUpdatedState(onPaste)
    val target = remember {
        KeyboardAccessoryTarget {
            currentOnAccessoryAction.value()
        }
    }
    val pasteTarget = remember {
        PasteRelayTarget { pastedText ->
            currentOnPaste.value(pastedText)
        }
    }
    val toolbar = remember(
        accessoryAction,
        target,
        pasteTarget
    ) {
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
                        val pasteControl =
                            UIPasteControl().apply {
                                this.target = pasteTarget
                                sizeToFit()
                            }

                        UIBarButtonItem(customView = pasteControl)
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

private class PasteRelayTarget(
    private val onPaste: (String) -> Unit
) : NSObject(), UIPasteConfigurationSupportingProtocol {
    private val plainTextIdentifier =
        UTTypePlainText.identifier
    private var configuration: UIPasteConfiguration? =
        UIPasteConfiguration(
            acceptableTypeIdentifiers =
                listOf(plainTextIdentifier)
        )

    override fun pasteConfiguration(): UIPasteConfiguration? =
        configuration

    override fun setPasteConfiguration(
        pasteConfiguration: UIPasteConfiguration?
    ) {
        configuration = pasteConfiguration
    }

    override fun canPasteItemProviders(
        itemProviders: List<*>
    ): Boolean = itemProviders
        .filterIsInstance<NSItemProvider>()
        .any { provider ->
            provider.hasItemConformingToTypeIdentifier(
                plainTextIdentifier
            )
        }

    override fun pasteItemProviders(
        itemProviders: List<*>
    ) {
        val provider = itemProviders
            .filterIsInstance<NSItemProvider>()
            .firstOrNull { candidate ->
                candidate.hasItemConformingToTypeIdentifier(
                    plainTextIdentifier
                )
            }
            ?: return

        provider.loadItemForTypeIdentifier(
            typeIdentifier = plainTextIdentifier,
            options = null
        ) { item, _ ->
            val pastedText = item
                ?.toString()
                ?.takeIf { it.isNotEmpty() }
                ?: return@loadItemForTypeIdentifier

            dispatch_async(dispatch_get_main_queue()) {
                onPaste(pastedText)
            }
        }
    }
}
