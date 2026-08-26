package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supreme.priceintelligence.data.PriceRetailer
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlin.math.roundToInt

private val AmazonChartColor =
    Color(0xFFFF9900)

private val FlipkartChartColor =
    Color(0xFF2874F0)

@Composable
fun ShopPriceMovementDialog(
    snapshot: ShopPriceMovementSnapshot,
    isLoading: Boolean,
    errorMessage: String?,
    reduceMotionEnabled: Boolean,
    notificationTarget:
        PriceMovementNotificationTarget? = null,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRangeName by rememberSaveable {
        mutableStateOf(
            ShopMovementRange
                .THIRTY_DAYS
                .name
        )
    }

    var selectedRetailerName by rememberSaveable {
        mutableStateOf(
            ShopMovementRetailerFilter
                .ALL
                .name
        )
    }

    val selectedRange =
        ShopMovementRange.entries
            .firstOrNull { range ->
                range.name ==
                        selectedRangeName
            }
            ?: ShopMovementRange.THIRTY_DAYS

    val selectedRetailer =
        ShopMovementRetailerFilter.entries
            .firstOrNull { retailer ->
                retailer.name ==
                        selectedRetailerName
            }
            ?: ShopMovementRetailerFilter.ALL

    LaunchedEffect(
        notificationTarget?.requestId
    ) {
        notificationTarget?.let { target ->
            selectedRangeName =
                ShopMovementRange.THIRTY_DAYS.name

            selectedRetailerName =
                when (target.retailer) {
                    PriceRetailer.AMAZON ->
                        ShopMovementRetailerFilter
                            .AMAZON.name

                    PriceRetailer.FLIPKART ->
                        ShopMovementRetailerFilter
                            .FLIPKART.name
                }
        }
    }

    val movementView = remember(
        snapshot,
        selectedRange,
        selectedRetailer,
        notificationTarget
    ) {
        buildShopPriceMovementView(
            snapshot = snapshot,
            range = selectedRange,
            retailerFilter =
                selectedRetailer,
            notificationTarget =
                notificationTarget
        )
    }

    val motionProgress = remember {
        Animatable(0f)
    }

    var dismissRequested by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        dismissRequested,
        reduceMotionEnabled
    ) {
        if (dismissRequested) {
            if (reduceMotionEnabled) {
                motionProgress.snapTo(0f)
            } else {
                motionProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 170
                    )
                )
            }

            onDismiss()
        } else {
            if (reduceMotionEnabled) {
                motionProgress.snapTo(1f)
            } else {
                motionProgress.snapTo(0f)
                motionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 220
                    )
                )
            }
        }
    }

    val requestDismiss: () -> Unit = {
        if (!dismissRequested) {
            dismissRequested = true
        }
    }

    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress =
                        motionProgress.value

                    alpha = progress
                    scaleX =
                        0.985f +
                                0.015f * progress
                    scaleY =
                        0.985f +
                                0.015f * progress
                },
            color =
                MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                MovementHeader(
                    isLoading = isLoading,
                    onRefresh = onRefresh,
                    onBack = requestDismiss
                )

                HorizontalDivider(
                    color =
                        MaterialTheme
                            .supremeColors
                            .border
                )

                when {
                    isLoading -> {
                        MovementLoadingState(
                            modifier =
                                Modifier.weight(1f)
                        )
                    }

                    errorMessage != null &&
                            snapshot.generatedAt <= 0L -> {
                        MovementErrorState(
                            message = errorMessage,
                            onRetry = onRefresh,
                            modifier =
                                Modifier.weight(1f)
                        )
                    }

                    else -> {
                        MovementContent(
                            movementView =
                                movementView,
                            selectedRange =
                                selectedRange,
                            selectedRetailer =
                                selectedRetailer,
                            generatedAt =
                                snapshot.generatedAt,
                            notificationTarget =
                                notificationTarget,
                            reduceMotionEnabled =
                                reduceMotionEnabled,
                            onRangeSelected = {
                                selectedRangeName =
                                    it.name
                            },
                            onRetailerSelected = {
                                selectedRetailerName =
                                    it.name
                            },
                            modifier =
                                Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementHeader(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector =
                    Icons.AutoMirrored
                        .Rounded
                        .ArrowBack,
                contentDescription =
                    "Close Price Movement"
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "PRICE MOVEMENT",
                color =
                    MaterialTheme
                        .colorScheme
                        .primary,
                fontSize = 11.sp,
                fontWeight =
                    FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text =
                    "Amazon and Flipkart changes",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(
            onClick = onRefresh,
            enabled = !isLoading
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.Refresh,
                contentDescription =
                    "Reload price movement",
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }
    }
}

@Composable
private fun MovementContent(
    movementView: ShopPriceMovementView,
    selectedRange: ShopMovementRange,
    selectedRetailer:
    ShopMovementRetailerFilter,
    generatedAt: Long,
    notificationTarget:
        PriceMovementNotificationTarget?,
    reduceMotionEnabled: Boolean,
    onRangeSelected:
        (ShopMovementRange) -> Unit,
    onRetailerSelected:
        (ShopMovementRetailerFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(
        notificationTarget?.requestId,
        movementView.products
    ) {
        val target =
            notificationTarget
                ?: return@LaunchedEffect

        val productIndex =
            movementView.products
                .indexOfFirst { product ->
                    product.item.id ==
                        target.productId
                }

        if (productIndex >= 0) {
            val listIndex =
                productIndex + 3

            if (reduceMotionEnabled) {
                listState.scrollToItem(listIndex)
            } else {
                listState.animateScrollToItem(
                    listIndex
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 32.dp
        ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        item(
            key = "movement-filters"
        ) {
            MovementFilters(
                selectedRange =
                    selectedRange,
                selectedRetailer =
                    selectedRetailer,
                onRangeSelected =
                    onRangeSelected,
                onRetailerSelected =
                    onRetailerSelected
            )
        }

        item(
            key = "movement-overview"
        ) {
            MovementOverviewCard(
                movementView =
                    movementView,
                range = selectedRange,
                generatedAt =
                    generatedAt
            )
        }

        if (movementView.products.isEmpty()) {
            item(
                key = "movement-empty"
            ) {
                MovementEmptyState(
                    range = selectedRange,
                    retailer =
                        selectedRetailer
                )
            }
        } else {
            item(
                key = "movement-list-title"
            ) {
                Column {
                    Text(
                        text =
                            "CHANGED PRODUCTS",
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        fontSize = 11.sp,
                        fontWeight =
                            FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp
                    )

                    Text(
                        text =
                            "${movementView.changedProductCount} " +
                                    if (
                                        movementView
                                            .changedProductCount ==
                                        1
                                    ) {
                                        "product"
                                    } else {
                                        "products"
                                    },
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            items(
                items =
                    movementView.products,
                key = { product ->
                    product.item.id
                }
            ) { product ->
                MovementProductCard(
                    product = product,
                    generatedAt =
                        generatedAt,
                    notificationTarget =
                        notificationTarget
                            ?.takeIf { target ->
                                target.productId ==
                                    product.item.id
                            },
                    reduceMotionEnabled =
                        reduceMotionEnabled
                )
            }
        }
    }
}

@Composable
private fun MovementFilters(
    selectedRange: ShopMovementRange,
    selectedRetailer:
    ShopMovementRetailerFilter,
    onRangeSelected:
        (ShopMovementRange) -> Unit,
    onRetailerSelected:
        (ShopMovementRetailerFilter) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color =
            MaterialTheme
                .supremeColors
                .panel,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme
                    .supremeColors
                    .border
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Time period",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                ShopMovementRange.entries
                    .forEach { range ->
                        MovementFilterButton(
                            text =
                                range.buttonLabel,
                            selected =
                                range ==
                                        selectedRange,
                            onClick = {
                                onRangeSelected(
                                    range
                                )
                            },
                            modifier =
                                Modifier.weight(1f)
                        )
                    }
            }

            Text(
                text = "Retailer",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                ShopMovementRetailerFilter
                    .entries
                    .forEach { retailer ->
                        MovementFilterButton(
                            text =
                                retailer.buttonLabel,
                            selected =
                                retailer ==
                                        selectedRetailer,
                            onClick = {
                                onRetailerSelected(
                                    retailer
                                )
                            },
                            modifier =
                                Modifier.weight(1f)
                        )
                    }
            }
        }
    }
}

@Composable
private fun MovementFilterButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color =
            if (selected) {
                MaterialTheme
                    .colorScheme
                    .primaryContainer
            } else {
                MaterialTheme
                    .supremeColors
                    .panelMuted
            },
        border = BorderStroke(
            width = 1.dp,
            color =
                if (selected) {
                    MaterialTheme
                        .colorScheme
                        .primary
                } else {
                    MaterialTheme
                        .supremeColors
                        .border
                }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = text,
                color =
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MovementOverviewCard(
    movementView: ShopPriceMovementView,
    range: ShopMovementRange,
    generatedAt: Long
) {
    // A lower online price is bad for the shop.
    val lowerColor =
        MaterialTheme
            .colorScheme
            .error

    // A higher online price improves the shop's position.
    val higherColor =
        MaterialTheme
            .supremeColors
            .competitive

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color =
            MaterialTheme
                .supremeColors
                .panel,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme
                    .supremeColors
                    .border
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                                    .copy(
                                        alpha = 0.14f
                                    ),
                            shape = CircleShape
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.ShowChart,
                        contentDescription = null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Column {
                    Text(
                        text =
                            "${movementView.changes.size} price changes",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )

                    Text(
                        text =
                            "${movementView.changedProductCount} changed products",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                MovementMetric(
                    value =
                        movementView.lowerCount,
                    label = "Lower",
                    icon =
                        Icons.Rounded
                            .TrendingDown,
                    color = lowerColor,
                    modifier =
                        Modifier.weight(1f)
                )

                MovementMetric(
                    value =
                        movementView.higherCount,
                    label = "Higher",
                    icon =
                        Icons.Rounded
                            .TrendingUp,
                    color = higherColor,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            if (
                movementView
                    .changes
                    .isNotEmpty()
            ) {
                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                InteractiveAggregateMovementChart(
                    changes =
                        movementView.changes,
                    range = range,
                    generatedAt =
                        generatedAt,
                    lowerColor =
                        lowerColor,
                    higherColor =
                        higherColor
                )
            }
        }
    }
}

@Composable
private fun MovementMetric(
    value: Int,
    label: String,
    icon:
    androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.28f)
        )
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Column {
                Text(
                    text = value.toString(),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 17.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )

                Text(
                    text = label,
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AggregateMovementChart(
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
        buildMovementBuckets(
            changes = changes,
            range = range,
            generatedAt =
                generatedAt
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

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .semantics {
                    contentDescription =
                        "Price movement graph with " +
                                "${changes.size} changes"
                }
        ) {
            val baseline =
                size.height - 8.dp.toPx()

            drawLine(
                color =
                    Color.Gray.copy(
                        alpha = 0.25f
                    ),
                start =
                    Offset(0f, baseline),
                end =
                    Offset(
                        size.width,
                        baseline
                    ),
                strokeWidth = 1.dp.toPx()
            )

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

                val usableHeight =
                    baseline -
                            8.dp.toPx()

                val lowerHeight =
                    usableHeight *
                            bucket.lowerCount /
                            maximumCount

                val higherHeight =
                    usableHeight *
                            bucket.higherCount /
                            maximumCount

                if (bucket.lowerCount > 0) {
                    drawLine(
                        color = lowerColor,
                        start = Offset(
                            centerX -
                                    barWidth * 0.65f,
                            baseline
                        ),
                        end = Offset(
                            centerX -
                                    barWidth * 0.65f,
                            baseline -
                                    lowerHeight
                        ),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }

                if (bucket.higherCount > 0) {
                    drawLine(
                        color = higherColor,
                        start = Offset(
                            centerX +
                                    barWidth * 0.65f,
                            baseline
                        ),
                        end = Offset(
                            centerX +
                                    barWidth * 0.65f,
                            baseline -
                                    higherHeight
                        ),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text =
                    when (range) {
                        ShopMovementRange
                            .ONE_DAY ->
                            "24 hours ago"

                        ShopMovementRange
                            .SEVEN_DAYS ->
                            "7 days ago"

                        ShopMovementRange
                            .THIRTY_DAYS ->
                            "30 days ago"
                    },
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 9.sp
            )

            Text(
                text = "Now",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 9.sp
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            MovementLegend(
                text = "Lower",
                color = lowerColor
            )

            MovementLegend(
                text = "Higher",
                color = higherColor
            )
        }
    }
}

@Composable
private fun MovementProductCard(
    product: ShopProductMovementView,
    generatedAt: Long,
    notificationTarget:
        PriceMovementNotificationTarget?,
    reduceMotionEnabled: Boolean
) {
    val latestChange =
        notificationTarget
            ?.let { target ->
                product.changes
                    .firstOrNull { change ->
                        change.retailer ==
                            target.retailer &&
                            change.checkedAt ==
                            target.detectedAt
                    }
            }
            ?: product.latestChange
            ?: return

    val movementColor =
        if (
            latestChange.direction ==
            DetectedPriceDirection.LOWER
        ) {
            // The retailer became more competitive.
            MaterialTheme
                .colorScheme
                .error
        } else {
            // The retailer became less competitive.
            MaterialTheme
                .supremeColors
                .competitive
        }

    val notificationPulse = remember {
        Animatable(0f)
    }

    LaunchedEffect(
        notificationTarget?.requestId,
        reduceMotionEnabled
    ) {
        notificationPulse.snapTo(0f)

        if (notificationTarget != null) {
            if (reduceMotionEnabled) {
                notificationPulse.snapTo(0.55f)
                kotlinx.coroutines.delay(2400)
                notificationPulse.snapTo(0f)
            } else {
                repeat(4) {
                    notificationPulse.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 320
                        )
                    )

                    notificationPulse.animateTo(
                        targetValue = 0.12f,
                        animationSpec = tween(
                            durationMillis = 360
                        )
                    )
                }

                notificationPulse.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 220
                    )
                )
            }
        }
    }

    val panelColor =
        MaterialTheme
            .supremeColors
            .panel

    val normalBorderColor =
        MaterialTheme
            .supremeColors
            .border

    val pulseAmount =
        notificationPulse.value

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color =
            androidx.compose.ui.graphics.lerp(
                panelColor,
                movementColor,
                pulseAmount * 0.16f
            ),
        border = BorderStroke(
            width =
                if (pulseAmount > 0f) {
                    2.dp
                } else {
                    1.dp
                },
            color =
                androidx.compose.ui.graphics.lerp(
                    normalBorderColor,
                    movementColor,
                    pulseAmount
                )
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Text(
                text =
                    product.item.productName,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (
                            latestChange.direction ==
                            DetectedPriceDirection
                                .LOWER
                        ) {
                            Icons.Rounded
                                .TrendingDown
                        } else {
                            Icons.Rounded
                                .TrendingUp
                        },
                    contentDescription = null,
                    tint = movementColor,
                    modifier =
                        Modifier.size(18.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(6.dp)
                )

                Text(
                    text =
                        latestChange
                            .retailer
                            .displayName() +
                                " " +
                                if (
                                    latestChange.direction ==
                                    DetectedPriceDirection
                                        .LOWER
                                ) {
                                    "lowered"
                                } else {
                                    "increased"
                                },
                    color = movementColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                Text(
                    text =
                        relativeMovementTime(
                            checkedAt =
                                latestChange
                                    .checkedAt,
                            nowMillis =
                                generatedAt
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    "${formatIndianPrice(latestChange.oldPrice)} → " +
                            formatIndianPrice(
                                latestChange.newPrice
                            ) +
                            " • " +
                            formatMovementPercent(
                                latestChange.percentage
                            ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                product.amazonHistory
                    .lastOrNull()
                    ?.let { latest ->
                        MovementLegend(
                            text =
                                "Amazon " +
                                        formatIndianPrice(
                                            latest.price
                                        ),
                            color =
                                AmazonChartColor
                        )
                    }

                product.flipkartHistory
                    .lastOrNull()
                    ?.let { latest ->
                        MovementLegend(
                            text =
                                "Flipkart " +
                                        formatIndianPrice(
                                            latest.price
                                        ),
                            color =
                                FlipkartChartColor
                        )
                    }
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            InteractiveProductMovementLineChart(
                amazonHistory =
                    product.amazonHistory,
                flipkartHistory =
                    product.flipkartHistory,
                notificationTarget =
                    notificationTarget,
                notificationHighlightColor =
                    movementColor,
                notificationPulseProgress =
                    pulseAmount
            )
        }
    }
}

@Composable
private fun ProductMovementLineChart(
    amazonHistory: List<ShopPricePoint>,
    flipkartHistory: List<ShopPricePoint>
) {
    val series = remember(
        amazonHistory,
        flipkartHistory
    ) {
        buildList {
            if (amazonHistory.isNotEmpty()) {
                add(
                    MovementChartSeries(
                        color =
                            AmazonChartColor,
                        points =
                            amazonHistory
                    )
                )
            }

            if (flipkartHistory.isNotEmpty()) {
                add(
                    MovementChartSeries(
                        color =
                            FlipkartChartColor,
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

    val minimumPrice =
        allPoints.minOf { point ->
            point.price
        }

    val maximumPrice =
        allPoints.maxOf { point ->
            point.price
        }

    val minimumTime =
        allPoints.minOf { point ->
            point.checkedAt
        }

    val maximumTime =
        allPoints.maxOf { point ->
            point.checkedAt
        }

    val gridColor =
        MaterialTheme
            .colorScheme
            .onSurfaceVariant
            .copy(alpha = 0.15f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(94.dp)
            .semantics {
                contentDescription =
                    "Amazon and Flipkart price history graph"
            }
    ) {
        val horizontalPadding =
            7.dp.toPx()

        val verticalPadding =
            9.dp.toPx()

        val chartWidth =
            (
                    size.width -
                            horizontalPadding * 2f
                    ).coerceAtLeast(1f)

        val chartHeight =
            (
                    size.height -
                            verticalPadding * 2f
                    ).coerceAtLeast(1f)

        repeat(3) { index ->
            val y =
                verticalPadding +
                        chartHeight *
                        index / 2f

            drawLine(
                color = gridColor,
                start =
                    Offset(
                        horizontalPadding,
                        y
                    ),
                end =
                    Offset(
                        size.width -
                                horizontalPadding,
                        y
                    ),
                strokeWidth = 1.dp.toPx()
            )
        }

        val priceRange =
            (
                    maximumPrice -
                            minimumPrice
                    ).takeIf {
                    it > 0.01
                }
                ?: 1.0

        val timeRange =
            (
                    maximumTime -
                            minimumTime
                    ).takeIf {
                    it > 0L
                }
                ?: 1L

        series.forEach { chartSeries ->
            val path = Path()

            chartSeries.points
                .forEachIndexed {
                        index,
                        point ->

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
                                ) /
                                priceRange

                    val y =
                        verticalPadding +
                                chartHeight *
                                (
                                        1f -
                                                normalizedPrice
                                                    .toFloat()
                                        )

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
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
                                ) /
                                priceRange

                    val y =
                        verticalPadding +
                                chartHeight *
                                (
                                        1f -
                                                normalizedPrice
                                                    .toFloat()
                                        )

                    drawCircle(
                        color =
                            chartSeries.color,
                        radius =
                            3.dp.toPx(),
                        center =
                            Offset(x, y)
                    )
                }
        }
    }
}

@Composable
private fun MovementLegend(
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
private fun MovementEmptyState(
    range: ShopMovementRange,
    retailer:
    ShopMovementRetailerFilter
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color =
            MaterialTheme
                .supremeColors
                .panel,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme
                    .supremeColors
                    .border
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 30.dp
            ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.ShowChart,
                contentDescription = null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                modifier = Modifier.size(34.dp)
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "No price changes found",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "No ${retailer.buttonLabel} changes were found in the last ${range.days} " +
                            if (range.days == 1) {
                                "day."
                            } else {
                                "days."
                            },
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    "Daily observations are still being saved for future graphs.",
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
private fun MovementLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color =
                    MaterialTheme
                        .colorScheme
                        .primary,
                modifier = Modifier.size(34.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text =
                    "Reading saved price history",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MovementErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color =
                    MaterialTheme
                        .colorScheme
                        .error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Surface(
                onClick = onRetry,
                shape =
                    RoundedCornerShape(12.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {
                Text(
                    text = "Try again",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight =
                        FontWeight.Bold,
                    modifier =
                        Modifier.padding(
                            horizontal = 18.dp,
                            vertical = 10.dp
                        )
                )
            }
        }
    }
}

private data class MovementChartSeries(
    val color: Color,
    val points: List<ShopPricePoint>
)

private data class MovementBucket(
    val lowerCount: Int,
    val higherCount: Int
)

private fun buildMovementBuckets(
    changes: List<ShopPriceChange>,
    range: ShopMovementRange,
    generatedAt: Long
): List<MovementBucket> {
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
        MovementBucket(
            lowerCount =
                lowerCounts[index],
            higherCount =
                higherCounts[index]
        )
    }
}

private fun PriceRetailer.displayName():
        String =
    when (this) {
        PriceRetailer.AMAZON ->
            "Amazon"

        PriceRetailer.FLIPKART ->
            "Flipkart"
    }

private fun relativeMovementTime(
    checkedAt: Long,
    nowMillis: Long
): String {
    val difference =
        (
                nowMillis -
                        checkedAt
                ).coerceAtLeast(0L)

    val hours =
        difference /
                (60L * 60L * 1000L)

    val days =
        difference /
                PRICE_MOVEMENT_DAY_MILLIS

    return when {
        difference <
                60L * 60L * 1000L ->
            "Just now"

        hours < 24L ->
            "${hours}h ago"

        days == 1L ->
            "1d ago"

        else ->
            "${days}d ago"
    }
}

private fun formatMovementPercent(
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