@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.supreme.priceintelligence.ui.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

@Composable
internal actual fun rememberPlatformHaptics(): PlatformHaptics =
    remember {
        IosPlatformHaptics()
    }

private class IosPlatformHaptics : PlatformHaptics {
    private val selectionGenerator =
        UISelectionFeedbackGenerator()

    private val impactGenerator =
        UIImpactFeedbackGenerator(
            style =
                UIImpactFeedbackStyle
                    .UIImpactFeedbackStyleLight
        )

    private val notificationGenerator =
        UINotificationFeedbackGenerator()

    override fun selectionChanged() {
        selectionGenerator.prepare()
        selectionGenerator.selectionChanged()
    }

    override fun actionConfirmed() {
        impactGenerator.prepare()
        impactGenerator.impactOccurred()
    }

    override fun warning() {
        notificationGenerator.prepare()
        notificationGenerator.notificationOccurred(
            UINotificationFeedbackType
                .UINotificationFeedbackTypeWarning
        )
    }

    override fun error() {
        notificationGenerator.prepare()
        notificationGenerator.notificationOccurred(
            UINotificationFeedbackType
                .UINotificationFeedbackTypeError
        )
    }
}
