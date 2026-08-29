package com.supreme.priceintelligence.ui.input

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Composable

/**
 * Applies the best text-editing pipeline offered by the current platform while
 * leaving the field's visuals and focus handling in shared Compose code.
 */
internal expect fun KeyboardOptions.withPlatformTextInput(): KeyboardOptions

internal enum class KeyboardAccessoryAction {
    NONE,
    NEXT,
    DONE
}

@Composable
internal expect fun rememberPlatformTextInputOptions(
    keyboardOptions: KeyboardOptions,
    accessoryAction: KeyboardAccessoryAction,
    onAccessoryAction: () -> Unit
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
