@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AudioToolbox.AudioServicesPlayAlertSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberScanHapticFeedback(): ScanHapticFeedback {
    return remember {
        object : ScanHapticFeedback {
            override fun scanSucceeded() {
                dispatch_async(dispatch_get_main_queue()) {
                    AudioServicesPlayAlertSound(
                        kSystemSoundID_Vibrate
                    )
                }
            }
        }
    }
}
