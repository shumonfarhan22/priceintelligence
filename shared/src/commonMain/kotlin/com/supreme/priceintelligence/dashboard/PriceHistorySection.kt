package com.supreme.priceintelligence.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.data.PriceRetailer
import com.supreme.priceintelligence.settings.AdvancedInfoLevel
import com.supreme.priceintelligence.settings.CustomRetailerChartColors
import com.supreme.priceintelligence.settings.GraphPointMode
import com.supreme.priceintelligence.settings.GraphSize
import com.supreme.priceintelligence.settings.HistoryGraphStyle
import com.supreme.priceintelligence.settings.PriceHistoryRange
import com.supreme.priceintelligence.settings.RetailerChartPalette
import com.supreme.priceintelligence.ui.theme.retailerChartColors
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
internal fun PriceHistorySection(
    entries: List<PriceHistoryEntry>,
    isLoading: Boolean,
    shopPrice: Double,
    informationLevel: AdvancedInfoLevel =
        AdvancedInfoLevel.STANDARD,
    range: PriceHistoryRange =
        PriceHistoryRange.THIRTY_DAYS,
    graphStyle: HistoryGraphStyle =
        HistoryGraphStyle.LINE,
    graphSize: GraphSize = GraphSize.STANDARD,
    pointMode: GraphPointMode =
        GraphPointMode.TAP_ONLY,
    retailerChartPalette: RetailerChartPalette =
        RetailerChartPalette.ORIGINAL,
    customRetailerChartColors:
        CustomRetailerChartColors =
        CustomRetailerChartColors()
) {
    val oldestAllowedTimestamp =
        Clock.System.now().toEpochMilliseconds() -
            range.days * 86_400_000L

    val displayEntries = entries
        .asSequence()
        .filter { entry ->
            entry.retailer in PriceRetailer.entries.map { it.name } &&
                entry.price.isFinite() &&
                entry.price > 0.0 &&
                entry.checkedAt >= oldestAllowedTimestamp
        }
        .sortedWith(
            compareBy<PriceHistoryEntry> { entry ->
                entry.checkedAt
            }.thenBy { entry ->
                entry.id
            }
        )
        .distinctBy { entry ->
            Triple(
                entry.retailer,
                (entry.checkedAt + 19_800_000L) / 86_400_000L,
                (entry.price * 100.0 + 0.5).toLong()
            )
        }
        .toList()

    val summaries = PriceRetailer.entries.mapNotNull { retailer ->
        summarizePriceHistory(displayEntries, retailer)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Price history",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text =
                "Shows the last ${range.days} days. Up to 60 successful checks " +
                    "per retailer are kept on this device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        when {
            isLoading -> LoadingPriceHistory()
            summaries.isEmpty() -> EmptyPriceHistory()
            else -> summaries.forEach { summary ->
                RetailerPriceHistoryCard(
                    summary = summary,
                    entries = displayEntries,
                    shopPrice = shopPrice,
                    informationLevel = informationLevel,
                    graphStyle = graphStyle,
                    graphSize = graphSize,
                    pointMode = pointMode,
                    retailerChartPalette =
                        retailerChartPalette,
                    customRetailerChartColors =
                        customRetailerChartColors
                )
            }
        }
    }
}

@Composable
private fun LoadingPriceHistory() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Loading saved checks...",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EmptyPriceHistory() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.supremeColors.panel,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.supremeColors.border
        )
    ) {
        Text(
            text = "History will appear after the first successful price check.",
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RetailerPriceHistoryCard(
    summary: RetailerPriceHistorySummary,
    entries: List<PriceHistoryEntry>,
    shopPrice: Double,
    informationLevel: AdvancedInfoLevel,
    graphStyle: HistoryGraphStyle,
    graphSize: GraphSize,
    pointMode: GraphPointMode,
    retailerChartPalette: RetailerChartPalette,
    customRetailerChartColors:
        CustomRetailerChartColors
) {
    val retailerName = when (summary.retailer) {
        PriceRetailer.AMAZON -> "Amazon"
        PriceRetailer.FLIPKART -> "Flipkart"
    }
    val movementText = movementDescription(summary)
    val retailerPrices = entries
        .filter { it.retailer == summary.retailer.name }
        .map { it.price }
    val averagePrice = retailerPrices
        .takeIf { it.isNotEmpty() }
        ?.average()
    val movementColor = when (summary.movement) {
        // A lower online price is bad for the shop.
        PriceMovement.LOWER -> MaterialTheme.colorScheme.error

        // A higher online price is good for the shop.
        PriceMovement.HIGHER -> MaterialTheme.supremeColors.competitive

        PriceMovement.UNCHANGED,
        PriceMovement.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.supremeColors.panel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.supremeColors.border
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = retailerName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (summary.observationCount == 1) {
                        "1 saved check"
                    } else {
                        "${summary.observationCount} saved checks"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (informationLevel == AdvancedInfoLevel.ESSENTIAL) {
                PriceHistoryMetric(
                    label = "Latest",
                    value = formatIndianPrice(summary.latestPrice)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PriceHistoryMetric(
                        label = "Latest",
                        value = formatIndianPrice(summary.latestPrice),
                        modifier = Modifier.weight(1f)
                    )
                    PriceHistoryMetric(
                        label = "Lowest saved",
                        value = formatIndianPrice(summary.lowestSavedPrice),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (informationLevel == AdvancedInfoLevel.FULL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PriceHistoryMetric(
                            label = "Highest saved",
                            value = formatIndianPrice(summary.highestSavedPrice),
                            modifier = Modifier.weight(1f)
                        )
                        PriceHistoryMetric(
                            label = "Average",
                            value = averagePrice?.let(::formatIndianPrice) ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Text(
                text = movementText,
                color = movementColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            if (informationLevel != AdvancedInfoLevel.ESSENTIAL) {
                PriceHistoryLineGraph(
                    entries = entries,
                    retailer = summary.retailer,
                    shopPrice = shopPrice,
                    graphStyle = graphStyle,
                    graphSize = graphSize,
                    pointMode = pointMode,
                    retailerChartPalette =
                        retailerChartPalette,
                    customRetailerChartColors =
                        customRetailerChartColors
                )
            }

            Text(
                text = "Latest saved ${formatTimeAgo(summary.latestCheckedAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun PriceHistoryLineGraph(
    entries: List<PriceHistoryEntry>,
    retailer: PriceRetailer,
    shopPrice: Double,
    graphStyle: HistoryGraphStyle,
    graphSize: GraphSize,
    pointMode: GraphPointMode,
    retailerChartPalette: RetailerChartPalette,
    customRetailerChartColors:
        CustomRetailerChartColors
) {
    val points = entries
        .asSequence()
        .filter { entry ->
            entry.retailer == retailer.name &&
                entry.price.isFinite() &&
                entry.price > 0.0 &&
                entry.checkedAt > 0L
        }
        .sortedWith(
            compareBy<PriceHistoryEntry> { entry ->
                entry.checkedAt
            }.thenBy { entry ->
                entry.id
            }
        )
        .toList()

    if (points.isEmpty()) {
        return
    }

    var selectedPoint by remember(points, pointMode) {
        mutableStateOf<PriceHistoryEntry?>(
            if (pointMode == GraphPointMode.ALWAYS_LATEST) {
                points.last()
            } else {
                null
            }
        )
    }

    val retailerName = when (retailer) {
        PriceRetailer.AMAZON -> "Amazon"
        PriceRetailer.FLIPKART -> "Flipkart"
    }

    val retailerColors =
        retailerChartPalette.retailerChartColors(
            customRetailerChartColors
        )

    val lineColor = when (retailer) {
        PriceRetailer.AMAZON ->
            retailerColors.amazon

        PriceRetailer.FLIPKART ->
            retailerColors.flipkart
    }

    val gridColor =
        MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)

    val shopLineColor =
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

    val validShopPrice =
        shopPrice.takeIf { price ->
            price.isFinite() && price > 0.0
        }

    val latestPrice = points.last().price

    val graphDescription =
        "$retailerName price history with ${points.size} saved checks. " +
            "Latest price ${formatIndianPrice(latestPrice)}."

    val chartSurfaceColor =
        MaterialTheme.supremeColors.panelMuted

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.11f),
                        chartSurfaceColor,
                        chartSurfaceColor
                    )
                )
            )
            .border(
                width = 1.dp,
                color = lineColor.copy(alpha = 0.22f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(graphSize.heightDp.dp)
                .pointerInput(points, pointMode) {
                    if (pointMode != GraphPointMode.HIDDEN) {
                        detectTapGestures { tapPosition ->
                            val horizontalInset = 10.dp.toPx()
                            val graphWidth = (
                                size.width.toFloat() -
                                    horizontalInset * 2f
                                ).coerceAtLeast(1f)

                            val firstTimestamp =
                                points.first().checkedAt

                            val timestampRange = (
                                points.last().checkedAt -
                                    firstTimestamp
                                ).coerceAtLeast(1L)

                            selectedPoint = points.minByOrNull { entry ->
                                val pointX =
                                    if (points.size == 1) {
                                        horizontalInset +
                                            graphWidth / 2f
                                    } else {
                                        val fraction = (
                                            (
                                                entry.checkedAt -
                                                    firstTimestamp
                                                ).toDouble() /
                                                timestampRange.toDouble()
                                            ).toFloat()

                                        horizontalInset +
                                            graphWidth * fraction
                                    }

                                kotlin.math.abs(
                                    pointX - tapPosition.x
                                )
                            }
                        }
                    }
                }
                .semantics {
                    contentDescription =
                        if (pointMode == GraphPointMode.HIDDEN) {
                            graphDescription
                        } else {
                            "$graphDescription Tap the graph to inspect a saved price."
                        }
                }
        ) {
            val horizontalInset = 10.dp.toPx()
            val verticalInset = 12.dp.toPx()

            val graphWidth =
                (size.width - horizontalInset * 2f)
                    .coerceAtLeast(1f)

            val graphHeight =
                (size.height - verticalInset * 2f)
                    .coerceAtLeast(1f)

            val displayedPrices = buildList {
                points.forEach { entry ->
                    add(entry.price)
                }
                validShopPrice?.let { price ->
                    add(price)
                }
            }

            val rawMinimum = displayedPrices.minOrNull() ?: latestPrice
            val rawMaximum = displayedPrices.maxOrNull() ?: latestPrice
            val rawRange = rawMaximum - rawMinimum

            val pricePadding = maxOf(
                rawRange * 0.12,
                rawMaximum * 0.01,
                1.0
            )

            val chartMinimum =
                (rawMinimum - pricePadding).coerceAtLeast(0.0)

            val chartMaximum =
                rawMaximum + pricePadding

            val chartRange =
                (chartMaximum - chartMinimum).coerceAtLeast(1.0)

            val firstTimestamp = points.first().checkedAt
            val lastTimestamp = points.last().checkedAt
            val timestampRange =
                (lastTimestamp - firstTimestamp).coerceAtLeast(1L)

            fun xPosition(entry: PriceHistoryEntry): Float {
                if (points.size == 1) {
                    return horizontalInset + graphWidth / 2f
                }

                val fraction = (
                    (entry.checkedAt - firstTimestamp).toDouble() /
                        timestampRange.toDouble()
                    ).toFloat()

                return horizontalInset + graphWidth * fraction
            }

            fun yPosition(price: Double): Float {
                val fraction = (
                    (price - chartMinimum) / chartRange
                    ).toFloat()

                return verticalInset +
                    graphHeight * (1f - fraction)
            }

            repeat(3) { index ->
                val y =
                    verticalInset +
                        graphHeight * index.toFloat() / 2f

                drawLine(
                    color = gridColor,
                    start = Offset(horizontalInset, y),
                    end = Offset(
                        horizontalInset + graphWidth,
                        y
                    ),
                    strokeWidth = 1.dp.toPx()
                )
            }

            validShopPrice?.let { price ->
                val y = yPosition(price)

                drawLine(
                    color = shopLineColor,
                    start = Offset(horizontalInset, y),
                    end = Offset(
                        horizontalInset + graphWidth,
                        y
                    ),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(
                            7.dp.toPx(),
                            5.dp.toPx()
                        )
                    )
                )
            }

            val pointOffsets = points.map { entry ->
                Offset(
                    x = xPosition(entry),
                    y = yPosition(entry.price)
                )
            }

            val chartBottom =
                verticalInset + graphHeight

            val linePath =
                premiumChartLinePath(
                    points = pointOffsets,
                    graphStyle = graphStyle
                )

            val areaPath =
                premiumChartAreaPath(
                    points = pointOffsets,
                    graphStyle = graphStyle,
                    baselineY = chartBottom
                )

            val areaStrength =
                if (graphStyle == HistoryGraphStyle.AREA) {
                    0.24f
                } else {
                    0.08f
                }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = areaStrength),
                        lineColor.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    startY = verticalInset,
                    endY = chartBottom
                )
            )

            if (pointOffsets.size > 1) {
                drawPath(
                    path = linePath,
                    color = lineColor.copy(alpha = 0.12f),
                    style = Stroke(
                        width = 11.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor.copy(alpha = 0.28f),
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            selectedPoint?.let { entry ->
                val selectedX =
                    xPosition(entry)

                drawLine(
                    color = lineColor.copy(alpha = 0.34f),
                    start = Offset(
                        x = selectedX,
                        y = verticalInset
                    ),
                    end = Offset(
                        x = selectedX,
                        y = chartBottom
                    ),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(
                            4.dp.toPx(),
                            4.dp.toPx()
                        )
                    )
                )
            }

            points.forEachIndexed { index, entry ->
                val pointCenter =
                    pointOffsets[index]

                val isLatest =
                    index == points.lastIndex

                val isSelected =
                    selectedPoint == entry

                if (isLatest && !isSelected) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.18f),
                        radius = 8.dp.toPx(),
                        center = pointCenter
                    )
                }

                drawCircle(
                    color = chartSurfaceColor,
                    radius =
                        if (isLatest) {
                            4.8.dp.toPx()
                        } else {
                            3.8.dp.toPx()
                        },
                    center = pointCenter
                )

                drawCircle(
                    color = lineColor,
                    radius =
                        if (isLatest) {
                            3.2.dp.toPx()
                        } else {
                            2.4.dp.toPx()
                        },
                    center = pointCenter
                )
            }

            selectedPoint?.let { entry ->
                val selectedCenter = Offset(
                    x = xPosition(entry),
                    y = yPosition(entry.price)
                )

                drawCircle(
                    color = lineColor.copy(alpha = 0.16f),
                    radius = 14.dp.toPx(),
                    center = selectedCenter
                )

                drawCircle(
                    color = chartSurfaceColor,
                    radius = 7.dp.toPx(),
                    center = selectedCenter
                )

                drawCircle(
                    color = lineColor,
                    radius = 4.5.dp.toPx(),
                    center = selectedCenter
                )
            }
        }

        selectedPoint?.let { entry ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = lineColor.copy(alpha = 0.14f),
                border = BorderStroke(
                    width = 1.dp,
                    color = lineColor.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 9.dp
                    ),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text =
                                "Saved on ${formatHistoryDate(entry.checkedAt)}",
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = formatTimeAgo(entry.checkedAt),
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = formatIndianPrice(entry.price),
                        color = lineColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "OLDER",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "LATEST",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        validShopPrice?.let { price ->
            Text(
                text =
                    "Dashed line: Supreme price " +
                        formatIndianPrice(price),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }

        if (points.size == 1) {
            Text(
                text = "One check saved. The trend line will appear after another successful check.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatHistoryDate(timestamp: Long): String {
    val indiaTime = Instant
        .fromEpochMilliseconds(timestamp + 19_800_000L)
        .toString()
    val date = indiaTime.substringBefore('T')
    val time = indiaTime.substringAfter('T').take(5)

    return date + " at " + time + " IST"
}

private fun movementDescription(
    summary: RetailerPriceHistorySummary
): String =
    when (summary.movement) {
        PriceMovement.LOWER -> buildMovementDescription("Down", summary)
        PriceMovement.HIGHER -> buildMovementDescription("Up", summary)
        PriceMovement.UNCHANGED -> "Unchanged since the previous check"
        PriceMovement.UNKNOWN -> "First saved check"
    }

private fun buildMovementDescription(
    direction: String,
    summary: RetailerPriceHistorySummary
): String = buildString {
    append(direction)
    append(" ")
    append(formatIndianPrice(summary.movementAmount ?: 0.0))
    summary.movementPercent?.let { percent ->
        append(" (")
        append(formatPercent(percent))
        append(")")
    }
    append(" since the previous check")
}

@Composable
private fun PriceHistoryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(ExperimentalTime::class)
internal fun formatTimeAgo(timeMs: Long): String {
    val elapsedMs = (
            Clock.System.now().toEpochMilliseconds() - timeMs
            ).coerceAtLeast(0L)
    val elapsedMinutes = elapsedMs / 60_000L
    val elapsedHours = elapsedMinutes / 60L
    val elapsedDays = elapsedHours / 24L

    return when {
        elapsedDays > 0L -> "$elapsedDays day${if (elapsedDays == 1L) "" else "s"} ago"
        elapsedHours > 0L -> "$elapsedHours hour${if (elapsedHours == 1L) "" else "s"} ago"
        elapsedMinutes > 0L -> "$elapsedMinutes minute${if (elapsedMinutes == 1L) "" else "s"} ago"
        else -> "recently"
    }
}

internal fun formatPercent(value: Double): String {
    val tenths = (value.absoluteValue * 10.0).roundToLong()
    val whole = tenths / 10
    val decimal = tenths % 10
    return if (decimal == 0L) "$whole%" else "$whole.$decimal%"
}