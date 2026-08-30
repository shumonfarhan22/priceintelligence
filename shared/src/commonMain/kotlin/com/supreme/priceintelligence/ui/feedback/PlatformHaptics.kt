package com.supreme.priceintelligence.ui.feedback

import androidx.compose.runtime.Composable

/**
 * Small, meaningful feedback events that each platform renders with its own
 * haptic engine. Routine taps deliberately do not produce feedback.
 */
internal interface PlatformHaptics {
    fun selectionChanged()

    fun actionConfirmed()

    fun warning()

    fun error()
}

@Composable
internal expect fun rememberPlatformHaptics(): PlatformHaptics
