@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

@Composable
actual fun rememberScanHapticFeedback(): ScanHapticFeedback {
    return remember {
        val feedbackGenerator =
            UINotificationFeedbackGenerator()

        object : ScanHapticFeedback {
            override fun scanSucceeded() {
                feedbackGenerator.prepare()
                feedbackGenerator.notificationOccurred(
                    UINotificationFeedbackType
                        .UINotificationFeedbackTypeSuccess
                )
            }
        }
    }
}
