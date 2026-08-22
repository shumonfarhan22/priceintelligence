package com.supreme.priceintelligence.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.data.InventoryItem

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
    modifier: Modifier = Modifier,
    collapseSignal: Boolean = false
) {
    var isBreakdownExpanded by remember { mutableStateOf(false) }
    var isPriorityListExpanded by remember { mutableStateOf(false) }

    // Lets the caller fold this card shut once the user has scrolled well
    // past it, so it doesn't sit expanded — and taking up space — if they
    // scroll back near the top later.
    LaunchedEffect(collapseSignal) {
        if (collapseSignal) {
            isBreakdownExpanded = false
            isPriorityListExpanded = false
        }
    }

    val competitiveColor = MaterialTheme.colorScheme.primary
    val reviewColor = MaterialTheme.colorScheme.error

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Always-visible meter: green (competitive) vs red (review),
            // side by side, proportional to how many products fall in
            // each. This is the "at a glance" view — no tap needed.
            DecisionMeterBar(
                competitiveCount = summary.shopCompetitiveCount,
                reviewCount = summary.onlineCheaperCount,
                competitiveColor = competitiveColor,
                reviewColor = reviewColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            ExpandToggleRow(
                label = "Breakdown",
                expanded = isBreakdownExpanded,
                onClick = { isBreakdownExpanded = !isBreakdownExpanded }
            )

            if (isBreakdownExpanded) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DecisionMetric(
                        value = summary.shopCompetitiveCount,
                        label = "Competitive",
                        color = competitiveColor,
                        modifier = Modifier.weight(1f)
                    )

                    DecisionMetric(
                        value = summary.onlineCheaperCount,
                        label = "Review",
                        color = reviewColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (summary.priorityProducts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                ExpandToggleRow(
                    label = "Top priorities (${summary.priorityProducts.size})",
                    expanded = isPriorityListExpanded,
                    onClick = { isPriorityListExpanded = !isPriorityListExpanded }
                )

                if (isPriorityListExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summary.priorityProducts.forEachIndexed { index, product ->
                            PriorityProductRow(
                                rank = index + 1,
                                product = product,
                                reviewColor = reviewColor,
                                competitiveColor = competitiveColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandToggleRow(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
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
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DecisionMeterBar(
    competitiveCount: Int,
    reviewCount: Int,
    competitiveColor: Color,
    reviewColor: Color
) {
    val total = competitiveCount + reviewCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (total == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.06f))
            )
        } else {
            if (competitiveCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(competitiveCount.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(competitiveColor)
                )
            }

            if (reviewCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(reviewCount.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(reviewColor)
                )
            }
        }
    }
}

@Composable
private fun DecisionMetric(
    value: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(
                horizontal = 9.dp,
                vertical = 9.dp
            )
    ) {
        Text(
            text = value.toString(),
            color = color,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PriorityProductRow(
    rank: Int,
    product: PriorityProduct,
    reviewColor: Color,
    competitiveColor: Color
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
        color = Color.White.copy(alpha = 0.035f),
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
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
            .background(Color.White.copy(alpha = 0.04f))
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
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}