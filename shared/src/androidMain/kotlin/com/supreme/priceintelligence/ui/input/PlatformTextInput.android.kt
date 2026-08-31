package com.supreme.priceintelligence.ui.input

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable

internal actual fun KeyboardOptions.withPlatformTextInput(): KeyboardOptions = this

internal actual fun dismissPlatformKeyboard() = Unit

@Composable
internal actual fun rememberPlatformTextInputOptions(
    keyboardOptions: KeyboardOptions,
    accessoryAction: KeyboardAccessoryAction,
    onAccessoryAction: () -> Unit,
    onPaste: (String) -> Unit
): KeyboardOptions = keyboardOptions
