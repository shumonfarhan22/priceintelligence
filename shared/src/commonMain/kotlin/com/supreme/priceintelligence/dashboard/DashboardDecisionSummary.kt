package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.settings.BreakdownLayout
import com.supreme.priceintelligence.settings.BreakdownValueMode
import com.supreme.priceintelligence.settings.InsightCustomization
import com.supreme.priceintelligence.settings.PriorityRowStyle
import com.supreme.priceintelligence.settings.PrioritySortMode
import com.supreme.priceintelligence.settings.SectionStartState
import com.supreme.priceintelligence.ui.theme.supremeColors

data class PriorityProduct(
    val productName: String,
    val gap: Double,
    val onlinePrice: Double,
    val purchaseCost: Double?
)

data class DashboardDecisionSummary(
    val comparedCount: Int,
    val onlineCheaperCount: Int,
    val shopCompetitiveCount: Int,
    val noOnlinePriceCount: Int,
    val invalidShopPriceCount: Int,
    val livePriceProductCount: Int,
    val priorityProducts: List<PriorityProduct> = emptyList()
)

// Builds the summary from every matching product (allItems) — the whole
// shop, or the whole search result — never just whatever page is currently
// on screen. livePriceCards supplies a fresher price for whichever of those
// products also happen to be visible right now, so a check you just ran
// shows up immediately instead of waiting for the next full reload.
fun List<InventoryItem>.buildDecisionSummary(
    livePriceCards: List<ProductCardUiState>
): DashboardDecisionSummary {
    val liveById = livePriceCards.associateBy { card -> card.item.id }

    var onlineCheaperCount = 0
    var shopCompetitiveCount = 0
    var noOnlinePriceCount = 0
    var invalidShopPriceCount = 0
    var livePriceProductCount = 0
    val priorityProducts = mutableListOf<PriorityProduct>()

    forEach { item ->
        val liveCard = liveById[item.id]
        val amazonPrice = liveCard?.amazonResult?.price ?: item.amazonLastPrice
        val flipkartPrice = liveCard?.flipkartResult?.price ?: item.flipkartLastPrice

        val comparison = compareWithOnlinePrices(
            shopPrice = item.shopPrice,
            amazonPrice = amazonPrice,
            flipkartPrice = flipkartPrice
        )

        if (
            liveCard?.amazonResult?.price != null ||
            liveCard?.flipkartResult?.price != null
        ) {
            livePriceProductCount += 1
        }

        when (comparison.shopPosition) {
            ShopPricePosition.HIGHER -> {
                onlineCheaperCount += 1
                val gap = comparison.shopDifference
                val onlinePrice = comparison.onlineLowestPrice

                if (gap != null && onlinePrice != null) {
                    priorityProducts += PriorityProduct(
                        productName = item.productName,
                        gap = gap,
                        onlinePrice = onlinePrice,
                        purchaseCost = item.purchaseCost
                    )
                }
            }

            ShopPricePosition.LOWER,
            ShopPricePosition.MATCHED -> {
                shopCompetitiveCount += 1
            }

            ShopPricePosition.NO_ONLINE_PRICE -> {
                noOnlinePriceCount += 1
            }

            ShopPricePosition.INVALID_SHOP_PRICE -> {
                invalidShopPriceCount += 1
            }
        }
    }

    return DashboardDecisionSummary(
        comparedCount = size,
        onlineCheaperCount = onlineCheaperCount,
        shopCompetitiveCount = shopCompetitiveCount,
        noOnlinePriceCount = noOnlinePriceCount,
        invalidShopPriceCount = invalidShopPriceCount,
        livePriceProductCount = livePriceProductCount,
        priorityProducts = priorityProducts.sortedByDescending { product -> product.gap }
    )
}

@Composable
fun DashboardDecisionSummaryCard(
    summary: DashboardDecisionSummary,
    freshnessSummary: PriceFreshnessSummary,
    modifier: Modifier = Modifier,
    collapseSignal: Boolean = false,
    refreshTick: Int = 0,
    activeFilter: PricePositionFilter? = null,
    reduceMotionEnabled: Boolean = false,
    insightCustomization: InsightCustomization =
        InsightCustomization(),
    onPriceMovementClick: () -> Unit = {},
    onFilterToggle: (PricePositionFilter) -> Unit = {}
) {
    var isCardExpanded by rememberSaveable(
        insightCustomization.shopOverviewStartState
    ) {
        mutableStateOf(
            insightCustomization.shopOverviewStartState ==
                SectionStartState.EXPANDED
        )
    }
    var isBreakdownExpanded by rememberSaveable(
        insightCustomization.breakdownStartState
    ) {
        mutableStateOf(
            insightCustomization.breakdownStartState ==
                SectionStartState.EXPANDED
        )
    }
    var isPriorityListExpanded by rememberSaveable(
        insightCustomization.prioritiesStartState
    ) {
        mutableStateOf(
            insightCustomization.prioritiesStartState ==
                SectionStartState.EXPANDED
        )
    }

    val displayedPriorityProducts = remember(
        summary.priorityProducts,
        insightCustomization.prioritySortMode,
        insightCustomization.priorityProductLimit
    ) {
        val sorted = when (
            insightCustomization.prioritySortMode
        ) {
            PrioritySortMode.RUPEE_GAP ->
                summary.priorityProducts
                    .sortedByDescending { it.gap }

            PrioritySortMode.PERCENTAGE_GAP ->
                summary.priorityProducts
                    .sortedByDescending {
                        it.priorityGapPercent()
                    }
        }

        sorted.take(
            insightCustomization
                .priorityProductLimit
                .count
        )
    }

    // Lets the caller fold this card shut once the user has scrolled well
    // past it, so it doesn't sit expanded — and taking up space — if they
    // scroll back near the top later.
    LaunchedEffect(collapseSignal) {
        if (collapseSignal) {
            isCardExpanded = false
            isBreakdownExpanded = false
            isPriorityListExpanded = false
        }
    }

    // Pull-to-refresh should also hand back the compact view. refreshTick
    // starts at 0 and only ever increases, so the guard below skips the
    // very first composition and only fires on an actual refresh.
    LaunchedEffect(refreshTick) {
        if (refreshTick > 0) {
            isCardExpanded = false
            isBreakdownExpanded = false
            isPriorityListExpanded = false
        }
    }

    val competitiveColor = MaterialTheme.supremeColors.competitive
    val reviewColor = MaterialTheme.colorScheme.error
    val freshnessColor = MaterialTheme.supremeColors.warning

    val cardChevronRotation by animateFloatAsState(
        targetValue = if (isCardExpanded) 180f else 0f,
        animationSpec = if (reduceMotionEnabled) {
            snap()
        } else {
            tween(durationMillis = 200)
        },
        label = "shopOverviewChevron"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .animateContentSize(
                animationSpec = if (reduceMotionEnabled) {
                    snap()
                } else {
                    tween(durationMillis = 220)
                }
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.supremeColors.border
        ),
        shadowElevation =
            if (MaterialTheme.supremeColors.isDark) {
                0.dp
            } else {
                2.dp
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        isCardExpanded = !isCardExpanded
                    },
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "SHOP OVERVIEW",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "${summary.comparedCount} products compared",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.12f
                        )
                    ) {
                        Text(
                            text =
                                "${summary.livePriceProductCount}/" +
                                        "${summary.comparedCount} live",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = if (isCardExpanded) {
                            "Collapse"
                        } else {
                            "Expand"
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = cardChevronRotation
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Always visible — green (competitive) vs red (review), side by
            // side, proportional to how many products fall in each. Tapping
            // it (like tapping the header above) reveals the breakdown and
            // priorities below.
            DecisionMeterBar(
                competitiveCount = summary.shopCompetitiveCount,
                reviewCount = summary.onlineCheaperCount,
                competitiveColor = competitiveColor,
                reviewColor = reviewColor,
                activeFilter = activeFilter,
                onTap = { isCardExpanded = !isCardExpanded }
            )

            if (isCardExpanded) {
                Spacer(modifier = Modifier.height(10.dp))

                ExpandToggleRow(
                    label = "Breakdown",
                    expanded = isBreakdownExpanded,
                    reduceMotionEnabled = reduceMotionEnabled,
                    onClick = {
                        isBreakdownExpanded =
                            !isBreakdownExpanded
                    }
                )

                if (isBreakdownExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {
                        DecisionMetric(
                            value =
                                summary.shopCompetitiveCount,
                            label = "Competitive",
                            secondaryText =
                                metricSecondaryText(
                                    summary.shopCompetitiveCount,
                                    summary.comparedCount,
                                    insightCustomization.breakdownValueMode
                                ),
                            icon = Icons.Rounded.EmojiEvents,
                            color = competitiveColor,
                            selected =
                                activeFilter ==
                                    PricePositionFilter.COMPETITIVE,
                            onClick = {
                                onFilterToggle(
                                    PricePositionFilter.COMPETITIVE
                                )
                            },
                            modifier = Modifier.weight(1f),
                            compact =
                                insightCustomization.breakdownLayout ==
                                    BreakdownLayout.COMPACT_STRIP
                        )

                        DecisionMetric(
                            value =
                                freshnessSummary.needsCheckCount,
                            label = "Needs check",
                            secondaryText =
                                metricSecondaryText(
                                    freshnessSummary.needsCheckCount,
                                    summary.comparedCount,
                                    insightCustomization.breakdownValueMode
                                ),
                            icon = Icons.Rounded.Search,
                            color = freshnessColor,
                            selected =
                                activeFilter ==
                                    PricePositionFilter.NEEDS_CHECK,
                            onClick =
                                if (
                                    freshnessSummary.needsCheckCount > 0 ||
                                    activeFilter ==
                                        PricePositionFilter.NEEDS_CHECK
                                ) {
                                    {
                                        onFilterToggle(
                                            PricePositionFilter.NEEDS_CHECK
                                        )
                                    }
                                } else {
                                    null
                                },
                            modifier = Modifier.weight(1f),
                            compact =
                                insightCustomization.breakdownLayout ==
                                    BreakdownLayout.COMPACT_STRIP
                        )

                        DecisionMetric(
                            value =
                                summary.onlineCheaperCount,
                            label = "Review",
                            secondaryText =
                                metricSecondaryText(
                                    summary.onlineCheaperCount,
                                    summary.comparedCount,
                                    insightCustomization.breakdownValueMode
                                ),
                            icon = Icons.Rounded.PriorityHigh,
                            color = reviewColor,
                            selected =
                                activeFilter ==
                                    PricePositionFilter.REVIEW,
                            onClick = {
                                onFilterToggle(
                                    PricePositionFilter.REVIEW
                                )
                            },
                            modifier = Modifier.weight(1f),
                            compact =
                                insightCustomization.breakdownLayout ==
                                    BreakdownLayout.COMPACT_STRIP
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    PriceMovementAction(
                        onClick =
                            onPriceMovementClick
                    )
                }

                if (summary.priorityProducts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    ExpandToggleRow(
                        label =
                            "Top priorities " +
                                "(${summary.priorityProducts.size})",
                        expanded = isPriorityListExpanded,
                        reduceMotionEnabled =
                            reduceMotionEnabled,
                        onClick = {
                            isPriorityListExpanded =
                                !isPriorityListExpanded
                        }
                    )

                    if (isPriorityListExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayedPriorityProducts.forEachIndexed { index, product ->
                                PriorityProductRow(
                                    rank = index + 1,
                                    product = product,
                                    reviewColor = reviewColor,
                                    competitiveColor = competitiveColor,
                                    rowStyle =
                                        insightCustomization
                                            .priorityRowStyle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceMovementAction(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color =
            MaterialTheme
                .colorScheme
                .primary
                .copy(alpha = 0.10f),
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme
                    .colorScheme
                    .primary
                    .copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 11.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(alpha = 0.18f)
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
                            .primary,
                    modifier =
                        Modifier.size(19.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Price Movement",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 12.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "View retailer changes and 30-day graphs",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            Icon(
                imageVector =
                    Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun ExpandToggleRow(
    label: String,
    expanded: Boolean,
    reduceMotionEnabled: Boolean,
    onClick: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reduceMotionEnabled) {
            snap()
        } else {
            tween(durationMillis = 180)
        },
        label = "shopOverviewSectionChevron"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) {
                "Collapse"
            } else {
                "Expand"
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    rotationZ = chevronRotation
                }
        )
    }
}

@Composable
private fun DecisionMeterBar(
    competitiveCount: Int,
    reviewCount: Int,
    competitiveColor: Color,
    reviewColor: Color,
    activeFilter: PricePositionFilter?,
    onTap: () -> Unit
) {
    val total = competitiveCount + reviewCount
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    val resolvedCompetitiveColor = if (
        activeFilter == null || activeFilter == PricePositionFilter.COMPETITIVE
    ) {
        competitiveColor
    } else {
        mutedColor
    }

    val resolvedReviewColor = if (
        activeFilter == null || activeFilter == PricePositionFilter.REVIEW
    ) {
        reviewColor
    } else {
        mutedColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.supremeColors.panelMuted)
            .clickable(onClick = onTap),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (total == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.supremeColors.panelMuted)
            )
        } else {
            if (competitiveCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(competitiveCount.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(resolvedCompetitiveColor)
                )
            }

            if (reviewCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(reviewCount.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(resolvedReviewColor)
                )
            }
        }
    }
}

@Composable
private fun DecisionMetric(
    value: Int,
    label: String,
    secondaryText: String? = null,
    icon: ImageVector,
    color: Color,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        modifier = modifier,
        onClick = {
            onClick?.invoke()
        },
        enabled = onClick != null,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            color.copy(alpha = 0.15f)
        } else {
            MaterialTheme.supremeColors.panelMuted
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                color.copy(alpha = 0.72f)
            } else {
                MaterialTheme.supremeColors.border
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    horizontal =
                        if (compact) 7.dp else 10.dp,
                    vertical =
                        if (compact) 7.dp else 10.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 24.dp else 30.dp)
                    .clip(CircleShape)
                    .background(
                        color.copy(
                            alpha = if (selected) {
                                0.28f
                            } else {
                                0.16f
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(
                        if (compact) 14.dp else 17.dp
                    )
                )
            }

            Spacer(
                modifier = Modifier.height(
                    if (compact) 5.dp else 8.dp
                )
            )

            Text(
                text = if (secondaryText == null) {
                    value.toString()
                } else {
                    "$value • $secondaryText"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (compact) 14.sp else 19.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = label,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PriorityProductRow(
    rank: Int,
    product: PriorityProduct,
    reviewColor: Color,
    competitiveColor: Color,
    rowStyle: PriorityRowStyle
) {
    val supremePrice = product.onlinePrice + product.gap
    val purchaseCost = product.purchaseCost
        ?.takeIf { cost -> cost.isFinite() && cost > 0.0 }

    val marginRisk =
        purchaseCost != null &&
                product.onlinePrice <= purchaseCost + 0.01

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            width = 1.dp,
            color = reviewColor.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (marginRisk) "MARGIN RISK" else "#$rank PRIORITY",
                        color = if (marginRisk) reviewColor else competitiveColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )

                    Text(
                        text = product.productName,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${formatIndianPrice(product.gap)} gap",
                    color = reviewColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (rowStyle == PriorityRowStyle.DETAILED) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    DecisionPricePoint(
                        label = "Cost",
                        value = purchaseCost?.let { cost -> formatIndianPrice(cost) } ?: "Not set",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    DecisionPricePoint(
                        label = "Online",
                        value = formatIndianPrice(product.onlinePrice),
                        color = reviewColor,
                        modifier = Modifier.weight(1f)
                    )

                    DecisionPricePoint(
                        label = "Supreme",
                        value = formatIndianPrice(supremePrice),
                        color = competitiveColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionPricePoint(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.supremeColors.panelMuted)
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            )
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            maxLines = 1
        )

        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
    }
}

private fun metricSecondaryText(
    value: Int,
    total: Int,
    mode: BreakdownValueMode
): String? {
    if (
        mode != BreakdownValueMode.COUNTS_AND_PERCENTAGES ||
        total <= 0
    ) {
        return null
    }

    val percentage =
        (value.toDouble() * 100.0 / total.toDouble() + 0.5)
            .toInt()

    return "$percentage%"
}

private fun PriorityProduct.priorityGapPercent(): Double {
    val supremePrice = onlinePrice + gap
    return if (supremePrice > 0.0) {
        gap / supremePrice * 100.0
    } else {
        0.0
    }
}