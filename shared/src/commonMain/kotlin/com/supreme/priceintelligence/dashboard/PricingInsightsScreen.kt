@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.supreme.priceintelligence.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.supreme.priceintelligence.settings.InsightCustomization
import com.supreme.priceintelligence.ui.components.ScrollAwareHeader
import com.supreme.priceintelligence.ui.components.rememberScrollAwareHeaderVisible
import com.supreme.priceintelligence.ui.theme.supremeColors
import com.supreme.priceintelligence.ui.theme.tintedSurface
import kotlin.math.absoluteValue
import kotlin.time.Clock

internal enum class InsightGroup {
    COMPETITIVE_FRESH,
    COMPETITIVE_DUE,
    REVIEW_FRESH,
    REVIEW_DUE,
    AMAZON_PRESSURE,
    FLIPKART_PRESSURE,
    ONLINE_LOWER,
    NEAR_MATCH,
    SHOP_LOWER,
    NEEDS_CHECK,
    MISSING_LINKS,
    MISSING_PRICES,
    MISSING_COSTS
}

internal enum class InsightPosition {
    COMPETITIVE,
    REVIEW,
    NO_COMPARISON
}

internal enum class PriceGapBand {
    ONLINE_LOWER,
    NEAR_MATCH,
    SHOP_LOWER,
    NO_COMPARISON
}

internal data class InsightProduct(
    val item: InventoryItem,
    val position: InsightPosition,
    val gapBand: PriceGapBand,
    val needsCheck: Boolean,
    val amazonAlert: Boolean,
    val flipkartAlert: Boolean,
    val amazonFresh: Boolean,
    val flipkartFresh: Boolean,
    val missingSavedPrice: Boolean
)

private data class BrandInsight(
    val name: String,
    val total: Int,
    val competitive: Int,
    val review: Int,
    val unresolved: Int
)

private data class PricingInsightsSnapshot(
    val products: List<InsightProduct>,
    val competitiveFresh: Int,
    val competitiveDue: Int,
    val reviewFresh: Int,
    val reviewDue: Int,
    val noComparison: Int,
    val amazonAlerts: Int,
    val flipkartAlerts: Int,
    val amazonFresh: Int,
    val flipkartFresh: Int,
    val onlineLower: Int,
    val nearMatch: Int,
    val shopLower: Int,
    val brands: List<BrandInsight>,
    val needsCheck: Int,
    val missingLinks: Int,
    val missingPrices: Int,
    val missingCosts: Int
)

@Composable
internal fun PricingInsightsScreen(
    viewModel: DashboardViewModel,
    insightCustomization: InsightCustomization,
    reduceMotionEnabled: Boolean,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val snapshot = remember(state.allMatchingItems) {
        buildPricingInsightsSnapshot(
            items = state.allMatchingItems,
            nowMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    var selectedGroupName by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedBrand by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedProductId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(state.priceFilter) {
        selectedGroupName = when (state.priceFilter) {
            PricePositionFilter.COMPETITIVE ->
                InsightGroup.SHOP_LOWER.name

            PricePositionFilter.REVIEW ->
                InsightGroup.ONLINE_LOWER.name

            PricePositionFilter.NEEDS_CHECK ->
                InsightGroup.NEEDS_CHECK.name

            null -> selectedGroupName
        }
    }

    val selectedGroup = selectedGroupName?.let { name ->
        InsightGroup.entries.firstOrNull { option ->
            option.name == name
        }
    }

    val selectedProducts = remember(
        snapshot,
        selectedGroup,
        selectedBrand
    ) {
        when {
            selectedBrand != null ->
                snapshot.products.filter { product ->
                    product.item.insightBrand() == selectedBrand
                }

            selectedGroup != null ->
                snapshot.products.filter { product ->
                    product.matches(selectedGroup)
                }

            else -> emptyList()
        }
    }

    val selectedProduct = remember(
        snapshot,
        selectedProductId
    ) {
        selectedProductId?.let { productId ->
            snapshot.products.firstOrNull { product ->
                product.item.id == productId
            }
        }
    }

    LaunchedEffect(selectedProductId) {
        selectedProductId?.let { productId ->
            viewModel.loadPriceHistory(productId)
        }
    }

    fun openGroup(group: InsightGroup) {
        selectedProductId = null
        selectedBrand = null
        selectedGroupName = group.name
    }

    val insightsListState = rememberLazyListState()
    val insightsHeaderVisible =
        rememberScrollAwareHeaderVisible(
            listState = insightsListState
        )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {
        ScrollAwareHeader(
            visible = insightsHeaderVisible,
            reduceMotionEnabled = reduceMotionEnabled
        ) {
            Box(
                modifier = Modifier.padding(
                    horizontal = 16.dp
                )
            ) {
                PricingInsightsHeader(
                    onNavigateHome = onNavigateHome
                )
            }
        }

        LazyColumn(
            state = insightsListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 6.dp,
                end = 16.dp,
                bottom = 28.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            item(key = "decision-matrix") {
                DecisionMatrixCard(
                    snapshot = snapshot,
                    onOpenGroup = ::openGroup
                )
            }

            item(key = "retailer-pressure") {
                RetailerPressureCard(
                    snapshot = snapshot,
                    onAmazonClick = {
                        openGroup(
                            InsightGroup.AMAZON_PRESSURE
                        )
                    },
                    onFlipkartClick = {
                        openGroup(
                            InsightGroup.FLIPKART_PRESSURE
                        )
                    }
                )
            }

        item(key = "price-gap-distribution") {
            PriceGapDistributionCard(
                snapshot = snapshot,
                onOnlineLowerClick = {
                    openGroup(
                        InsightGroup.ONLINE_LOWER
                    )
                },
                onNearMatchClick = {
                    openGroup(
                        InsightGroup.NEAR_MATCH
                    )
                },
                onShopLowerClick = {
                    openGroup(
                        InsightGroup.SHOP_LOWER
                    )
                }
            )
        }

        item(key = "brand-health") {
            BrandHealthCard(
                brands = snapshot.brands,
                onBrandClick = { brand ->
                    selectedProductId = null
                    selectedGroupName = null
                    selectedBrand = brand
                }
            )
        }

        item(key = "data-quality") {
            DataQualityCard(
                snapshot = snapshot,
                onNeedsCheckClick = {
                    openGroup(
                        InsightGroup.NEEDS_CHECK
                    )
                },
                onMissingLinksClick = {
                    openGroup(
                        InsightGroup.MISSING_LINKS
                    )
                },
                onMissingPricesClick = {
                    openGroup(
                        InsightGroup.MISSING_PRICES
                    )
                },
                onMissingCostsClick = {
                    openGroup(
                        InsightGroup.MISSING_COSTS
                    )
                }
            )
        }
    }
    }

    if (selectedGroup != null || selectedBrand != null) {
        val dismissInsights: () -> Unit = {
            selectedProductId = null
            selectedGroupName = null
            selectedBrand = null
        }

        if (selectedProduct == null) {
            InsightProductsDialog(
                title = selectedBrand?.let { brand ->
                    "$brand brand health"
                } ?: selectedGroup.insightTitle(),
                products = selectedProducts,
                onProductSelected = { product ->
                    selectedProductId = product.item.id
                },
                onDismiss = dismissInsights
            )
        } else {
            InsightProductAnalysisDialog(
                product = selectedProduct,
                activeGroup = selectedGroup,
                activeBrand = selectedBrand,
                contextProductCount = selectedProducts.size,
                priceHistory = state.priceHistoryByProduct[
                    selectedProduct.item.id
                ].orEmpty(),
                isHistoryLoading =
                    selectedProduct.item.id in
                        state.historyLoadingProductIds,
                isRefreshing =
                    selectedProduct.item.id in
                        state.refreshingProductIds,
                isConnected = state.isConnected,
                insightCustomization = insightCustomization,
                reduceMotionEnabled = reduceMotionEnabled,
                onBack = {
                    selectedProductId = null
                },
                onRefresh = {
                    viewModel.refreshProduct(
                        selectedProduct.item.id
                    )
                },
                onDismiss = dismissInsights
            )
        }
    }
}

@Composable
private fun PricingInsightsHeader(
    onNavigateHome: () -> Unit
) {
    val supremeColors = MaterialTheme.supremeColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateHome) {
            Icon(
                imageVector =
                    Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to launch page"
            )
        }

        Surface(
            shape = RoundedCornerShape(13.dp),
            color = supremeColors.tintedSurface(
                roleColor =
                    MaterialTheme.colorScheme.primary,
                strength = 0.12f,
                lightBase =
                    MaterialTheme.colorScheme.background
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Dashboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(9.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "PRICING INSIGHTS",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Understand your shop position",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DecisionMatrixCard(
    snapshot: PricingInsightsSnapshot,
    onOpenGroup: (InsightGroup) -> Unit
) {
    InsightCard(
        title = "DECISION MATRIX"
    ) {
        Text(
            text = "Position × freshness",
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Spacer(
                modifier = Modifier.weight(0.82f)
            )

            MatrixColumnLabel(
                text = "Fresh",
                color =
                    MaterialTheme.supremeColors.competitive,
                modifier = Modifier.weight(1f)
            )

            MatrixColumnLabel(
                text = "Check due",
                color =
                    MaterialTheme.supremeColors.warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        MatrixRow(
            label = "Competitive",
            labelColor =
                MaterialTheme.supremeColors.competitive,
            firstValue = snapshot.competitiveFresh,
            secondValue = snapshot.competitiveDue,
            firstColor =
                MaterialTheme.supremeColors.competitive,
            secondColor =
                MaterialTheme.supremeColors.warning,
            onFirstClick = {
                onOpenGroup(
                    InsightGroup.COMPETITIVE_FRESH
                )
            },
            onSecondClick = {
                onOpenGroup(
                    InsightGroup.COMPETITIVE_DUE
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        MatrixRow(
            label = "Review",
            labelColor =
                MaterialTheme.colorScheme.error,
            firstValue = snapshot.reviewFresh,
            secondValue = snapshot.reviewDue,
            firstColor =
                MaterialTheme.colorScheme.error,
            secondColor =
                MaterialTheme.colorScheme.error,
            onFirstClick = {
                onOpenGroup(
                    InsightGroup.REVIEW_FRESH
                )
            },
            onSecondClick = {
                onOpenGroup(
                    InsightGroup.REVIEW_DUE
                )
            }
        )

        if (snapshot.noComparison > 0) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text =
                    "${snapshot.noComparison} products have no usable online comparison",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Tap a cell to see products",
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MatrixColumnLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun MatrixRow(
    label: String,
    labelColor: Color,
    firstValue: Int,
    secondValue: Int,
    firstColor: Color,
    secondColor: Color,
    onFirstClick: () -> Unit,
    onSecondClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.82f),
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        MatrixCell(
            value = firstValue,
            color = firstColor,
            onClick = onFirstClick,
            modifier = Modifier.weight(1f)
        )

        MatrixCell(
            value = secondValue,
            color = secondColor,
            onClick = onSecondClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MatrixCell(
    value: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(
            onClick = onClick
        ),
        shape = RoundedCornerShape(13.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.44f)
        )
    ) {
        Box(
            modifier = Modifier.padding(
                vertical = 15.dp
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                color = color,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun RetailerPressureCard(
    snapshot: PricingInsightsSnapshot,
    onAmazonClick: () -> Unit,
    onFlipkartClick: () -> Unit
) {
    InsightCard(
        title = "RETAILER PRESSURE"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            RetailerPressureTile(
                name = "Amazon",
                alertCount = snapshot.amazonAlerts,
                freshCount = snapshot.amazonFresh,
                accent =
                    MaterialTheme.supremeColors.warning,
                onClick = onAmazonClick,
                modifier = Modifier.weight(1f)
            )

            RetailerPressureTile(
                name = "Flipkart",
                alertCount =
                    snapshot.flipkartAlerts,
                freshCount =
                    snapshot.flipkartFresh,
                accent =
                    MaterialTheme.colorScheme.secondary,
                onClick = onFlipkartClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RetailerPressureTile(
    name: String,
    alertCount: Int,
    freshCount: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(
            onClick = onClick
        ),
        shape = RoundedCornerShape(14.dp),
        color =
            MaterialTheme.supremeColors.panelStrong,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.supremeColors.border
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = name,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$alertCount price alerts",
                color =
                    MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$freshCount fresh",
                color =
                    MaterialTheme.supremeColors.competitive,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PriceGapDistributionCard(
    snapshot: PricingInsightsSnapshot,
    onOnlineLowerClick: () -> Unit,
    onNearMatchClick: () -> Unit,
    onShopLowerClick: () -> Unit
) {
    val comparableCount = (
            snapshot.onlineLower +
                    snapshot.nearMatch +
                    snapshot.shopLower
            ).coerceAtLeast(1)

    InsightCard(
        title = "PRICE GAP DISTRIBUTION"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            horizontalArrangement =
                Arrangement.spacedBy(2.dp)
        ) {
            GapBarSegment(
                weight =
                    snapshot.onlineLower.toFloat() /
                            comparableCount,
                color =
                    MaterialTheme.colorScheme.error
            )

            GapBarSegment(
                weight =
                    snapshot.nearMatch.toFloat() /
                            comparableCount,
                color =
                    MaterialTheme.supremeColors.warning
            )

            GapBarSegment(
                weight =
                    snapshot.shopLower.toFloat() /
                            comparableCount,
                color =
                    MaterialTheme.supremeColors.competitive
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            GapLegendTile(
                count = snapshot.onlineLower,
                title = "Online lower",
                subtitle = "Needs review",
                color =
                    MaterialTheme.colorScheme.error,
                onClick = onOnlineLowerClick,
                modifier = Modifier.weight(1f)
            )

            GapLegendTile(
                count = snapshot.nearMatch,
                title = "Near match",
                subtitle = "Within 5%",
                color =
                    MaterialTheme.supremeColors.warning,
                onClick = onNearMatchClick,
                modifier = Modifier.weight(1f)
            )

            GapLegendTile(
                count = snapshot.shopLower,
                title = "Shop lower",
                subtitle = "Competitive",
                color =
                    MaterialTheme.supremeColors.competitive,
                onClick = onShopLowerClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowScope.GapBarSegment(
    weight: Float,
    color: Color
) {
    if (weight > 0f) {
        Box(
            modifier = Modifier
                .weight(weight)
                .height(12.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(6.dp)
                )
        )
    }
}

@Composable
private fun GapLegendTile(
    count: Int,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = count.toString(),
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = title,
            color =
                MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = subtitle,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BrandHealthCard(
    brands: List<BrandInsight>,
    onBrandClick: (String) -> Unit
) {
    InsightCard(
        title = "BRAND HEALTH"
    ) {
        if (brands.isEmpty()) {
            Text(
                text =
                    "Add products to see brand health.",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        } else {
            brands.take(6).forEachIndexed {
                    index,
                    brand ->

                if (index > 0) {
                    HorizontalDivider(
                        color =
                            MaterialTheme.supremeColors.border
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onBrandClick(brand.name)
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = brand.name,
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                                "${brand.competitive} of ${brand.total} competitive",
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }

                    BrandHealthBar(
                        competitive =
                            brand.competitive,
                        review = brand.review,
                        unresolved =
                            brand.unresolved,
                        modifier = Modifier.width(92.dp)
                    )

                    Icon(
                        imageVector =
                            Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHealthBar(
    competitive: Int,
    review: Int,
    unresolved: Int,
    modifier: Modifier = Modifier
) {
    val total = (
            competitive +
                    review +
                    unresolved
            ).coerceAtLeast(1)

    Row(
        modifier = modifier.height(8.dp),
        horizontalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {
        GapBarSegment(
            weight =
                competitive.toFloat() / total,
            color =
                MaterialTheme.supremeColors.competitive
        )

        GapBarSegment(
            weight =
                review.toFloat() / total,
            color =
                MaterialTheme.colorScheme.error
        )

        GapBarSegment(
            weight =
                unresolved.toFloat() / total,
            color =
                MaterialTheme.supremeColors.warning
        )
    }
}

@Composable
private fun DataQualityCard(
    snapshot: PricingInsightsSnapshot,
    onNeedsCheckClick: () -> Unit,
    onMissingLinksClick: () -> Unit,
    onMissingPricesClick: () -> Unit,
    onMissingCostsClick: () -> Unit
) {
    InsightCard(
        title = "DATA QUALITY"
    ) {
        DataQualityRow(
            icon = Icons.Rounded.Schedule,
            text =
                "${snapshot.needsCheck} prices need checking",
            color =
                MaterialTheme.supremeColors.warning,
            onClick = onNeedsCheckClick
        )

        HorizontalDivider(
            color =
                MaterialTheme.supremeColors.border
        )

        DataQualityRow(
            icon = Icons.Rounded.LinkOff,
            text =
                "${snapshot.missingLinks} missing retailer links",
            color =
                MaterialTheme.supremeColors.warning,
            onClick = onMissingLinksClick
        )

        HorizontalDivider(
            color =
                MaterialTheme.supremeColors.border
        )

        DataQualityRow(
            icon = Icons.Rounded.PriorityHigh,
            text =
                "${snapshot.missingPrices} linked products without a saved price",
            color =
                MaterialTheme.colorScheme.error,
            onClick = onMissingPricesClick
        )

        HorizontalDivider(
            color =
                MaterialTheme.supremeColors.border
        )

        DataQualityRow(
            icon = Icons.Rounded.PriorityHigh,
            text =
                "${snapshot.missingCosts} missing purchase costs",
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onMissingCostsClick
        )
    }
}

@Composable
private fun DataQualityRow(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .padding(7.dp)
                    .size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color =
                MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Icon(
            imageVector =
                Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint =
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun InsightCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.supremeColors.border
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color =
                    MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
private fun InsightProductsDialog(
    title: String,
    products: List<InsightProduct>,
    onProductSelected: (InsightProduct) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(24.dp),
            color =
                MaterialTheme.supremeColors.panelStrong,
            border = BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme.supremeColors.border
            )
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 18.dp,
                            end = 8.dp,
                            top = 10.dp,
                            bottom = 8.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                                if (products.size == 1) {
                                    "1 product"
                                } else {
                                    "${products.size} products"
                                },
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector =
                                Icons.Rounded.Close,
                            contentDescription =
                                "Close product group"
                        )
                    }
                }

                HorizontalDivider(
                    color =
                        MaterialTheme.supremeColors.border
                )

                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                "No products are in this group.",
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding =
                            PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                    ) {
                        items(
                            items = products,
                            key = { product ->
                                product.item.id
                            }
                        ) { product ->
                            InsightProductRow(
                                product = product,
                                onClick = {
                                    onProductSelected(product)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightProductRow(
    product: InsightProduct,
    onClick: () -> Unit
) {
    val item = product.item

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(
                        RoundedCornerShape(18.dp)
                    )
                    .background(
                        MaterialTheme
                            .supremeColors
                            .imagePanel
                    ),
                contentAlignment =
                    Alignment.Center
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
                    fontWeight =
                        FontWeight.ExtraBold
                )

                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription =
                            "${item.productName} product image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale =
                            ContentScale.Fit
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.productName,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text = product.insightReason(),
                    color =
                        when (product.position) {
                            InsightPosition.REVIEW ->
                                MaterialTheme
                                    .colorScheme
                                    .error

                            InsightPosition.COMPETITIVE ->
                                MaterialTheme
                                    .supremeColors
                                    .competitive

                            InsightPosition.NO_COMPARISON ->
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        },
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Open product analysis",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(
                top = 10.dp
            ),
            color =
                MaterialTheme.supremeColors.border
        )
    }
}

private fun buildPricingInsightsSnapshot(
    items: List<InventoryItem>,
    nowMillis: Long
): PricingInsightsSnapshot {
    val products = items.map { item ->
        val comparison = compareWithOnlinePrices(
            shopPrice = item.shopPrice,
            amazonPrice = item.amazonLastPrice,
            flipkartPrice =
                item.flipkartLastPrice
        )

        val position =
            when (comparison.shopPosition) {
                ShopPricePosition.LOWER,
                ShopPricePosition.MATCHED ->
                    InsightPosition.COMPETITIVE

                ShopPricePosition.HIGHER ->
                    InsightPosition.REVIEW

                ShopPricePosition.INVALID_SHOP_PRICE,
                ShopPricePosition.NO_ONLINE_PRICE ->
                    InsightPosition.NO_COMPARISON
            }

        val gapPercent =
            comparison.onlineLowestPrice
                ?.takeIf { online ->
                    online > 0.0
                }
                ?.let { online ->
                    (
                            (
                                    item.shopPrice -
                                            online
                                    ).absoluteValue /
                                    online
                            ) * 100.0
                }

        val gapBand = when {
            position == InsightPosition.REVIEW ->
                PriceGapBand.ONLINE_LOWER

            gapPercent != null &&
                    gapPercent <= 5.0 ->
                PriceGapBand.NEAR_MATCH

            position ==
                    InsightPosition.COMPETITIVE ->
                PriceGapBand.SHOP_LOWER

            else ->
                PriceGapBand.NO_COMPARISON
        }

        val amazonValid =
            item.amazonLastPrice
                ?.takeIf { price ->
                    price.isFinite() &&
                            price > 0.0
                }

        val flipkartValid =
            item.flipkartLastPrice
                ?.takeIf { price ->
                    price.isFinite() &&
                            price > 0.0
                }

        InsightProduct(
            item = item,
            position = position,
            gapBand = gapBand,
            needsCheck =
                item.needsPriceCheck(nowMillis),
            amazonAlert =
                amazonValid != null &&
                        item.shopPrice > amazonValid,
            flipkartAlert =
                flipkartValid != null &&
                        item.shopPrice > flipkartValid,
            amazonFresh = retailerPriceIsFresh(
                url = item.amazonUrl,
                price = item.amazonLastPrice,
                checkedAt =
                    item.amazonLastChecked,
                nowMillis = nowMillis
            ),
            flipkartFresh =
                retailerPriceIsFresh(
                    url = item.flipkartUrl,
                    price =
                        item.flipkartLastPrice,
                    checkedAt =
                        item.flipkartLastChecked,
                    nowMillis = nowMillis
                ),
            missingSavedPrice =
                (
                        !item.amazonUrl.isNullOrBlank() &&
                                amazonValid == null
                        ) ||
                        (
                                !item.flipkartUrl.isNullOrBlank() &&
                                        flipkartValid == null
                                )
        )
    }

    val brands = products
        .groupBy { product ->
            product.item.insightBrand()
        }
        .map { entry ->
            val name = entry.key
            val brandProducts = entry.value

            BrandInsight(
                name = name,
                total = brandProducts.size,
                competitive =
                    brandProducts.count { product ->
                        product.position ==
                                InsightPosition.COMPETITIVE
                    },
                review =
                    brandProducts.count { product ->
                        product.position ==
                                InsightPosition.REVIEW
                    },
                unresolved =
                    brandProducts.count { product ->
                        product.position ==
                                InsightPosition.NO_COMPARISON
                    }
            )
        }
        .sortedWith(
            compareByDescending<BrandInsight> { brand ->
                brand.review
            }
                .thenByDescending { brand ->
                    brand.unresolved
                }
                .thenByDescending { brand ->
                    brand.total
                }
                .thenBy { brand ->
                    brand.name
                }
        )

    return PricingInsightsSnapshot(
        products = products,
        competitiveFresh =
            products.count { product ->
                product.position ==
                        InsightPosition.COMPETITIVE &&
                        !product.needsCheck
            },
        competitiveDue =
            products.count { product ->
                product.position ==
                        InsightPosition.COMPETITIVE &&
                        product.needsCheck
            },
        reviewFresh =
            products.count { product ->
                product.position ==
                        InsightPosition.REVIEW &&
                        !product.needsCheck
            },
        reviewDue =
            products.count { product ->
                product.position ==
                        InsightPosition.REVIEW &&
                        product.needsCheck
            },
        noComparison =
            products.count { product ->
                product.position ==
                        InsightPosition.NO_COMPARISON
            },
        amazonAlerts =
            products.count { product ->
                product.amazonAlert
            },
        flipkartAlerts =
            products.count { product ->
                product.flipkartAlert
            },
        amazonFresh =
            products.count { product ->
                product.amazonFresh
            },
        flipkartFresh =
            products.count { product ->
                product.flipkartFresh
            },
        onlineLower =
            products.count { product ->
                product.gapBand ==
                        PriceGapBand.ONLINE_LOWER
            },
        nearMatch =
            products.count { product ->
                product.gapBand ==
                        PriceGapBand.NEAR_MATCH
            },
        shopLower =
            products.count { product ->
                product.gapBand ==
                        PriceGapBand.SHOP_LOWER
            },
        brands = brands,
        needsCheck =
            products.count { product ->
                product.needsCheck
            },
        missingLinks =
            products.count { product ->
                product.item.amazonUrl
                    .isNullOrBlank() &&
                        product.item.flipkartUrl
                            .isNullOrBlank()
            },
        missingPrices =
            products.count { product ->
                product.missingSavedPrice
            },
        missingCosts =
            products.count { product ->
                product.item.purchaseCost
                    ?.takeIf { cost ->
                        cost.isFinite() &&
                                cost > 0.0
                    } == null
            }
    )
}

private fun retailerPriceIsFresh(
    url: String?,
    price: Double?,
    checkedAt: Long?,
    nowMillis: Long
): Boolean {
    if (url.isNullOrBlank()) {
        return false
    }

    if (
        price == null ||
        !price.isFinite() ||
        price <= 0.0
    ) {
        return false
    }

    val checked =
        checkedAt?.takeIf { timestamp ->
            timestamp > 0L
        } ?: return false

    return nowMillis - checked <
            PRICE_FRESHNESS_WINDOW_MILLIS
}

private fun InventoryItem.insightBrand(): String {
    val firstWord = productName
        .trim()
        .substringBefore(' ')
        .ifBlank {
            "Other"
        }

    return firstWord
        .lowercase()
        .replaceFirstChar { character ->
            character.uppercase()
        }
}

private fun InsightProduct.matches(
    group: InsightGroup
): Boolean = when (group) {
    InsightGroup.COMPETITIVE_FRESH ->
        position ==
                InsightPosition.COMPETITIVE &&
                !needsCheck

    InsightGroup.COMPETITIVE_DUE ->
        position ==
                InsightPosition.COMPETITIVE &&
                needsCheck

    InsightGroup.REVIEW_FRESH ->
        position ==
                InsightPosition.REVIEW &&
                !needsCheck

    InsightGroup.REVIEW_DUE ->
        position ==
                InsightPosition.REVIEW &&
                needsCheck

    InsightGroup.AMAZON_PRESSURE ->
        amazonAlert

    InsightGroup.FLIPKART_PRESSURE ->
        flipkartAlert

    InsightGroup.ONLINE_LOWER ->
        gapBand ==
                PriceGapBand.ONLINE_LOWER

    InsightGroup.NEAR_MATCH ->
        gapBand ==
                PriceGapBand.NEAR_MATCH

    InsightGroup.SHOP_LOWER ->
        gapBand ==
                PriceGapBand.SHOP_LOWER

    InsightGroup.NEEDS_CHECK ->
        needsCheck

    InsightGroup.MISSING_LINKS ->
        item.amazonUrl.isNullOrBlank() &&
                item.flipkartUrl.isNullOrBlank()

    InsightGroup.MISSING_PRICES ->
        missingSavedPrice

    InsightGroup.MISSING_COSTS ->
        item.purchaseCost
            ?.takeIf { cost ->
                cost.isFinite() &&
                        cost > 0.0
            } == null
}

private fun InsightGroup?.insightTitle(): String =
    when (this) {
        InsightGroup.COMPETITIVE_FRESH ->
            "Competitive and fresh"

        InsightGroup.COMPETITIVE_DUE ->
            "Competitive but due"

        InsightGroup.REVIEW_FRESH ->
            "Review with fresh prices"

        InsightGroup.REVIEW_DUE ->
            "Review and check due"

        InsightGroup.AMAZON_PRESSURE ->
            "Amazon price alerts"

        InsightGroup.FLIPKART_PRESSURE ->
            "Flipkart price alerts"

        InsightGroup.ONLINE_LOWER ->
            "Online price is lower"

        InsightGroup.NEAR_MATCH ->
            "Prices within 5%"

        InsightGroup.SHOP_LOWER ->
            "Shop price is lower"

        InsightGroup.NEEDS_CHECK ->
            "Prices needing a check"

        InsightGroup.MISSING_LINKS ->
            "Missing retailer links"

        InsightGroup.MISSING_PRICES ->
            "Missing saved prices"

        InsightGroup.MISSING_COSTS ->
            "Missing purchase costs"

        null ->
            "Products"
    }

private fun InsightProduct.insightReason(): String {
    val positionText = when (position) {
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
