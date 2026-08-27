package com.supreme.priceintelligence

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // iPhone back navigation is handled by platformBackSwipe.
}

@Composable
actual fun Modifier.platformBackSwipe(
    enabled: Boolean,
    onBack: () -> Unit
): Modifier {
    val currentOnBack =
        rememberUpdatedState(onBack)

    val density = LocalDensity.current

    val edgeWidthPx =
        with(density) {
            28.dp.toPx()
        }

    val requiredDistancePx =
        with(density) {
            72.dp.toPx()
        }

    if (!enabled) {
        return this
    }

    return this.pointerInput(
        enabled,
        edgeWidthPx,
        requiredDistancePx
    ) {
        var startedAtLeftEdge = false
        var horizontalDistance = 0f
        var verticalDistance = 0f

        detectDragGestures(
            onDragStart = { startPosition ->
                startedAtLeftEdge =
                    startPosition.x <= edgeWidthPx

                horizontalDistance = 0f
                verticalDistance = 0f
            },
            onDragCancel = {
                startedAtLeftEdge = false
                horizontalDistance = 0f
                verticalDistance = 0f
            },
            onDragEnd = {
                val isRightwardBackSwipe =
                    startedAtLeftEdge &&
                        horizontalDistance >=
                            requiredDistancePx &&
                        horizontalDistance >
                            verticalDistance * 1.35f

                if (isRightwardBackSwipe) {
                    currentOnBack.value.invoke()
                }

                startedAtLeftEdge = false
                horizontalDistance = 0f
                verticalDistance = 0f
            },
            onDrag = { change, dragAmount ->
                if (startedAtLeftEdge) {
                    horizontalDistance =
                        (
                            horizontalDistance +
                                dragAmount.x
                            ).coerceAtLeast(0f)

                    verticalDistance +=
                        abs(dragAmount.y)

                    if (
                        horizontalDistance >
                        verticalDistance
                    ) {
                        change.consume()
                    }
                }
            }
        )
    }
}