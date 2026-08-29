package com.supreme.priceintelligence.ui.input

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable

internal actual fun KeyboardOptions.withPlatformTextInput(): KeyboardOptions = this

@Composable
internal actual fun rememberPlatformTextInputOptions(
    keyboardOptions: KeyboardOptions,
    accessoryAction: KeyboardAccessoryAction,
    onAccessoryAction: () -> Unit
): KeyboardOptions = keyboardOptions
