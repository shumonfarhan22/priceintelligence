package com.supreme.priceintelligence.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberPlatformAccessibility():
    PlatformAccessibility {
    val view = LocalView.current

    return remember(view) {
        object : PlatformAccessibility {
            override fun screenChanged(title: String) {
                view.announceForAccessibility(title)
            }
        }
    }
}
