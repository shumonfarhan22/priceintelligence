package com.supreme.priceintelligence.ui.accessibility

import androidx.compose.runtime.Composable

internal interface PlatformAccessibility {
    /** Moves assistive-technology context to a newly opened app screen. */
    fun screenChanged(title: String)
}

@Composable
internal expect fun rememberPlatformAccessibility():
    PlatformAccessibility
