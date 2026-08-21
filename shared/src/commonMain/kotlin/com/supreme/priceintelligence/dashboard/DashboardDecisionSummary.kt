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

data class DashboardDecisionSummary(
    val comparedCount: Int,
    val onlineCheaperCount: Int,
    val shopCompetitiveCount: Int,
    val unavailableCount: Int,
    val livePriceProductCount: Int,
    val biggestSavingProductName: String? = null,
    val biggestSavingAmount: Double? = null,
    val priorityOnlinePrice: Double? = null,
    val priorityPurchaseCost: Double? = null
)

fun List<ProductCardUiState>.buildDecisionSummary():
        DashboardDecisionSummary {
    var onlineCheaperCount = 0
    var shopCompetitiveCount = 0
    var unavailableCount = 0
    var livePriceProductCount = 0
    var biggestSavingProductName: String? = null
    var biggestSavingAmount: Double? = null
    var priorityOnlinePrice: Double? = null
    var priorityPurchaseCost: Double? = null

    forEach { card ->
        val amazonPrice =
            card.amazonResult?.price ?: card.item.amazonLastPrice
        val flipkartPrice =
            card.flipkartResult?.price ?: card.item.flipkartLastPrice

        val comparison = compareWithOnlinePrices(
            shopPrice = card.item.shopPrice,
            amazonPrice = amazonPrice,
            flipkartPrice = flipkartPrice
        )

        if (
            card.amazonResult?.price != null ||
            card.flipkartResult?.price != null
        ) {
            livePriceProductCount += 1
        }

        when (comparison.shopPosition) {
            ShopPricePosition.HIGHER -> {
                onlineCheaperCount += 1
                val gap = comparison.shopDifference

                if (
                    gap != null &&
                    gap > (biggestSavingAmount ?: 0.0)
                ) {
                    biggestSavingAmount = gap
                    biggestSavingProductName =
                        card.item.productName
                    priorityOnlinePrice =
                        comparison.onlineLowestPrice
                    priorityPurchaseCost =
                        card.item.purchaseCost
                }
            }

            ShopPricePosition.LOWER,
            ShopPricePosition.MATCHED -> {
                shopCompetitiveCount += 1
            }

            ShopPricePosition.NO_ONLINE_PRICE,
            ShopPricePosition.INVALID_SHOP_PRICE -> {
                unavailableCount += 1
            }
        }
    }

    return DashboardDecisionSummary(
        comparedCount = size,
        onlineCheaperCount = onlineCheaperCount,
        shopCompetitiveCount = shopCompetitiveCount,
        unavailableCount = unavailableCount,
        livePriceProductCount = livePriceProductCount,
        biggestSavingProductName = biggestSavingProductName,
        biggestSavingAmount = biggestSavingAmount,
        priorityOnlinePrice = priorityOnlinePrice,
        priorityPurchaseCost = priorityPurchaseCost
    )
}
private enum class DecisionFocus {
    COMPETITIVE,
    REVIEW_PRICE,
    NEED_CHECK
}

private val DecisionCheckColor = Color(0xFFF59E0B)

@Composable
fun DashboardDecisionSummaryCard(
    summary: DashboardDecisionSummary,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    var selectedFocus by remember {
        mutableStateOf<DecisionFocus?>(null)
    }

    val competitiveColor = MaterialTheme.colorScheme.primary
    val reviewColor = MaterialTheme.colorScheme.error

    val selectedColor = when (selectedFocus) {
        DecisionFocus.COMPETITIVE -> competitiveColor
        DecisionFocus.REVIEW_PRICE -> reviewColor
        DecisionFocus.NEED_CHECK -> DecisionCheckColor
        null -> when {
            summary.onlineCheaperCount > 0 -> reviewColor
            summary.shopCompetitiveCount > 0 -> competitiveColor
            else -> DecisionCheckColor
        }
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
                        text = "PRICE POSITION",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Page $currentPage snapshot",
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
                onFocusSelected = { focus ->
                    selectedFocus =
                        if (selectedFocus == focus) null else focus
                }
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
                    selected =
                        selectedFocus == DecisionFocus.COMPETITIVE,
                    onClick = {
                        selectedFocus =
                            if (
                                selectedFocus ==
                                DecisionFocus.COMPETITIVE
                            ) {
                                null
                            } else {
                                DecisionFocus.COMPETITIVE
                            }
                    },
                    modifier = Modifier.weight(1f)
                )

                DecisionMetric(
                    value = summary.onlineCheaperCount,
                    label = "Review",
                    color = reviewColor,
                    selected =
                        selectedFocus == DecisionFocus.REVIEW_PRICE,
                    onClick = {
                        selectedFocus =
                            if (
                                selectedFocus ==
                                DecisionFocus.REVIEW_PRICE
                            ) {
                                null
                            } else {
                                DecisionFocus.REVIEW_PRICE
                            }
                    },
                    modifier = Modifier.weight(1f)
                )

                DecisionMetric(
                    value = summary.unavailableCount,
                    label = "Check",
                    color = DecisionCheckColor,
                    selected =
                        selectedFocus == DecisionFocus.NEED_CHECK,
                    onClick = {
                        selectedFocus =
                            if (
                                selectedFocus ==
                                DecisionFocus.NEED_CHECK
                            ) {
                                null
                            } else {
                                DecisionFocus.NEED_CHECK
                            }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(selectedColor.copy(alpha = 0.10f))
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
            ) {
                Text(
                    text = summary.focusText(selectedFocus),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
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
    onFocusSelected: (DecisionFocus) -> Unit
) {
    val hasResults =
        summary.shopCompetitiveCount > 0 ||
                summary.onlineCheaperCount > 0 ||
                summary.unavailableCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        if (!hasResults) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.06f))
            )
        } else {
            if (summary.shopCompetitiveCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(
                            summary.shopCompetitiveCount.toFloat()
                        )
                        .fillMaxHeight()
                        .background(
                            competitiveColor.copy(
                                alpha = if (
                                    selectedFocus == null ||
                                    selectedFocus ==
                                    DecisionFocus.COMPETITIVE
                                ) {
                                    1f
                                } else {
                                    0.35f
                                }
                            )
                        )
                        .clickable {
                            onFocusSelected(
                                DecisionFocus.COMPETITIVE
                            )
                        }
                )
            }

            if (summary.onlineCheaperCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(
                            summary.onlineCheaperCount.toFloat()
                        )
                        .fillMaxHeight()
                        .background(
                            reviewColor.copy(
                                alpha = if (
                                    selectedFocus == null ||
                                    selectedFocus ==
                                    DecisionFocus.REVIEW_PRICE
                                ) {
                                    1f
                                } else {
                                    0.35f
                                }
                            )
                        )
                        .clickable {
                            onFocusSelected(
                                DecisionFocus.REVIEW_PRICE
                            )
                        }
                )
            }

            if (summary.unavailableCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(
                            summary.unavailableCount.toFloat()
                        )
                        .fillMaxHeight()
                        .background(
                            DecisionCheckColor.copy(
                                alpha = if (
                                    selectedFocus == null ||
                                    selectedFocus ==
                                    DecisionFocus.NEED_CHECK
                                ) {
                                    1f
                                } else {
                                    0.35f
                                }
                            )
                        )
                        .clickable {
                            onFocusSelected(
                                DecisionFocus.NEED_CHECK
                            )
                        }
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
            maxLines = 1
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
                            "PRICE PRIORITY"
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

private fun DashboardDecisionSummary.focusText(
    focus: DecisionFocus?
): String {
    return when (focus) {
        DecisionFocus.COMPETITIVE ->
            "$shopCompetitiveCount products are priced at or below the available online price."

        DecisionFocus.REVIEW_PRICE ->
            "$onlineCheaperCount products have a lower online offer. Review these first."

        DecisionFocus.NEED_CHECK ->
            "$unavailableCount products need an individual fresh price check."

        null -> when {
            onlineCheaperCount > 0 &&
                    biggestSavingProductName != null ->
                "Start with $biggestSavingProductName — it has the largest online price gap."

            shopCompetitiveCount > 0 &&
                    unavailableCount == 0 ->
                "Your available comparisons currently look competitive."

            unavailableCount > 0 ->
                "Check the unavailable products before making a pricing decision."

            else ->
                "No reliable online comparison is currently available."
        }
    }
}