package com.supreme.priceintelligence

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    BackHandler(
        enabled = enabled,
        onBack = onBack
    )
}

@Composable
actual fun Modifier.platformBackSwipe(
    enabled: Boolean,
    onBack: () -> Unit
): Modifier = this