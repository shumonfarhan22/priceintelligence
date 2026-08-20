package com.supreme.priceintelligence.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.data.PriceRetailer
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.logo_amazon
import com.supreme.priceintelligence.resources.logo_flipkart
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun PriceHistorySection(
    entries: List<PriceHistoryEntry>,
    isLoading: Boolean
) {
    val summaries = PriceRetailer.entries.mapNotNull { retailer ->
        summarizePriceHistory(entries, retailer)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.16f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SAVED INTELLIGENCE",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.9.sp
            )

            Text(
                text = "Price history",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Every successful online check is stored on this device. The newest 60 checks per retailer are kept.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            when {
                isLoading -> LoadingPriceHistory()
                summaries.isEmpty() -> EmptyPriceHistory()
                else -> summaries.forEach { summary ->
                    RetailerPriceHistoryCard(summary)
                }
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
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
    summary: RetailerPriceHistorySummary
) {
    val retailerName = when (summary.retailer) {
        PriceRetailer.AMAZON -> "Amazon"
        PriceRetailer.FLIPKART -> "Flipkart"
    }

    val retailerAccent = when (summary.retailer) {
        PriceRetailer.AMAZON -> Color(0xFFFFA41C)
        PriceRetailer.FLIPKART -> Color(0xFF2874F0)
    }

    val retailerLogo = when (summary.retailer) {
        PriceRetailer.AMAZON -> Res.drawable.logo_amazon
        PriceRetailer.FLIPKART -> Res.drawable.logo_flipkart
    }

    val movementText = movementDescription(summary)
    val movementColor = when (summary.movement) {
        PriceMovement.LOWER -> MaterialTheme.colorScheme.primary
        PriceMovement.HIGHER -> MaterialTheme.colorScheme.error
        PriceMovement.UNCHANGED,
        PriceMovement.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(
            width = 1.dp,
            color = retailerAccent.copy(alpha = 0.42f)
        )
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(
                        width = 82.dp,
                        height = 36.dp
                    ),
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Image(
                        painter = painterResource(retailerLogo),
                        contentDescription = "$retailerName logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(
                            horizontal = 6.dp,
                            vertical = 7.dp
                        )
                    )
                }

                Text(
                    text = if (summary.observationCount == 1) {
                        "1 SAVED CHECK"
                    } else {
                        "${summary.observationCount} SAVED CHECKS"
                    },
                    color = retailerAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriceHistoryMetric(
                    label = "LATEST",
                    value = formatIndianPrice(summary.latestPrice),
                    modifier = Modifier.weight(1f)
                )

                PriceHistoryMetric(
                    label = "LOWEST SAVED",
                    value = formatIndianPrice(summary.lowestSavedPrice),
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = movementColor.copy(alpha = 0.09f)
            ) {
                Text(
                    text = movementText,
                    color = movementColor,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 11.dp,
                        vertical = 8.dp
                    )
                )
            }

            if (summary.recentPrices.size > 1) {
                Text(
                    text = "Recent • " + summary.recentPrices
                        .asReversed()
                        .joinToString(" → ") { price ->
                            formatIndianPrice(price)
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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

private fun movementDescription(summary: RetailerPriceHistorySummary): String =
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 9.dp
            )
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.width(2.dp))

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
