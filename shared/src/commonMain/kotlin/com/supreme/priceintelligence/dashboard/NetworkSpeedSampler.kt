package com.supreme.priceintelligence.dashboard

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberNetworkSpeedText(
    isActive: Boolean
): String?