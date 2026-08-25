package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate

@Composable
actual fun rememberScanHapticFeedback(): ScanHapticFeedback {
    return remember {
        object : ScanHapticFeedback {
            override fun scanSucceeded() {
                AudioServicesPlaySystemSound(
                    kSystemSoundID_Vibrate
                )
            }
        }
    }
}