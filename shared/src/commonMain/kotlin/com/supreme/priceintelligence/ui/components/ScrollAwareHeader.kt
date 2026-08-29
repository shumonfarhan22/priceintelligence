package com.supreme.priceintelligence.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun rememberScrollAwareHeaderVisible(
    listState: LazyListState,
    forceVisible: Boolean = false,
    hideThreshold: Dp = 24.dp
): Boolean {
    var visible by remember(listState) {
        mutableStateOf(true)
    }

    val hideThresholdPx =
        with(LocalDensity.current) {
            hideThreshold.roundToPx()
        }

    LaunchedEffect(
        listState,
        hideThresholdPx,
        forceVisible
    ) {
        if (forceVisible) {
            visible = true
            return@LaunchedEffect
        }

        var previousIndex =
            listState.firstVisibleItemIndex

        var previousOffset =
            listState.firstVisibleItemScrollOffset

        var downwardTravelPx = 0

        snapshotFlow {
            listState.firstVisibleItemIndex to
                listState.firstVisibleItemScrollOffset
        }.collect { position ->
            val currentIndex = position.first
            val currentOffset = position.second

            val atTop =
                currentIndex == 0 &&
                    currentOffset == 0

            val scrollingUp =
                currentIndex < previousIndex ||
                    (
                        currentIndex == previousIndex &&
                            currentOffset < previousOffset
                    )

            val scrollingDown =
                currentIndex > previousIndex ||
                    (
                        currentIndex == previousIndex &&
                            currentOffset > previousOffset
                    )

            when {
                atTop -> {
                    visible = true
                    downwardTravelPx = 0
                }

                scrollingUp -> {
                    visible = true
                    downwardTravelPx = 0
                }

                scrollingDown -> {
                    val downwardDelta =
                        if (currentIndex > previousIndex) {
                            hideThresholdPx
                        } else {
                            (currentOffset - previousOffset)
                                .coerceAtLeast(0)
                        }

                    downwardTravelPx += downwardDelta

                    if (
                        downwardTravelPx >=
                            hideThresholdPx
                    ) {
                        visible = false
                        downwardTravelPx = 0
                    }
                }
            }

            previousIndex = currentIndex
            previousOffset = currentOffset
        }
    }

    return visible
}

@Composable
internal fun ScrollAwareHeader(
    visible: Boolean,
    reduceMotionEnabled: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter =
            if (reduceMotionEnabled) {
                fadeIn(
                    animationSpec =
                        tween(durationMillis = 0)
                )
            } else {
                expandVertically(
                    animationSpec =
                        tween(durationMillis = 180),
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    animationSpec =
                        tween(durationMillis = 140)
                )
            },
        exit =
            if (reduceMotionEnabled) {
                fadeOut(
                    animationSpec =
                        tween(durationMillis = 0)
                )
            } else {
                shrinkVertically(
                    animationSpec =
                        tween(durationMillis = 160),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(
                    animationSpec =
                        tween(durationMillis = 110)
                )
            }
    ) {
        content()
    }
}
