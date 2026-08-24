package com.supreme.priceintelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberReduceMotionEnabled(): Boolean {
    return remember {
        UIAccessibilityIsReduceMotionEnabled()
    }
}