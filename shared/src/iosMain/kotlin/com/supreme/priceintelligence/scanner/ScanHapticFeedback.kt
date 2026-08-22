package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

@Composable
actual fun rememberScanHapticFeedback(): ScanHapticFeedback {
    return remember {
        object : ScanHapticFeedback {
            private val generator = UINotificationFeedbackGenerator()

            override fun scanSucceeded() {
                generator.prepare()
                generator.notificationOccurred(
                    UINotificationFeedbackType.UINotificationFeedbackTypeSuccess
                )
            }
        }
    }
}