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
@Composable
fun DashboardDecisionSummaryCard(
    summary: DashboardDecisionSummary,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "RETAIL PRICE POSITION • PAGE $currentPage",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Competitive position",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text =
                    "${summary.comparedCount} products on this page • " +
                        "${summary.livePriceProductCount} checked live",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecisionMetric(
                    value = summary.shopCompetitiveCount,
                    label = "Competitive",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                DecisionMetric(
                    value = summary.onlineCheaperCount,
                    label = "Review price",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                DecisionMetric(
                    value = summary.unavailableCount,
                    label = "Need check",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.035f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.22f
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 13.dp,
                        vertical = 11.dp
                    )
                ) {
                    Text(
                        text = "RETAILER ACTION",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
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
                    text =
                        "Some results use saved prices. Open a product " +
                            "and refresh it before changing its selling price.",
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

private fun DashboardDecisionSummary.recommendationText(): String {
    val productName = biggestSavingProductName
    val priceGap = biggestSavingAmount
    val onlinePrice = priorityOnlinePrice
    val purchaseCost = priorityPurchaseCost?.takeIf { cost ->
        cost.isFinite() && cost > 0.0
    }

    if (
        productName != null &&
        priceGap != null &&
        onlinePrice != null
    ) {
        if (purchaseCost == null) {
            return "Review $productName. Online is " +
                    "${formatIndianPrice(priceGap)} below your " +
                    "selling price. Add the purchase cost before " +
                    "considering a price match."
        }

        if (onlinePrice <= purchaseCost + 0.01) {
            return "Protect your margin on $productName. The " +
                    "online price ${formatIndianPrice(onlinePrice)} " +
                    "is at or below your purchase cost " +
                    "${formatIndianPrice(purchaseCost)}. Do not match " +
                    "without reviewing supplier cost or creating a bundle."
        }

        val matchedGrossProfit = onlinePrice - purchaseCost
        val matchedMarginPercent =
            matchedGrossProfit / onlinePrice * 100.0

        return "Review $productName. Online is " +
                "${formatIndianPrice(priceGap)} below your selling " +
                "price. Matching it would leave estimated gross profit " +
                "${formatIndianPrice(matchedGrossProfit)} " +
                "(${formatPercent(matchedMarginPercent)} margin) " +
                "before tax and other business costs."
    }

    if (shopCompetitiveCount > 0 && unavailableCount == 0) {
        return "Your selling price is best or matched for every " +
                "product with an available comparison."
    }

    if (shopCompetitiveCount > 0) {
        return "Your selling prices are competitive where online " +
                "prices are available. Check unavailable products " +
                "individually when needed."
    }

    return "No reliable online comparison is available. Open a " +
            "product and check its price before taking action."
}