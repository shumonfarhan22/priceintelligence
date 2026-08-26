package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.data.PriceRetailer
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val InteractiveAmazonColor =
    Color(0xFFFF9900)

private val InteractiveFlipkartColor =
    Color(0xFF2874F0)

private const val IndiaTimeOffsetMillis =
    5L * 60L * 60L * 1000L +
            30L * 60L * 1000L

@Composable
internal fun InteractiveAggregateMovementChart(
    changes: List<ShopPriceChange>,
    range: ShopMovementRange,
    generatedAt: Long,
    lowerColor: Color,
    higherColor: Color
) {
    val buckets = remember(
        changes,
        range,
        generatedAt
    ) {
        buildInteractiveMovementBuckets(
            changes = changes,
            range = range,
            generatedAt = generatedAt
        )
    }

    val maximumCount =
        buckets.maxOfOrNull { bucket ->
            maxOf(
                bucket.lowerCount,
                bucket.higherCount
            )
        }
            ?.coerceAtLeast(1)
            ?: 1

    val middleCount =
        (maximumCount + 1) / 2

    val windowMillis =
        range.days *
                PRICE_MOVEMENT_DAY_MILLIS

    val startTime =
        (
                generatedAt -
                        windowMillis
                ).coerceAtLeast(0L)

    val middleTime =
        startTime +
                windowMillis / 2L

    Column {
        Text(
            text = "MOVEMENT ACTIVITY",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp
        )

        Text(
            text =
                "Y: number of changes • X: time",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 9.sp
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(122.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .padding(vertical = 7.dp),
                verticalArrangement =
                    Arrangement.SpaceBetween,
                horizontalAlignment =
                    Alignment.End
            ) {
                AxisText(
                    text =
                        maximumCount.toString()
                )

                AxisText(
                    text =
                        middleCount.toString()
                )

                AxisText(text = "0")
            }

            Spacer(
                modifier = Modifier.width(7.dp)
            )

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        contentDescription =
                            "Price movement graph. " +
                                    "Vertical axis shows change count. " +
                                    "Horizontal axis shows time."
                    }
            ) {
                val topPadding =
                    7.dp.toPx()

                val baseline =
                    size.height -
                            7.dp.toPx()

                val usableHeight =
                    (
                            baseline -
                                    topPadding
                            ).coerceAtLeast(1f)

                repeat(3) { index ->
                    val y =
                        topPadding +
                                usableHeight *
                                index / 2f

                    drawLine(
                        color =
                            Color.Gray.copy(
                                alpha = 0.22f
                            ),
                        start =
                            Offset(0f, y),
                        end =
                            Offset(
                                size.width,
                                y
                            ),
                        strokeWidth =
                            1.dp.toPx()
                    )
                }

                val groupWidth =
                    if (buckets.isEmpty()) {
                        size.width
                    } else {
                        size.width /
                                buckets.size
                    }

                val barWidth =
                    minOf(
                        groupWidth * 0.22f,
                        8.dp.toPx()
                    ).coerceAtLeast(
                        2.dp.toPx()
                    )

                buckets.forEachIndexed {
                        index,
                        bucket ->

                    val centerX =
                        groupWidth *
                                (index + 0.5f)

                    val lowerHeight =
                        usableHeight *
                                bucket.lowerCount /
                                maximumCount

                    val higherHeight =
                        usableHeight *
                                bucket.higherCount /
                                maximumCount

                    if (
                        bucket.lowerCount >
                        0
                    ) {
                        drawLine(
                            color = lowerColor,
                            start = Offset(
                                x =
                                    centerX -
                                            barWidth *
                                            0.65f,
                                y = baseline
                            ),
                            end = Offset(
                                x =
                                    centerX -
                                            barWidth *
                                            0.65f,
                                y =
                                    baseline -
                                            lowerHeight
                            ),
                            strokeWidth =
                                barWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    if (
                        bucket.higherCount >
                        0
                    ) {
                        drawLine(
                            color = higherColor,
                            start = Offset(
                                x =
                                    centerX +
                                            barWidth *
                                            0.65f,
                                y = baseline
                            ),
                            end = Offset(
                                x =
                                    centerX +
                                            barWidth *
                                            0.65f,
                                y =
                                    baseline -
                                            higherHeight
                            ),
                            strokeWidth =
                                barWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 35.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            AxisText(
                text =
                    formatAggregateAxisLabel(
                        timestamp = startTime,
                        range = range
                    )
            )

            AxisText(
                text =
                    formatAggregateAxisLabel(
                        timestamp = middleTime,
                        range = range
                    )
            )

            AxisText(
                text =
                    formatAggregateAxisLabel(
                        timestamp = generatedAt,
                        range = range
                    )
            )
        }

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            InteractiveChartLegend(
                text = "Lower",
                color = lowerColor
            )

            InteractiveChartLegend(
                text = "Higher",
                color = higherColor
            )
        }
    }
}

@Composable
internal fun InteractiveProductMovementLineChart(
    amazonHistory: List<ShopPricePoint>,
    flipkartHistory: List<ShopPricePoint>,
    notificationTarget:
        PriceMovementNotificationTarget? = null,
    notificationHighlightColor:
        Color = Color.Transparent,
    notificationPulseProgress:
        Float = 0f
) {
    val series = remember(
        amazonHistory,
        flipkartHistory
    ) {
        buildList {
            if (amazonHistory.isNotEmpty()) {
                add(
                    InteractiveChartSeries(
                        retailer =
                            PriceRetailer.AMAZON,
                        color =
                            InteractiveAmazonColor,
                        points =
                            amazonHistory
                    )
                )
            }

            if (flipkartHistory.isNotEmpty()) {
                add(
                    InteractiveChartSeries(
                        retailer =
                            PriceRetailer.FLIPKART,
                        color =
                            InteractiveFlipkartColor,
                        points =
                            flipkartHistory
                    )
                )
            }
        }
    }

    val allPoints =
        series.flatMap { item ->
            item.points
        }

    if (allPoints.isEmpty()) {
        return
    }

    var selectedPoint by remember {
        mutableStateOf<
                SelectedMovementChartPoint?
                >(null)
    }

    LaunchedEffect(
        amazonHistory,
        flipkartHistory,
        notificationTarget?.requestId
    ) {
        val target =
            notificationTarget

        selectedPoint =
            if (target == null) {
                null
            } else {
                val targetSeries =
                    series.firstOrNull { chartSeries ->
                        chartSeries.retailer ==
                            target.retailer
                    }

                val targetPoint =
                    targetSeries
                        ?.points
                        ?.minByOrNull { point ->
                            kotlin.math.abs(
                                point.checkedAt -
                                    target.detectedAt
                            )
                        }

                if (
                    targetSeries != null &&
                    targetPoint != null
                ) {
                    SelectedMovementChartPoint(
                        retailer =
                            targetSeries.retailer,
                        point =
                            targetPoint,
                        previousPrice =
                            target.oldPrice,
                        color =
                            targetSeries.color
                    )
                } else {
                    null
                }
            }
    }

    val minimumPrice =
        allPoints.minOf { point ->
            point.price
        }

    val maximumPrice =
        allPoints.maxOf { point ->
            point.price
        }

    val middlePrice =
        minimumPrice +
                (
                        maximumPrice -
                                minimumPrice
                        ) / 2.0

    val minimumTime =
        allPoints.minOf { point ->
            point.checkedAt
        }

    val maximumTime =
        allPoints.maxOf { point ->
            point.checkedAt
        }

    val middleTime =
        minimumTime +
                (
                        maximumTime -
                                minimumTime
                        ) / 2L

    val gridColor =
        MaterialTheme
            .colorScheme
            .onSurfaceVariant
            .copy(alpha = 0.16f)

    val selectedPointSurfaceColor =
        MaterialTheme
            .colorScheme
            .surface

    Column {
        Text(
            text =
                "Y: saved price • X: date • Tap a point for details",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 9.sp
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(122.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(57.dp)
                    .fillMaxHeight()
                    .padding(vertical = 9.dp),
                verticalArrangement =
                    Arrangement.SpaceBetween,
                horizontalAlignment =
                    Alignment.End
            ) {
                AxisText(
                    text =
                        formatAxisPrice(
                            maximumPrice
                        )
                )

                AxisText(
                    text =
                        if (
                            maximumPrice -
                            minimumPrice >
                            0.01
                        ) {
                            formatAxisPrice(
                                middlePrice
                            )
                        } else {
                            ""
                        }
                )

                AxisText(
                    text =
                        formatAxisPrice(
                            minimumPrice
                        )
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(
                        series,
                        minimumPrice,
                        maximumPrice,
                        minimumTime,
                        maximumTime
                    ) {
                        detectTapGestures {
                                tapPosition ->

                            val horizontalPadding =
                                8.dp.toPx()

                            val verticalPadding =
                                9.dp.toPx()

                            val targets =
                                series.flatMap {
                                        chartSeries ->

                                    chartSeries
                                        .points
                                        .mapIndexed {
                                                index,
                                                point ->

                                            InteractiveTapTarget(
                                                selection =
                                                    SelectedMovementChartPoint(
                                                        retailer =
                                                            chartSeries
                                                                .retailer,
                                                        point =
                                                            point,
                                                        previousPrice =
                                                            chartSeries
                                                                .points
                                                                .getOrNull(
                                                                    index -
                                                                            1
                                                                )
                                                                ?.price,
                                                        color =
                                                            chartSeries
                                                                .color
                                                    ),
                                                position =
                                                    movementPointOffset(
                                                        point =
                                                            point,
                                                        minimumPrice =
                                                            minimumPrice,
                                                        maximumPrice =
                                                            maximumPrice,
                                                        minimumTime =
                                                            minimumTime,
                                                        maximumTime =
                                                            maximumTime,
                                                        canvasWidth =
                                                            size.width
                                                                .toFloat(),
                                                        canvasHeight =
                                                            size.height
                                                                .toFloat(),
                                                        horizontalPadding =
                                                            horizontalPadding,
                                                        verticalPadding =
                                                            verticalPadding
                                                    )
                                            )
                                        }
                                }

                            val nearestTarget =
                                targets.minByOrNull {
                                        target ->

                                    distanceSquared(
                                        first =
                                            target.position,
                                        second =
                                            tapPosition
                                    )
                                }

                            val touchRadius =
                                30.dp.toPx()

                            selectedPoint =
                                if (
                                    nearestTarget !=
                                    null &&
                                    distanceSquared(
                                        first =
                                            nearestTarget
                                                .position,
                                        second =
                                            tapPosition
                                    ) <=
                                    touchRadius *
                                    touchRadius
                                ) {
                                    nearestTarget.selection
                                } else {
                                    null
                                }
                        }
                    }
                    .semantics {
                        contentDescription =
                            "Interactive Amazon and Flipkart price graph. " +
                                    "Tap a point to hear or view its exact price and date."
                    }
            ) {
                val horizontalPadding =
                    8.dp.toPx()

                val verticalPadding =
                    9.dp.toPx()

                val chartWidth =
                    (
                            size.width -
                                    horizontalPadding *
                                    2f
                            ).coerceAtLeast(1f)

                val chartHeight =
                    (
                            size.height -
                                    verticalPadding *
                                    2f
                            ).coerceAtLeast(1f)

                repeat(3) { index ->
                    val y =
                        verticalPadding +
                                chartHeight *
                                index / 2f

                    drawLine(
                        color = gridColor,
                        start = Offset(
                            horizontalPadding,
                            y
                        ),
                        end = Offset(
                            size.width -
                                    horizontalPadding,
                            y
                        ),
                        strokeWidth =
                            1.dp.toPx()
                    )
                }

                series.forEach {
                        chartSeries ->

                    val path = Path()

                    chartSeries.points
                        .forEachIndexed {
                                index,
                                point ->

                            val pointOffset =
                                movementPointOffset(
                                    point = point,
                                    minimumPrice =
                                        minimumPrice,
                                    maximumPrice =
                                        maximumPrice,
                                    minimumTime =
                                        minimumTime,
                                    maximumTime =
                                        maximumTime,
                                    canvasWidth =
                                        size.width,
                                    canvasHeight =
                                        size.height,
                                    horizontalPadding =
                                        horizontalPadding,
                                    verticalPadding =
                                        verticalPadding
                                )

                            if (index == 0) {
                                path.moveTo(
                                    pointOffset.x,
                                    pointOffset.y
                                )
                            } else {
                                path.lineTo(
                                    pointOffset.x,
                                    pointOffset.y
                                )
                            }
                        }

                    if (
                        chartSeries.points.size >
                        1
                    ) {
                        drawPath(
                            path = path,
                            color =
                                chartSeries.color,
                            style = Stroke(
                                width =
                                    2.5.dp.toPx(),
                                cap =
                                    StrokeCap.Round
                            )
                        )
                    }

                    chartSeries.points
                        .forEach { point ->
                            val pointOffset =
                                movementPointOffset(
                                    point = point,
                                    minimumPrice =
                                        minimumPrice,
                                    maximumPrice =
                                        maximumPrice,
                                    minimumTime =
                                        minimumTime,
                                    maximumTime =
                                        maximumTime,
                                    canvasWidth =
                                        size.width,
                                    canvasHeight =
                                        size.height,
                                    horizontalPadding =
                                        horizontalPadding,
                                    verticalPadding =
                                        verticalPadding
                                )

                            val isSelected =
                                selectedPoint
                                    ?.matches(
                                        retailer =
                                            chartSeries
                                                .retailer,
                                        point = point
                                    )
                                    ?: false

                            if (isSelected) {
                                val isNotificationPoint =
                                    notificationTarget
                                        ?.let { target ->
                                            target.retailer ==
                                                chartSeries.retailer &&
                                                kotlin.math.abs(
                                                    point.checkedAt -
                                                        target.detectedAt
                                                ) < 1000L
                                        }
                                        ?: false

                                if (isNotificationPoint) {
                                    drawCircle(
                                        color =
                                            notificationHighlightColor
                                                .copy(
                                                    alpha =
                                                        0.16f +
                                                            notificationPulseProgress *
                                                            0.30f
                                                ),
                                        radius =
                                            (
                                                11f +
                                                    notificationPulseProgress *
                                                    8f
                                            ).dp.toPx(),
                                        center =
                                            pointOffset
                                    )
                                }

                                drawCircle(
                                    color =
                                        chartSeries
                                            .color
                                            .copy(
                                                alpha =
                                                    0.22f
                                            ),
                                    radius =
                                        10.dp.toPx(),
                                    center =
                                        pointOffset
                                )

                                drawCircle(
                                    color =
                                        selectedPointSurfaceColor,
                                    radius =
                                        6.dp.toPx(),
                                    center =
                                        pointOffset
                                )
                            }

                            drawCircle(
                                color =
                                    chartSeries.color,
                                radius =
                                    if (isSelected) {
                                        4.5.dp.toPx()
                                    } else {
                                        3.5.dp.toPx()
                                    },
                                center =
                                    pointOffset
                            )
                        }
                }
            }
        }

        if (minimumTime == maximumTime) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 65.dp),
                contentAlignment =
                    Alignment.Center
            ) {
                AxisText(
                    text =
                        formatShortGraphDate(
                            minimumTime
                        )
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 65.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                AxisText(
                    text =
                        formatShortGraphDate(
                            minimumTime
                        )
                )

                AxisText(
                    text =
                        formatShortGraphDate(
                            middleTime
                        )
                )

                AxisText(
                    text =
                        formatShortGraphDate(
                            maximumTime
                        )
                )
            }
        }

        AnimatedVisibility(
            visible =
                selectedPoint != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedPoint?.let { selection ->
                SelectedMovementPointDetails(
                    selection = selection,
                    modifier =
                        Modifier.padding(
                            top = 10.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun SelectedMovementPointDetails(
    selection: SelectedMovementChartPoint,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier =
            modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color =
            selection.color.copy(
                alpha = 0.10f
            ),
        border = BorderStroke(
            width = 1.dp,
            color =
                selection.color.copy(
                    alpha = 0.38f
                )
        )
    ) {
        Column(
            modifier = Modifier.padding(11.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            color =
                                selection.color,
                            shape = CircleShape
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(7.dp)
                )

                Text(
                    text =
                        selection
                            .retailer
                            .displayInteractiveName(),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 12.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                Text(
                    text =
                        formatIndianPrice(
                            selection.point.price
                        ),
                    color = selection.color,
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text =
                    formatFullGraphDate(
                        selection
                            .point
                            .checkedAt
                    ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 10.sp
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    selectedMovementDescription(
                        selection
                    ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun InteractiveChartLegend(
    text: String,
    color: Color
) {
    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )

        Spacer(
            modifier = Modifier.width(5.dp)
        )

        Text(
            text = text,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AxisText(
    text: String
) {
    Text(
        text = text,
        color =
            MaterialTheme
                .colorScheme
                .onSurfaceVariant,
        fontSize = 8.sp,
        maxLines = 1
    )
}

private data class InteractiveChartSeries(
    val retailer: PriceRetailer,
    val color: Color,
    val points: List<ShopPricePoint>
)

private data class SelectedMovementChartPoint(
    val retailer: PriceRetailer,
    val point: ShopPricePoint,
    val previousPrice: Double?,
    val color: Color
) {
    fun matches(
        retailer: PriceRetailer,
        point: ShopPricePoint
    ): Boolean =
        this.retailer == retailer &&
                this.point.checkedAt ==
                point.checkedAt &&
                this.point.price ==
                point.price
}

private data class InteractiveTapTarget(
    val selection:
    SelectedMovementChartPoint,
    val position: Offset
)

private data class InteractiveMovementBucket(
    val lowerCount: Int,
    val higherCount: Int
)

private data class GraphCalendarParts(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int
)

private fun movementPointOffset(
    point: ShopPricePoint,
    minimumPrice: Double,
    maximumPrice: Double,
    minimumTime: Long,
    maximumTime: Long,
    canvasWidth: Float,
    canvasHeight: Float,
    horizontalPadding: Float,
    verticalPadding: Float
): Offset {
    val chartWidth =
        (
                canvasWidth -
                        horizontalPadding * 2f
                ).coerceAtLeast(1f)

    val chartHeight =
        (
                canvasHeight -
                        verticalPadding * 2f
                ).coerceAtLeast(1f)

    val timeRange =
        (
                maximumTime -
                        minimumTime
                ).takeIf {
                it > 0L
            }
            ?: 1L

    val priceRange =
        (
                maximumPrice -
                        minimumPrice
                ).takeIf {
                it > 0.01
            }
            ?: 1.0

    val x =
        if (
            maximumTime ==
            minimumTime
        ) {
            horizontalPadding +
                    chartWidth / 2f
        } else {
            horizontalPadding +
                    chartWidth *
                    (
                            point.checkedAt -
                                    minimumTime
                            ).toFloat() /
                    timeRange.toFloat()
        }

    val normalizedPrice =
        (
                point.price -
                        minimumPrice
                ) / priceRange

    val y =
        if (
            maximumPrice -
            minimumPrice <=
            0.01
        ) {
            verticalPadding +
                    chartHeight / 2f
        } else {
            verticalPadding +
                    chartHeight *
                    (
                            1f -
                                    normalizedPrice
                                        .toFloat()
                            )
        }

    return Offset(x, y)
}

private fun distanceSquared(
    first: Offset,
    second: Offset
): Float {
    val xDifference =
        first.x - second.x

    val yDifference =
        first.y - second.y

    return xDifference *
            xDifference +
            yDifference *
            yDifference
}

private fun buildInteractiveMovementBuckets(
    changes: List<ShopPriceChange>,
    range: ShopMovementRange,
    generatedAt: Long
): List<InteractiveMovementBucket> {
    val bucketCount =
        when (range) {
            ShopMovementRange.ONE_DAY ->
                6

            ShopMovementRange.SEVEN_DAYS ->
                7

            ShopMovementRange.THIRTY_DAYS ->
                15
        }

    val windowMillis =
        range.days *
                PRICE_MOVEMENT_DAY_MILLIS

    val cutoff =
        (
                generatedAt -
                        windowMillis
                ).coerceAtLeast(0L)

    val bucketSize =
        (
                windowMillis /
                        bucketCount
                ).coerceAtLeast(1L)

    val lowerCounts =
        IntArray(bucketCount)

    val higherCounts =
        IntArray(bucketCount)

    changes.forEach { change ->
        val index =
            (
                    (
                            change.checkedAt -
                                    cutoff
                            ) /
                            bucketSize
                    )
                .toInt()
                .coerceIn(
                    0,
                    bucketCount - 1
                )

        if (
            change.direction ==
            DetectedPriceDirection.LOWER
        ) {
            lowerCounts[index] += 1
        } else {
            higherCounts[index] += 1
        }
    }

    return List(bucketCount) { index ->
        InteractiveMovementBucket(
            lowerCount =
                lowerCounts[index],
            higherCount =
                higherCounts[index]
        )
    }
}

private fun selectedMovementDescription(
    selection: SelectedMovementChartPoint
): String {
    val previousPrice =
        selection.previousPrice
            ?: return "First saved observation for this retailer"

    val difference =
        (
                selection.point.price -
                        previousPrice
                ).absoluteValue

    val percentage =
        if (previousPrice > 0.0) {
            difference /
                    previousPrice *
                    100.0
        } else {
            0.0
        }

    val direction =
        when {
            selection.point.price <
                    previousPrice ->
                "Lower by"

            selection.point.price >
                    previousPrice ->
                "Higher by"

            else ->
                "Unchanged at"
        }

    return buildString {
        append(direction)
        append(" ")
        append(formatIndianPrice(difference))

        if (
            selection.point.price !=
            previousPrice
        ) {
            append(" (")
            append(
                formatInteractivePercentage(
                    percentage
                )
            )
            append(")")
        }

        append(" from ")
        append(
            formatIndianPrice(
                previousPrice
            )
        )
    }
}

private fun PriceRetailer.displayInteractiveName():
        String =
    when (this) {
        PriceRetailer.AMAZON ->
            "Amazon"

        PriceRetailer.FLIPKART ->
            "Flipkart"
    }

private fun formatAggregateAxisLabel(
    timestamp: Long,
    range: ShopMovementRange
): String =
    if (
        range ==
        ShopMovementRange.ONE_DAY
    ) {
        formatGraphTime(timestamp)
    } else {
        formatShortGraphDate(timestamp)
    }

private fun formatAxisPrice(
    price: Double
): String =
    when {
        price >= 100_000.0 ->
            "₹${formatCompactNumber(price / 100_000.0)}L"

        price >= 1_000.0 ->
            "₹${formatCompactNumber(price / 1_000.0)}K"

        else ->
            formatIndianPrice(price)
    }

private fun formatCompactNumber(
    value: Double
): String {
    val rounded =
        (
                value * 10.0
                ).roundToInt() / 10.0

    return if (
        rounded ==
        rounded.toInt().toDouble()
    ) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun formatInteractivePercentage(
    percentage: Double
): String {
    val rounded =
        (
                percentage * 10.0
                ).roundToInt() / 10.0

    val displayed =
        if (
            rounded ==
            rounded.toInt().toDouble()
        ) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }

    return "$displayed%"
}

private fun formatShortGraphDate(
    timestamp: Long
): String {
    val date =
        graphCalendarParts(timestamp)

    return "${date.day} " +
            graphMonthName(date.month)
}

private fun formatGraphTime(
    timestamp: Long
): String {
    val date =
        graphCalendarParts(timestamp)

    val twelveHour =
        when {
            date.hour == 0 ->
                12

            date.hour > 12 ->
                date.hour - 12

            else ->
                date.hour
        }

    val period =
        if (date.hour < 12) {
            "AM"
        } else {
            "PM"
        }

    return "$twelveHour:" +
            date.minute
                .toString()
                .padStart(2, '0') +
            " $period"
}

private fun formatFullGraphDate(
    timestamp: Long
): String {
    val date =
        graphCalendarParts(timestamp)

    return buildString {
        append(date.day)
        append(" ")
        append(
            graphMonthName(
                date.month
            )
        )
        append(" ")
        append(date.year)
        append(" • ")
        append(
            formatGraphTime(
                timestamp
            )
        )
    }
}

private fun graphCalendarParts(
    timestamp: Long
): GraphCalendarParts {
    val adjustedTimestamp =
        (
                timestamp +
                        IndiaTimeOffsetMillis
                ).coerceAtLeast(0L)

    val epochDay =
        adjustedTimestamp /
                PRICE_MOVEMENT_DAY_MILLIS

    val millisecondsInDay =
        adjustedTimestamp %
                PRICE_MOVEMENT_DAY_MILLIS

    var adjustedDay =
        epochDay + 719468L

    val era =
        if (adjustedDay >= 0L) {
            adjustedDay / 146097L
        } else {
            (
                    adjustedDay -
                            146096L
                    ) / 146097L
        }

    val dayOfEra =
        adjustedDay -
                era * 146097L

    val yearOfEra =
        (
                dayOfEra -
                        dayOfEra / 1460L +
                        dayOfEra / 36524L -
                        dayOfEra / 146096L
                ) / 365L

    var year =
        yearOfEra +
                era * 400L

    val dayOfYear =
        dayOfEra -
                (
                        365L * yearOfEra +
                                yearOfEra / 4L -
                                yearOfEra / 100L
                        )

    val monthPosition =
        (
                5L * dayOfYear +
                        2L
                ) / 153L

    val day =
        dayOfYear -
                (
                        153L *
                                monthPosition +
                                2L
                        ) / 5L +
                1L

    val month =
        monthPosition +
                if (monthPosition < 10L) {
                    3L
                } else {
                    -9L
                }

    year +=
        if (month <= 2L) {
            1L
        } else {
            0L
        }

    val hour =
        millisecondsInDay /
                (60L * 60L * 1000L)

    val minute =
        (
                millisecondsInDay %
                        (60L * 60L * 1000L)
                ) /
                (60L * 1000L)

    return GraphCalendarParts(
        year = year.toInt(),
        month = month.toInt(),
        day = day.toInt(),
        hour = hour.toInt(),
        minute = minute.toInt()
    )
}

private fun graphMonthName(
    month: Int
): String =
    when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }