package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable

interface ScanHapticFeedback {
    fun scanSucceeded()
}

@Composable
expect fun rememberScanHapticFeedback(): ScanHapticFeedback