package com.supreme.priceintelligence.ui.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberPlatformHaptics(): PlatformHaptics {
    val context = LocalContext.current

    return remember(context) {
        AndroidPlatformHaptics(context.applicationContext)
    }
}

private class AndroidPlatformHaptics(
    context: Context
) : PlatformHaptics {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context
                .getSystemService(VibratorManager::class.java)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE)
                as? Vibrator
        }

    override fun actionConfirmed() {
        vibrate(durationMillis = 32L)
    }

    override fun warning() {
        vibratePattern(
            timings = longArrayOf(0L, 28L, 42L, 42L)
        )
    }

    override fun error() {
        vibratePattern(
            timings = longArrayOf(0L, 35L, 35L, 55L)
        )
    }

    private fun vibrate(durationMillis: Long) {
        val activeVibrator = vibrator
            ?.takeIf { it.hasVibrator() }
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activeVibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMillis,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            activeVibrator.vibrate(durationMillis)
        }
    }

    private fun vibratePattern(timings: LongArray) {
        val activeVibrator = vibrator
            ?.takeIf { it.hasVibrator() }
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activeVibrator.vibrate(
                VibrationEffect.createWaveform(
                    timings,
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            activeVibrator.vibrate(timings, -1)
        }
    }
}
