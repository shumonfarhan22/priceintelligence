package com.supreme.priceintelligence.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.data.InventoryItem

data class DashboardDecisionSummary(
    val comparedCount: Int,
    val onlineCheaperCount: Int,
    val shopCompetitiveCount: Int,
    val noOnlinePriceCount: Int,
    val invalidShopPriceCount: Int,
    val livePriceProductCount: Int,
    val biggestSavingProductName: String? = null,
    val biggestSavingAmount: Double? = null,
    val priorityOnlinePrice: Double? = null,
    val priorityPurchaseCost: Double? = null
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
    var biggestSavingProductName: String? = null
    var biggestSavingAmount: Double? = null
    var priorityOnlinePrice: Double? = null
    var priorityPurchaseCost: Double? = null

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

                if (gap != null && gap > (biggestSavingAmount ?: 0.0)) {
                    biggestSavingAmount = gap
                    biggestSavingProductName = item.productName
                    priorityOnlinePrice = comparison.onlineLowestPrice
                    priorityPurchaseCost = item.purchaseCost
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
        biggestSavingProductName = biggestSavingProductName,
        biggestSavingAmount = biggestSavingAmount,
        priorityOnlinePrice = priorityOnlinePrice,
        priorityPurchaseCost = priorityPurchaseCost
    )
}

private enum class DecisionFocus {
    COMPETITIVE,
    REVIEW,
    NO_ONLINE_PRICE,
    FIX_PRICE
}

@Composable
fun DashboardDecisionSummaryCard(
    summary: DashboardDecisionSummary,
    modifier: Modifier = Modifier
) {
    var selectedFocus by remember {
        mutableStateOf<DecisionFocus?>(null)
    }

    val competitiveColor = MaterialTheme.colorScheme.primary
    val reviewColor = MaterialTheme.colorScheme.error
    val noOnlinePriceColor = MaterialTheme.colorScheme.secondary
    val fixPriceColor = MaterialTheme.colorScheme.onSurfaceVariant

    fun toggle(focus: DecisionFocus) {
        selectedFocus = if (selectedFocus == focus) null else focus
    }

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

            DecisionDistributionBar(
                summary = summary,
                selectedFocus = selectedFocus,
                competitiveColor = competitiveColor,
                reviewColor = reviewColor,
                noOnlinePriceColor = noOnlinePriceColor,
                fixPriceColor = fixPriceColor,
                onFocusSelected = ::toggle
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecisionMetric(
                    value = summary.shopCompetitiveCount,
                    label = "Competitive",
                    color = competitiveColor,
                    selected = selectedFocus == DecisionFocus.COMPETITIVE,
                    onClick = { toggle(DecisionFocus.COMPETITIVE) },
                    modifier = Modifier.weight(1f)
                )

                DecisionMetric(
                    value = summary.onlineCheaperCount,
                    label = "Review",
                    color = reviewColor,
                    selected = selectedFocus == DecisionFocus.REVIEW,
                    onClick = { toggle(DecisionFocus.REVIEW) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecisionMetric(
                    value = summary.noOnlinePriceCount,
                    label = "No online price",
                    color = noOnlinePriceColor,
                    selected = selectedFocus == DecisionFocus.NO_ONLINE_PRICE,
                    onClick = { toggle(DecisionFocus.NO_ONLINE_PRICE) },
                    modifier = Modifier.weight(1f)
                )

                DecisionMetric(
                    value = summary.invalidShopPriceCount,
                    label = "Fix shop price",
                    color = fixPriceColor,
                    selected = selectedFocus == DecisionFocus.FIX_PRICE,
                    onClick = { toggle(DecisionFocus.FIX_PRICE) },
                    modifier = Modifier.weight(1f)
                )
            }

            PriorityPriceComparison(
                summary = summary,
                reviewColor = reviewColor
            )
        }
    }
}

@Composable
private fun DecisionDistributionBar(
    summary: DashboardDecisionSummary,
    selectedFocus: DecisionFocus?,
    competitiveColor: Color,
    reviewColor: Color,
    noOnlinePriceColor: Color,
    fixPriceColor: Color,
    onFocusSelected: (DecisionFocus) -> Unit
) {
    val segments = listOf(
        Triple(DecisionFocus.COMPETITIVE, summary.shopCompetitiveCount, competitiveColor),
        Triple(DecisionFocus.REVIEW, summary.onlineCheaperCount, reviewColor),
        Triple(DecisionFocus.NO_ONLINE_PRICE, summary.noOnlinePriceCount, noOnlinePriceColor),
        Triple(DecisionFocus.FIX_PRICE, summary.invalidShopPriceCount, fixPriceColor)
    ).filter { (_, count, _) -> count > 0 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (segments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.06f))
            )
        } else {
            segments.forEach { (focus, count, color) ->
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            color.copy(
                                alpha = if (
                                    selectedFocus == null || selectedFocus == focus
                                ) {
                                    1f
                                } else {
                                    0.30f
                                }
                            )
                        )
                        .clickable { onFocusSelected(focus) }
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
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                color.copy(
                    alpha = if (selected) 0.20f else 0.10f
                )
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        color = color.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
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
private fun PriorityPriceComparison(
    summary: DashboardDecisionSummary,
    reviewColor: Color
) {
    val productName = summary.biggestSavingProductName
    val saving = summary.biggestSavingAmount
    val onlinePrice = summary.priorityOnlinePrice

    if (
        productName == null ||
        saving == null ||
        onlinePrice == null
    ) {
        return
    }

    val shopPrice = onlinePrice + saving
    val purchaseCost = summary.priorityPurchaseCost
        ?.takeIf { cost ->
            cost.isFinite() && cost > 0.0
        }

    val marginRisk =
        purchaseCost != null &&
                onlinePrice <= purchaseCost + 0.01

    Spacer(modifier = Modifier.height(12.dp))

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
                        text = if (marginRisk) {
                            "MARGIN RISK"
                        } else {
                            "TOP PRIORITY"
                        },
                        color = if (marginRisk) {
                            reviewColor
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )

                    Text(
                        text = productName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${formatIndianPrice(saving)} gap",
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
                    value = purchaseCost?.let {
                        formatIndianPrice(it)
                    } ?: "Not set",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                DecisionPricePoint(
                    label = "Online",
                    value = formatIndianPrice(onlinePrice),
                    color = reviewColor,
                    modifier = Modifier.weight(1f)
                )

                DecisionPricePoint(
                    label = "Your price",
                    value = formatIndianPrice(shopPrice),
                    color = MaterialTheme.colorScheme.primary,
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