@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.supreme.priceintelligence.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScreenChangedNotification

@Composable
internal actual fun rememberPlatformAccessibility():
    PlatformAccessibility = remember {
    object : PlatformAccessibility {
        override fun screenChanged(title: String) {
            UIAccessibilityPostNotification(
                notification =
                    UIAccessibilityScreenChangedNotification,
                argument = title
            )
        }
    }
}
