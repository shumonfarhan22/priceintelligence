package com.supreme.priceintelligence.inventory

import androidx.compose.runtime.Composable

@Composable
internal expect fun KeyboardAwareEditorDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
)