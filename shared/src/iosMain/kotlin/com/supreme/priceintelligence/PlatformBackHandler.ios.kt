package com.supreme.priceintelligence

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // iPhone navigation does not use the Android system Back button.
}