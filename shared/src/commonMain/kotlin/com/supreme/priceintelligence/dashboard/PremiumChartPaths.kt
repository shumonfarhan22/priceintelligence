package com.supreme.priceintelligence.dashboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.supreme.priceintelligence.settings.HistoryGraphStyle

internal fun premiumChartLinePath(
    points: List<Offset>,
    graphStyle: HistoryGraphStyle
): Path =
    Path().apply {
        val firstPoint =
            points.firstOrNull()
                ?: return@apply

        moveTo(
            firstPoint.x,
            firstPoint.y
        )

        for (index in 1 until points.size) {
            addPremiumChartSegment(
                previous = points[index - 1],
                current = points[index],
                graphStyle = graphStyle
            )
        }
    }

internal fun premiumChartAreaPath(
    points: List<Offset>,
    graphStyle: HistoryGraphStyle,
    baselineY: Float
): Path =
    Path().apply {
        val firstPoint =
            points.firstOrNull()
                ?: return@apply

        moveTo(
            firstPoint.x,
            baselineY
        )

        lineTo(
            firstPoint.x,
            firstPoint.y
        )

        for (index in 1 until points.size) {
            addPremiumChartSegment(
                previous = points[index - 1],
                current = points[index],
                graphStyle = graphStyle
            )
        }

        lineTo(
            points.last().x,
            baselineY
        )

        close()
    }

private fun Path.addPremiumChartSegment(
    previous: Offset,
    current: Offset,
    graphStyle: HistoryGraphStyle
) {
    if (graphStyle == HistoryGraphStyle.STEP) {
        lineTo(
            current.x,
            previous.y
        )

        lineTo(
            current.x,
            current.y
        )

        return
    }

    val middleX =
        (previous.x + current.x) / 2f

    cubicTo(
        middleX,
        previous.y,
        middleX,
        current.y,
        current.x,
        current.y
    )
}