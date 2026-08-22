package com.supreme.priceintelligence.scanner

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberScanHapticFeedback(): ScanHapticFeedback {
    val view = LocalView.current
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    return remember(view, context) {
        object : ScanHapticFeedback {
            override fun scanSucceeded() {
                mainHandler.post {
                    val handled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(
                            HapticFeedbackConstants.CONFIRM,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                        )
                    } else {
                        false
                    }

                    if (!handled) {
                        vibrateLegacy(context)
                    }
                }
            }
        }
    }
}

private fun vibrateLegacy(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(40)
    }
}