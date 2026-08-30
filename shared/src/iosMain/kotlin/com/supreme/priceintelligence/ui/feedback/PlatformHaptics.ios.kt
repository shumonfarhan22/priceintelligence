@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.supreme.priceintelligence.ui.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AudioToolbox.AudioServicesPlayAlertSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
internal actual fun rememberPlatformHaptics(): PlatformHaptics =
    remember {
        IosPlatformHaptics()
    }

private class IosPlatformHaptics : PlatformHaptics {
    override fun selectionChanged() {
        vibrate()
    }

    override fun actionConfirmed() {
        vibrate()
    }

    override fun warning() {
        vibrate()
    }

    override fun error() {
        vibrate()
    }

    private fun vibrate() {
        // Audio Services is used deliberately here instead of retaining a
        // UIKit feedback generator across Compose recompositions. It gives
        // iPhone a dependable, system-owned vibration and is dispatched to
        // the main queue as required by the native UI lifecycle.
        dispatch_async(dispatch_get_main_queue()) {
            AudioServicesPlayAlertSound(
                kSystemSoundID_Vibrate
            )
        }
    }
}
