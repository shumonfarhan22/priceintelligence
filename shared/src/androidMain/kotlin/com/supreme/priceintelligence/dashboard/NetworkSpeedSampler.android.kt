package com.supreme.priceintelligence.dashboard

import android.net.TrafficStats
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal actual fun rememberNetworkSpeedText(
    isActive: Boolean
): String? {
    var speedText by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            speedText = null
            return@LaunchedEffect
        }

        var previousBytes = TrafficStats.getTotalRxBytes()
        var previousTime = System.currentTimeMillis()

        if (previousBytes == TrafficStats.UNSUPPORTED.toLong()) {
            speedText = null
            return@LaunchedEffect
        }

        speedText = "0 B/s"

        while (true) {
            delay(250.milliseconds)

            val currentBytes = TrafficStats.getTotalRxBytes()
            val currentTime = System.currentTimeMillis()

            if (currentBytes == TrafficStats.UNSUPPORTED.toLong()) {
                speedText = null
                return@LaunchedEffect
            }

            val elapsedMilliseconds =
                (currentTime - previousTime).coerceAtLeast(1L)

            val downloadedBytes =
                (currentBytes - previousBytes).coerceAtLeast(0L)

            val bytesPerSecond =
                (downloadedBytes * 1000L) / elapsedMilliseconds

            speedText = formatNetworkSpeed(bytesPerSecond)

            previousBytes = currentBytes
            previousTime = currentTime
        }
    }

    return speedText
}

private fun formatNetworkSpeed(
    bytesPerSecond: Long
): String = when {
    bytesPerSecond >= 1024L * 1024L -> {
        String.format(
            Locale.US,
            "%.1f MB/s",
            bytesPerSecond / (1024f * 1024f)
        )
    }

    bytesPerSecond >= 1024L -> {
        String.format(
            Locale.US,
            "%.0f KB/s",
            bytesPerSecond / 1024f
        )
    }

    else -> {
        "$bytesPerSecond B/s"
    }
}