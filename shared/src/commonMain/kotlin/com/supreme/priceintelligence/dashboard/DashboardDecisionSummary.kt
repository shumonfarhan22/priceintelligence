package com.supreme.priceintelligence.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DashboardDecisionSummary(
    val comparedCount: Int,
    val onlineCheaperCount: Int,
    val shopCompetitiveCount: Int,
    val unavailableCount: Int,
    val livePriceProductCount: Int,
    val biggestSavingProductName: String? = null,
    val biggestSavingAmount: Double? = null
)

fun List<ProductCardUiState>.buildDecisionSummary(): DashboardDecisionSummary {
    var onlineCheaperCount = 0
    var shopCompetitiveCount = 0
    var unavailableCount = 0
    var livePriceProductCount = 0
    var biggestSavingProductName: String? = null
    var biggestSavingAmount: Double? = null

    forEach { card ->
        val amazonPrice = card.amazonResult?.price ?: card.item.amazonLastPrice
        val flipkartPrice = card.flipkartResult?.price ?: card.item.flipkartLastPrice
        val comparison = compareWithOnlinePrices(
            shopPrice = card.item.shopPrice,
            amazonPrice = amazonPrice,
            flipkartPrice = flipkartPrice
        )

        if (card.amazonResult?.price != null || card.flipkartResult?.price != null) {
            livePriceProductCount += 1
        }

        when (comparison.shopPosition) {
            ShopPricePosition.HIGHER -> {
                onlineCheaperCount += 1
                val saving = comparison.shopDifference
                if (saving != null && saving > (biggestSavingAmount ?: 0.0)) {
                    biggestSavingAmount = saving
                    biggestSavingProductName = card.item.productName
                }
            }

            ShopPricePosition.LOWER,
            ShopPricePosition.MATCHED -> shopCompetitiveCount += 1

            ShopPricePosition.NO_ONLINE_PRICE,
            ShopPricePosition.INVALID_SHOP_PRICE -> unavailableCount += 1
        }
    }

    return DashboardDecisionSummary(
        comparedCount = size,
        onlineCheaperCount = onlineCheaperCount,
        shopCompetitiveCount = shopCompetitiveCount,
        unavailableCount = unavailableCount,
        livePriceProductCount = livePriceProductCount,
        biggestSavingProductName = biggestSavingProductName,
        biggestSavingAmount = biggestSavingAmount
    )
}

@Composable
fun DashboardDecisionSummaryCard(
    summary: DashboardDecisionSummary,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "MARKET SNAPSHOT",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Page $currentPage price position",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                    )
                ) {
                    Text(
                        text = "${summary.livePriceProductCount} LIVE",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(13.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecisionMetric(
                    value = summary.shopCompetitiveCount,
                    label = "Lower / match",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                DecisionMetric(
                    value = summary.onlineCheaperCount,
                    label = "Online lower",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )

                DecisionMetric(
                    value = summary.unavailableCount,
                    label = "No price",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 13.dp,
                        vertical = 11.dp
                    )
                ) {
                    Text(
                        text = "DECISION NOTE",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = summary.recommendationText(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (summary.livePriceProductCount < summary.comparedCount) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Some comparisons use clearly marked saved prices. Check prices before making a final decision.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
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
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            text = value.toString(),
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun DashboardDecisionSummary.recommendationText(): String = when {
    biggestSavingProductName != null && biggestSavingAmount != null ->
        "$biggestSavingProductName has the largest online gap: ${formatIndianPrice(biggestSavingAmount)}."

    shopCompetitiveCount > 0 -> "Your shop is competitive for every product with a usable online price."
    else -> "No usable online comparison is available on this page yet."
}
