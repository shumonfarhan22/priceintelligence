@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.data.PriceRetailer
import com.supreme.priceintelligence.settings.InsightCustomization
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlin.math.absoluteValue
import kotlin.time.Clock

private enum class AnalysisTone {
    GOOD,
    BAD,
    WARNING,
    NEUTRAL
}

private data class AnalysisMessage(
    val label: String,
    val headline: String,
    val explanation: String,
    val recommendation: String,
    val tone: AnalysisTone
)

private data class OnlineEvidence(
    val retailer: String,
    val price: Double
)

@Composable
internal fun InsightProductAnalysisDialog(
    product: InsightProduct,
    activeGroup: InsightGroup?,
    activeBrand: String?,
    contextProductCount: Int,
    priceHistory: List<PriceHistoryEntry>,
    isHistoryLoading: Boolean,
    isRefreshing: Boolean,
    isConnected: Boolean,
    insightCustomization: InsightCustomization,
    reduceMotionEnabled: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = remember(
        product,
        activeGroup,
        activeBrand,
        contextProductCount
    ) {
        buildAnalysisMessage(
            product = product,
            group = activeGroup,
            brand = activeBrand,
            groupSize = contextProductCount
        )
    }

    val refreshSpotlightPosition = remember {
        Animatable(-0.18f)
    }

    LaunchedEffect(
        isRefreshing,
        reduceMotionEnabled
    ) {
        when {
            !isRefreshing -> {
                refreshSpotlightPosition.snapTo(-0.18f)
            }

            reduceMotionEnabled -> {
                refreshSpotlightPosition.snapTo(0.12f)
            }

            else -> {
                refreshSpotlightPosition.snapTo(-0.18f)

                while (true) {
                    refreshSpotlightPosition.animateTo(
                        targetValue = 1.18f,
                        animationSpec = tween(
                            durationMillis = 1500,
                            easing = LinearEasing
                        )
                    )

                    refreshSpotlightPosition.animateTo(
                        targetValue = -0.18f,
                        animationSpec = tween(
                            durationMillis = 1500,
                            easing = LinearEasing
                        )
                    )
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.supremeColors.panelStrong,
            border = BorderStroke(
                1.dp,
                MaterialTheme.supremeColors.border
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (isRefreshing) {
                            val spotlightCenterY =
                                size.height *
                                    refreshSpotlightPosition.value
                            val spotlightHalfHeight =
                                120.dp.toPx()

                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFF59E0B).copy(
                                            alpha = 0.08f
                                        ),
                                        Color(0xFFFBBF24).copy(
                                            alpha = 0.30f
                                        ),
                                        Color(0xFFF59E0B).copy(
                                            alpha = 0.08f
                                        ),
                                        Color.Transparent
                                    ),
                                    startY =
                                        spotlightCenterY -
                                            spotlightHalfHeight,
                                    endY =
                                        spotlightCenterY +
                                            spotlightHalfHeight
                                )
                            )
                        }
                    }
            ) {
                AnalysisHeader(
                    label = message.label,
                    onBack = onBack,
                    onDismiss = onDismiss
                )

                HorizontalDivider(
                    color = MaterialTheme.supremeColors.border
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ProductIdentity(product)
                    }

                    item {
                        VerdictCard(message)
                    }

                    item {
                        PriceEvidenceCard(product)
                    }

                    item {
                        MarginAndConfidenceCard(product)
                    }

                    item {
                        HistoryAnalysisCard(
                            product = product,
                            history = priceHistory,
                            isLoading = isHistoryLoading,
                            customization = insightCustomization
                        )
                    }

                    item {
                        AnalysisSection(
                            "RECOMMENDED NEXT STEP"
                        ) {
                            Text(
                                text = message.recommendation,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    item {
                        RefreshAnalysisButton(
                            hasRetailerLink =
                                !product.item.amazonUrl
                                    .isNullOrBlank() ||
                                        !product.item.flipkartUrl
                                            .isNullOrBlank(),
                            isConnected = isConnected,
                            isRefreshing = isRefreshing,
                            onClick = onRefresh
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisHeader(
    label: String,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 4.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector =
                    Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to product list"
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "PRODUCT ANALYSIS",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )

            Text(
                text = label,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription =
                    "Close product analysis"
            )
        }
    }
}

@Composable
private fun ProductIdentity(
    product: InsightProduct
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductAnalysisImage(
            item = product.item,
            modifier = Modifier.size(112.dp)
        )

        Spacer(Modifier.width(13.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = product.item.productName,
                color =
                    MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                shape = RoundedCornerShape(50),
                color =
                    product.statusColor()
                        .copy(alpha = 0.14f)
            ) {
                Text(
                    text = product.basicReason(),
                    color = product.statusColor(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 9.dp,
                        vertical = 5.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun VerdictCard(
    message: AnalysisMessage
) {
    val color = message.tone.color()

    val icon: ImageVector =
        when (message.tone) {
            AnalysisTone.GOOD ->
                Icons.Rounded.ShowChart

            AnalysisTone.BAD ->
                Icons.Rounded.PriorityHigh

            AnalysisTone.WARNING ->
                Icons.Rounded.Schedule

            AnalysisTone.NEUTRAL ->
                Icons.Rounded.ShowChart
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.11f),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        color.copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(11.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = message.headline,
                    color = color,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = message.explanation,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PriceEvidenceCard(
    product: InsightProduct
) {
    val item = product.item
    val online = item.lowestOnline()

    val difference =
        online?.let {
            item.shopPrice - it.price
        }

    val percent =
        difference?.let {
            if (item.shopPrice > 0.0) {
                it.absoluteValue /
                        item.shopPrice *
                        100.0
            } else {
                null
            }
        }

    AnalysisSection("PRICE EVIDENCE") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                label = "SHOP",
                value =
                    item.shopPrice
                        .validPrice()
                        ?.let(::formatIndianPrice)
                        ?: "Not set",
                modifier = Modifier.weight(1f)
            )

            MetricTile(
                label = "BEST ONLINE",
                value =
                    online?.price
                        ?.let(::formatIndianPrice)
                        ?: "Unavailable",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                label = "AMAZON • SAVED",
                value =
                    item.amazonLastPrice
                        .validPrice()
                        ?.let(::formatIndianPrice)
                        ?: "Unavailable",
                modifier = Modifier.weight(1f)
            )

            MetricTile(
                label = "FLIPKART • SAVED",
                value =
                    item.flipkartLastPrice
                        .validPrice()
                        ?.let(::formatIndianPrice)
                        ?: "Unavailable",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(11.dp))

        val summary =
            when {
                difference == null ->
                    "No usable online price is available for comparison."

                difference > 0.01 ->
                    "Shop is ${formatIndianPrice(difference)} higher than ${online.retailer}" +
                            percent?.let {
                                " (${formatPercent(it)})"
                            }.orEmpty() +
                            "."

                difference < -0.01 ->
                    "Shop is ${formatIndianPrice(difference.absoluteValue)} lower than ${online.retailer}" +
                            percent?.let {
                                " (${formatPercent(it)})"
                            }.orEmpty() +
                            "."

                else ->
                    "The shop price matches the best saved online price."
            }

        Text(
            text = summary,
            color =
                when {
                    difference == null ->
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant

                    difference > 0.01 ->
                        MaterialTheme.colorScheme.error

                    else ->
                        MaterialTheme
                            .supremeColors
                            .competitive
                },
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(11.dp))

        RetailerEvidence(
            name = "Amazon",
            hasLink =
                !item.amazonUrl.isNullOrBlank(),
            price = item.amazonLastPrice,
            checkedAt = item.amazonLastChecked,
            isFresh = product.amazonFresh
        )

        Spacer(Modifier.height(7.dp))

        RetailerEvidence(
            name = "Flipkart",
            hasLink =
                !item.flipkartUrl.isNullOrBlank(),
            price = item.flipkartLastPrice,
            checkedAt = item.flipkartLastChecked,
            isFresh = product.flipkartFresh
        )
    }
}

@Composable
private fun MarginAndConfidenceCard(
    product: InsightProduct
) {
    val item = product.item
    val cost = item.purchaseCost.validPrice()

    val linked =
        listOf(
            item.amazonUrl,
            item.flipkartUrl
        ).count {
            !it.isNullOrBlank()
        }

    val priced =
        listOf(
            item.amazonLastPrice,
            item.flipkartLastPrice
        ).count {
            it.validPrice() != null
        }

    val fresh =
        listOf(
            product.amazonFresh,
            product.flipkartFresh
        ).count { it }

    val confidence =
        when {
            linked == 2 && fresh == 2 ->
                "High confidence"

            priced > 0 ->
                "Medium confidence"

            else ->
                "Low confidence"
        }

    AnalysisSection("MARGIN & CONFIDENCE") {
        if (
            cost == null ||
            item.shopPrice.validPrice() == null
        ) {
            Text(
                text =
                    "Purchase cost is missing, so profit and margin cannot be calculated safely.",
                color =
                    MaterialTheme.supremeColors.warning,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        } else {
            val profit = item.shopPrice - cost
            val margin =
                profit / item.shopPrice * 100.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    label = "PURCHASE COST",
                    value = formatIndianPrice(cost),
                    modifier = Modifier.weight(1f)
                )

                MetricTile(
                    label = "PROFIT",
                    value = formatIndianPrice(profit),
                    valueColor =
                        if (profit >= 0.0) {
                            MaterialTheme
                                .supremeColors
                                .competitive
                        } else {
                            MaterialTheme
                                .colorScheme
                                .error
                        },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text =
                    "Current shop margin: ${formatPercent(margin)}",
                color =
                    if (margin >= 0.0) {
                        MaterialTheme
                            .supremeColors
                            .competitive
                    } else {
                        MaterialTheme
                            .colorScheme
                            .error
                    },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        HorizontalDivider(
            color =
                MaterialTheme.supremeColors.border
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = confidence,
            color =
                when (confidence) {
                    "High confidence" ->
                        MaterialTheme
                            .supremeColors
                            .competitive

                    "Medium confidence" ->
                        MaterialTheme
                            .supremeColors
                            .warning

                    else ->
                        MaterialTheme
                            .colorScheme
                            .error
                },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text =
                when (confidence) {
                    "High confidence" ->
                        "Both retailers have fresh saved prices."

                    "Medium confidence" ->
                        "Some usable evidence exists, but it is incomplete or due for checking."

                    else ->
                        "There is not enough retailer data for a reliable comparison."
                },
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun HistoryAnalysisCard(
    product: InsightProduct,
    history: List<PriceHistoryEntry>,
    isLoading: Boolean,
    customization: InsightCustomization
) {
    val rangeDays =
        customization.priceHistoryRange.days

    val points = remember(
        product.item.id,
        history,
        rangeDays
    ) {
        val cutoff =
            Clock.System.now()
                .toEpochMilliseconds() -
                    rangeDays *
                    PRICE_MOVEMENT_DAY_MILLIS

        history.filter {
            it.inventoryItemId ==
                    product.item.id &&
                    it.price.isFinite() &&
                    it.price > 0.0 &&
                    it.checkedAt >= cutoff
        }
    }

    val amazon = remember(points) {
        points.toGraphPoints(
            PriceRetailer.AMAZON
        )
    }

    val flipkart = remember(points) {
        points.toGraphPoints(
            PriceRetailer.FLIPKART
        )
    }

    AnalysisSection(
        "PRICE HISTORY • ${rangeDays} DAYS"
    ) {
        when {
            isLoading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement =
                        Arrangement.Center,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(9.dp))

                    Text(
                        text =
                            "Loading saved price history…",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            amazon.isEmpty() &&
                    flipkart.isEmpty() -> {
                Text(
                    text =
                        "No saved observations are available in this period. The graph will appear after successful price checks.",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            else -> {
                Text(
                    text =
                        "Tap a point to see its retailer, date, price and change.",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    fontSize = 10.sp
                )

                Spacer(Modifier.height(10.dp))

                InteractiveProductMovementLineChart(
                    amazonHistory = amazon,
                    flipkartHistory = flipkart,
                    graphStyle =
                        customization.historyGraphStyle,
                    pointMode =
                        customization.graphPointMode,
                    retailerChartPalette =
                        customization
                            .retailerChartPalette,
                    customRetailerChartColors =
                        customization
                            .customRetailerChartColors,
                    graphHeightDp =
                        customization.graphSize.heightDp
                )
            }
        }
    }
}

@Composable
private fun RefreshAnalysisButton(
    hasRetailerLink: Boolean,
    isConnected: Boolean,
    isRefreshing: Boolean,
    onClick: () -> Unit
) {
    val enabled =
        hasRetailerLink && isConnected && !isRefreshing

    val label =
        when {
            isRefreshing ->
                "Checking live prices…"

            !hasRetailerLink ->
                "Retailer link required"

            !isConnected ->
                "Offline — reconnect to refresh"

            else ->
                "Refresh live prices"
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color =
            when {
                isRefreshing ->
                    Color(0xFFF59E0B).copy(alpha = 0.16f)

                enabled ->
                    MaterialTheme.colorScheme.primary

                else ->
                    MaterialTheme.supremeColors.panel
            },
        border =
            if (enabled) {
                null
            } else {
                BorderStroke(
                    1.dp,
                    if (isRefreshing) {
                        Color(0xFFFBBF24).copy(alpha = 0.70f)
                    } else {
                        MaterialTheme.supremeColors.border
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement =
                Arrangement.Center,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    color = Color(0xFFFBBF24),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint =
                        if (enabled) {
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        },
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = label,
                color =
                    when {
                        isRefreshing ->
                            Color(0xFFFBBF24)

                        enabled ->
                            MaterialTheme
                                .colorScheme
                                .onPrimary

                        else ->
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AnalysisSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            1.dp,
            MaterialTheme.supremeColors.border
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp
            )

            Spacer(Modifier.height(9.dp))

            content()
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color =
        MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color =
            MaterialTheme.supremeColors.panelStrong,
        border = BorderStroke(
            1.dp,
            MaterialTheme.supremeColors.border
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RetailerEvidence(
    name: String,
    hasLink: Boolean,
    price: Double?,
    checkedAt: Long?,
    isFresh: Boolean
) {
    val value = price.validPrice()

    val status =
        when {
            !hasLink ->
                "Retailer link missing"

            value == null ->
                "No usable saved price"

            checkedAt == null ||
                    checkedAt <= 0L ->
                "Saved price • check time unavailable"

            isFresh ->
                "Saved • ${formatTimeAgo(checkedAt)} • fresh"

            else ->
                "Saved • ${formatTimeAgo(checkedAt)} • check due"
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color =
                    MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = status,
                color =
                    if (isFresh) {
                        MaterialTheme
                            .supremeColors
                            .competitive
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    },
                fontSize = 10.sp
            )
        }

        Text(
            text =
                value?.let(::formatIndianPrice)
                    ?: "—",
            color =
                MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProductAnalysisImage(
    item: InventoryItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                MaterialTheme.supremeColors.imagePanel
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text =
                item.productName
                    .trim()
                    .firstOrNull()
                    ?.uppercase()
                    ?: "P",
            color = Color(0xFF475569),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription =
                    "${item.productName} product image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun InsightProduct.statusColor():
        Color =
    when (position) {
        InsightPosition.COMPETITIVE ->
            MaterialTheme
                .supremeColors
                .competitive

        InsightPosition.REVIEW ->
            MaterialTheme.colorScheme.error

        InsightPosition.NO_COMPARISON ->
            MaterialTheme.supremeColors.warning
    }

@Composable
private fun AnalysisTone.color():
        Color =
    when (this) {
        AnalysisTone.GOOD ->
            MaterialTheme
                .supremeColors
                .competitive

        AnalysisTone.BAD ->
            MaterialTheme.colorScheme.error

        AnalysisTone.WARNING ->
            MaterialTheme.supremeColors.warning

        AnalysisTone.NEUTRAL ->
            MaterialTheme.colorScheme.primary
    }

private fun buildAnalysisMessage(
    product: InsightProduct,
    group: InsightGroup?,
    brand: String?,
    groupSize: Int
): AnalysisMessage {
    val item = product.item
    val online = item.lowestOnline()

    val gap =
        online?.let {
            (
                    item.shopPrice -
                            it.price
                    ).absoluteValue
        }

    val gapText =
        gap?.let {
            val percent =
                if (item.shopPrice > 0.0) {
                    it / item.shopPrice * 100.0
                } else {
                    0.0
                }

            "${formatIndianPrice(it)} (${formatPercent(percent)})"
        } ?: "an unknown amount"

    if (
        brand != null &&
        group == null
    ) {
        return when (product.position) {
            InsightPosition.COMPETITIVE ->
                AnalysisMessage(
                    label = "$brand BRAND HEALTH",
                    headline =
                        "This product supports the brand’s competitive position",
                    explanation =
                        "It is one of $groupSize $brand products in this analysis and is not above the best saved online price.",
                    recommendation =
                        if (product.needsCheck) {
                            "Refresh the retailer prices before relying on this position."
                        } else {
                            "Keep monitoring the product and confirm that its margin remains healthy."
                        },
                    tone = AnalysisTone.GOOD
                )

            InsightPosition.REVIEW ->
                AnalysisMessage(
                    label = "$brand BRAND HEALTH",
                    headline =
                        "This product weakens the brand’s price position",
                    explanation =
                        "${online?.retailer ?: "An online retailer"} is $gapText lower than the shop price.",
                    recommendation =
                        "Review the selling price and margin. Refresh first if the retailer price is old.",
                    tone = AnalysisTone.BAD
                )

            InsightPosition.NO_COMPARISON ->
                AnalysisMessage(
                    label = "$brand BRAND HEALTH",
                    headline =
                        "This product has no reliable brand comparison",
                    explanation =
                        "A usable online price is not currently available.",
                    recommendation =
                        "Add or check retailer links, then refresh this product.",
                    tone = AnalysisTone.WARNING
                )
        }
    }

    return when (group) {
        InsightGroup.COMPETITIVE_FRESH ->
            AnalysisMessage(
                label = "COMPETITIVE • FRESH",
                headline =
                    item.competitiveHeadline(
                        online,
                        gapText
                    ),
                explanation =
                    "Recent saved retailer prices support this competitive result.",
                recommendation =
                    "Keep monitoring it and confirm that the purchase cost leaves a healthy margin.",
                tone = AnalysisTone.GOOD
            )

        InsightGroup.COMPETITIVE_DUE ->
            AnalysisMessage(
                label = "COMPETITIVE • CHECK DUE",
                headline =
                    "The last saved result was competitive",
                explanation =
                    "The retailer evidence is old enough that the market position may have changed.",
                recommendation =
                    "Refresh live prices before making a pricing decision.",
                tone = AnalysisTone.WARNING
            )

        InsightGroup.REVIEW_FRESH ->
            AnalysisMessage(
                label = "REVIEW • FRESH",
                headline =
                    "${online?.retailer ?: "Online"} is $gapText lower",
                explanation =
                    "A recent saved online price is below the shop price.",
                recommendation =
                    "Review the selling price together with purchase cost and margin. Do not reduce it automatically.",
                tone = AnalysisTone.BAD
            )

        InsightGroup.REVIEW_DUE ->
            AnalysisMessage(
                label = "REVIEW • CHECK DUE",
                headline =
                    "The saved comparison needs review, but its evidence is old",
                explanation =
                    "${online?.retailer ?: "An online retailer"} was $gapText lower at the last usable check.",
                recommendation =
                    "Refresh first, then review the selling price only if the disadvantage remains.",
                tone = AnalysisTone.WARNING
            )

        InsightGroup.AMAZON_PRESSURE ->
            item.retailerPressureMessage(
                retailer = "Amazon",
                price = item.amazonLastPrice
            )

        InsightGroup.FLIPKART_PRESSURE ->
            item.retailerPressureMessage(
                retailer = "Flipkart",
                price = item.flipkartLastPrice
            )

        InsightGroup.ONLINE_LOWER ->
            AnalysisMessage(
                label = "ONLINE PRICE LOWER",
                headline =
                    "${online?.retailer ?: "Online"} is $gapText lower",
                explanation =
                    "This saved gap placed the product in the online-lower group.",
                recommendation =
                    "Compare the gap with your margin and refresh if the retailer evidence is old.",
                tone = AnalysisTone.BAD
            )

        InsightGroup.NEAR_MATCH ->
            AnalysisMessage(
                label = "NEAR MARKET MATCH",
                headline =
                    "The shop and best online price are within 5%",
                explanation =
                    "The saved gap is $gapText, so a small market change could alter the result.",
                recommendation =
                    "Monitor the product and avoid unnecessary price changes while the difference remains small.",
                tone = AnalysisTone.NEUTRAL
            )

        InsightGroup.SHOP_LOWER ->
            AnalysisMessage(
                label = "SHOP PRICE LOWER",
                headline =
                    item.competitiveHeadline(
                        online,
                        gapText
                    ),
                explanation =
                    "The shop currently has a competitive price advantage.",
                recommendation =
                    "Check purchase cost to make sure the competitive price is still profitable.",
                tone = AnalysisTone.GOOD
            )

        InsightGroup.NEEDS_CHECK ->
            AnalysisMessage(
                label = "PRICE CHECK DUE",
                headline =
                    "The saved retailer evidence is no longer fresh",
                explanation =
                    item.latestCheck()?.let {
                        "The newest retailer check was ${formatTimeAgo(it)}."
                    }
                        ?: "No successful retailer check time is available.",
                recommendation =
                    "Run a manual live-price refresh before using this product for a decision.",
                tone = AnalysisTone.WARNING
            )

        InsightGroup.MISSING_LINKS ->
            AnalysisMessage(
                label = "MISSING RETAILER LINKS",
                headline =
                    "No Amazon or Flipkart link is saved",
                explanation =
                    "The app cannot fetch an online price without a retailer link.",
                recommendation =
                    "Edit the product in Inventory and add at least one valid retailer link.",
                tone = AnalysisTone.WARNING
            )

        InsightGroup.MISSING_PRICES ->
            AnalysisMessage(
                label = "MISSING SAVED PRICES",
                headline =
                    "A linked retailer has no usable saved price",
                explanation =
                    item.missingPriceRetailers().let {
                        if (it.isEmpty()) {
                            "The saved retailer data is incomplete."
                        } else {
                            "${it.joinToString()} has no usable saved price."
                        }
                    },
                recommendation =
                    "Refresh the linked retailer. If it still fails, verify the saved product link.",
                tone = AnalysisTone.WARNING
            )

        InsightGroup.MISSING_COSTS ->
            AnalysisMessage(
                label = "MISSING PURCHASE COST",
                headline =
                    "Profit and margin cannot be calculated",
                explanation =
                    "The selling price exists, but purchase cost is missing or invalid.",
                recommendation =
                    "Edit the product in Inventory and enter its purchase cost.",
                tone = AnalysisTone.WARNING
            )

        null ->
            AnalysisMessage(
                label = "PRODUCT INSIGHT",
                headline = product.basicReason(),
                explanation =
                    "This analysis uses the latest safe prices stored by the app.",
                recommendation =
                    "Review the evidence and refresh if any retailer price is old.",
                tone =
                    when (product.position) {
                        InsightPosition.COMPETITIVE ->
                            AnalysisTone.GOOD

                        InsightPosition.REVIEW ->
                            AnalysisTone.BAD

                        InsightPosition.NO_COMPARISON ->
                            AnalysisTone.WARNING
                    }
            )
    }
}

private fun InventoryItem.competitiveHeadline(
    online: OnlineEvidence?,
    gapText: String
): String {
    if (online == null) {
        return "The shop price is currently competitive"
    }

    val difference =
        shopPrice - online.price

    return if (difference < -0.01) {
        "Shop is $gapText below ${online.retailer}"
    } else {
        "Shop matches the best saved online price"
    }
}

private fun InventoryItem.retailerPressureMessage(
    retailer: String,
    price: Double?
): AnalysisMessage {
    val usable = price.validPrice()

    val gap =
        usable?.let {
            (
                    shopPrice -
                            it
                    ).absoluteValue
        }

    return AnalysisMessage(
        label =
            "$retailer PRICE PRESSURE".uppercase(),
        headline =
            "$retailer is below the shop price",
        explanation =
            gap?.let {
                "$retailer’s saved price is ${formatIndianPrice(it)} lower than the shop price."
            }
                ?: "$retailer does not currently have a usable saved price.",
        recommendation =
            "Check freshness and margin before changing the shop price.",
        tone = AnalysisTone.BAD
    )
}

private fun InsightProduct.basicReason():
        String {
    val positionText =
        when (position) {
            InsightPosition.COMPETITIVE ->
                "Shop price is competitive"

            InsightPosition.REVIEW ->
                "Online price is lower"

            InsightPosition.NO_COMPARISON ->
                "No usable online comparison"
        }

    return if (needsCheck) {
        "$positionText • Price check due"
    } else {
        "$positionText • Price is fresh"
    }
}

private fun InventoryItem.lowestOnline():
        OnlineEvidence? =
    listOfNotNull(
        amazonLastPrice.validPrice()?.let {
            OnlineEvidence("Amazon", it)
        },
        flipkartLastPrice.validPrice()?.let {
            OnlineEvidence("Flipkart", it)
        }
    ).minByOrNull {
        it.price
    }

private fun InventoryItem.latestCheck():
        Long? =
    listOfNotNull(
        amazonLastChecked?.takeIf {
            it > 0L
        },
        flipkartLastChecked?.takeIf {
            it > 0L
        }
    ).maxOrNull()

private fun InventoryItem.missingPriceRetailers():
        List<String> =
    buildList {
        if (
            !amazonUrl.isNullOrBlank() &&
            amazonLastPrice.validPrice() == null
        ) {
            add("Amazon")
        }

        if (
            !flipkartUrl.isNullOrBlank() &&
            flipkartLastPrice.validPrice() == null
        ) {
            add("Flipkart")
        }
    }

private fun List<PriceHistoryEntry>.toGraphPoints(
    retailer: PriceRetailer
): List<ShopPricePoint> =
    filter {
        it.retailer == retailer.name
    }
        .sortedBy {
            it.checkedAt
        }
        .map {
            ShopPricePoint(
                price = it.price,
                checkedAt = it.checkedAt
            )
        }

private fun Double?.validPrice(): Double? =
    this?.takeIf {
        it.isFinite() && it > 0.0
    }