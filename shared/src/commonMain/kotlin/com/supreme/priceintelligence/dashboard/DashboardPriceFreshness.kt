package com.supreme.priceintelligence.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.ui.theme.supremeColors

internal const val PRICE_FRESHNESS_WINDOW_MILLIS =
    24L * 60L * 60L * 1000L

data class PriceFreshnessSummary(
    val totalProductCount: Int,
    val linkedProductCount: Int,
    val currentProductCount: Int,
    val needsCheckCount: Int,
    val missingRetailerLinksCount: Int
)

internal fun InventoryItem.needsPriceCheck(
    nowMillis: Long,
    maxAgeMillis: Long = PRICE_FRESHNESS_WINDOW_MILLIS
): Boolean {
    val amazonNeedsCheck = retailerNeedsCheck(
        url = amazonUrl,
        savedPrice = amazonLastPrice,
        checkedAt = amazonLastChecked,
        nowMillis = nowMillis,
        maxAgeMillis = maxAgeMillis
    )

    val flipkartNeedsCheck = retailerNeedsCheck(
        url = flipkartUrl,
        savedPrice = flipkartLastPrice,
        checkedAt = flipkartLastChecked,
        nowMillis = nowMillis,
        maxAgeMillis = maxAgeMillis
    )

    return amazonNeedsCheck || flipkartNeedsCheck
}

internal fun List<InventoryItem>.buildPriceFreshnessSummary(
    nowMillis: Long,
    maxAgeMillis: Long = PRICE_FRESHNESS_WINDOW_MILLIS
): PriceFreshnessSummary {
    var linkedProductCount = 0
    var currentProductCount = 0
    var needsCheckCount = 0
    var missingRetailerLinksCount = 0

    forEach { item ->
        val hasRetailerLink =
            !item.amazonUrl.isNullOrBlank() ||
                    !item.flipkartUrl.isNullOrBlank()

        if (!hasRetailerLink) {
            missingRetailerLinksCount += 1
        } else {
            linkedProductCount += 1

            if (
                item.needsPriceCheck(
                    nowMillis = nowMillis,
                    maxAgeMillis = maxAgeMillis
                )
            ) {
                needsCheckCount += 1
            } else {
                currentProductCount += 1
            }
        }
    }

    return PriceFreshnessSummary(
        totalProductCount = size,
        linkedProductCount = linkedProductCount,
        currentProductCount = currentProductCount,
        needsCheckCount = needsCheckCount,
        missingRetailerLinksCount = missingRetailerLinksCount
    )
}

private fun retailerNeedsCheck(
    url: String?,
    savedPrice: Double?,
    checkedAt: Long?,
    nowMillis: Long,
    maxAgeMillis: Long
): Boolean {
    if (url.isNullOrBlank()) return false

    if (
        savedPrice == null ||
        !savedPrice.isFinite() ||
        savedPrice <= 0.0
    ) {
        return true
    }

    val validCheckedAt =
        checkedAt?.takeIf { timestamp ->
            timestamp > 0L
        } ?: return true

    val ageMillis = nowMillis - validCheckedAt

    return ageMillis >= maxAgeMillis
}

@Composable
internal fun DashboardPriceFreshnessCard(
    summary: PriceFreshnessSummary,
    active: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val needsCheck = summary.needsCheckCount
    val productWord =
        if (needsCheck == 1) {
            "product"
        } else {
            "products"
        }

    val accentColor = when {
        active -> MaterialTheme.supremeColors.warning
        needsCheck > 0 -> MaterialTheme.supremeColors.warning
        summary.linkedProductCount == 0 ->
            MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.supremeColors.competitive
    }

    val title = when {
        active ->
            "Showing $needsCheck $productWord that need checking"

        needsCheck > 0 ->
            "$needsCheck $productWord need a fresh price check"

        summary.linkedProductCount == 0 ->
            "No retailer links available to check"

        else ->
            "Linked prices were checked within 24 hours"
    }

    val supportingText = when {
        summary.linkedProductCount == 0 ->
            "Add an Amazon or Flipkart link in Inventory"

        needsCheck > 0 ->
            buildString {
                append("Missing or at least 24 hours old")
                append(" • ")
                append(summary.currentProductCount)
                append(" current")

                if (summary.missingRetailerLinksCount > 0) {
                    append(" • ")
                    append(summary.missingRetailerLinksCount)
                    append(" without links")
                }
            }

        summary.missingRetailerLinksCount > 0 ->
            "${summary.currentProductCount} current • " +
                    "${summary.missingRetailerLinksCount} without links"

        else ->
            "${summary.currentProductCount} linked products are current"
    }

    val canToggle = needsCheck > 0 || active
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(shape)
            .then(
                if (canToggle) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onToggle
                    )
                } else {
                    Modifier
                }
            )
            .semantics {
                stateDescription =
                    if (active) {
                        "Needs check filter active"
                    } else {
                        "$needsCheck products need a fresh price check"
                    }
            },
        shape = shape,
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.34f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 13.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    summary.linkedProductCount == 0 ->
                        Icons.Rounded.LinkOff

                    needsCheck > 0 ->
                        Icons.Rounded.Schedule

                    else ->
                        Icons.Rounded.CheckCircle
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(23.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = supportingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (canToggle) {
                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text =
                            if (active) {
                                "CLEAR"
                            } else {
                                "SHOW"
                            },
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription =
                        "Dismiss price freshness reminder",
                    tint =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}