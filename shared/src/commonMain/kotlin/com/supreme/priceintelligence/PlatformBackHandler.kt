package com.supreme.priceintelligence

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
)

@Composable
expect fun Modifier.platformBackSwipe(
    enabled: Boolean,
    onBack: () -> Unit
): Modifier