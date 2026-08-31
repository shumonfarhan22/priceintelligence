package com.supreme.priceintelligence.ui.input

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Applies the best text-editing pipeline offered by the current platform while
 * leaving the field's visuals and focus handling in shared Compose code.
 */
internal expect fun KeyboardOptions.withPlatformTextInput(): KeyboardOptions

/**
 * Ends the current platform text-input session. On iOS this also asks UIKit's
 * active first responder to resign, which covers cases where clearing Compose
 * focus alone does not dismiss native text input.
 */
internal expect fun dismissPlatformKeyboard()

@Composable
internal fun rememberKeyboardDismissAction(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController =
        LocalSoftwareKeyboardController.current

    return remember(
        focusManager,
        keyboardController
    ) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            dismissPlatformKeyboard()
        }
    }
}

/**
 * Dismisses editing only for taps that were not consumed by a child control.
 * Text fields and buttons therefore retain their normal first-tap behavior.
 */
internal fun Modifier.dismissKeyboardOnUnhandledTap(
    dismissKeyboard: () -> Unit
): Modifier = pointerInput(dismissKeyboard) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Final
        )
        var current = down
        var consumedByChild = down.isConsumed

        while (current.pressed) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            current = event.changes.firstOrNull { change ->
                change.id == down.id
            } ?: break
            consumedByChild =
                consumedByChild || current.isConsumed
        }

        val movedBeyondTap =
            (current.position - down.position).getDistance() >
                viewConfiguration.touchSlop

        if (
            !current.pressed &&
            !consumedByChild &&
            !movedBeyondTap
        ) {
            dismissKeyboard()
        }
    }
}

internal enum class KeyboardAccessoryAction {
    NONE,
    PASTE_NEXT,
    NEXT,
    DONE
}

@Composable
internal expect fun rememberPlatformTextInputOptions(
    keyboardOptions: KeyboardOptions,
    accessoryAction: KeyboardAccessoryAction,
    onAccessoryAction: () -> Unit,
    onPaste: (String) -> Unit = {}
): KeyboardOptions

internal fun isValidDecimalInput(candidate: CharSequence): Boolean =
    candidate.count { character -> character == '.' } <= 1 &&
        candidate.all { character ->
            character.isDigit() || character == '.'
        }

internal fun isValidBarcodeInput(candidate: CharSequence): Boolean =
    candidate.length <= 64 &&
        candidate.all { character -> character.isDigit() }

internal fun isValidHexColorInput(candidate: CharSequence): Boolean =
    candidate.length <= 7 &&
        candidate.withIndex().all { indexedCharacter ->
            val character = indexedCharacter.value
            character.isDigit() ||
                character.lowercaseChar() in 'a'..'f' ||
                (character == '#' && indexedCharacter.index == 0)
        }

internal val DecimalNumberInputTransformation = InputTransformation {
    val proposed = toString()
    if (!isValidDecimalInput(proposed)) {
        revertAllChanges()
    }
}

internal val BarcodeInputTransformation =
    InputTransformation.maxLength(64).then(
        InputTransformation {
            if (!isValidBarcodeInput(toString())) {
                revertAllChanges()
            }
        }
    )

internal val HexColorInputTransformation =
    InputTransformation.maxLength(7).then(
        InputTransformation {
            if (!isValidHexColorInput(toString())) {
                revertAllChanges()
            }
        }
    )
