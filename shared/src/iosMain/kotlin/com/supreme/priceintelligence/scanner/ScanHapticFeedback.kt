package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberScanHapticFeedback(): ScanHapticFeedback {
    return remember {
        object : ScanHapticFeedback {
            private val generator =
                UINotificationFeedbackGenerator().apply {
                    prepare()
                }

            override fun scanSucceeded() {
                dispatch_async(dispatch_get_main_queue()) {
                    generator.notificationOccurred(
                        UINotificationFeedbackType.UINotificationFeedbackTypeSuccess
                    )
                    generator.prepare()
                }
            }
        }
    }
}