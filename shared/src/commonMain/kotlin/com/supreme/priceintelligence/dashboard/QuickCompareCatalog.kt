package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.ui.layout.adaptiveLayoutPolicy
import com.supreme.priceintelligence.ui.theme.supremeColors

private data class QuickCompareStatus(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
internal fun QuickCompareCatalogGrid(
    cards: List<ProductCardUiState>,
    query: String,
    gridState: LazyGridState,
    showResults: Boolean,
    reduceMotionEnabled: Boolean,
    isLoading: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    onProductClick: (ProductCardUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(
        showResults,
        query,
        currentPage
    ) {
        gridState.scrollToItem(0)
    }

    val skeletonShimmerProgress =
        if (reduceMotionEnabled) {
            0f
        } else {
            val transition =
                rememberInfiniteTransition(
                    label =
                        "quickCompareSkeletonShimmer"
                )

            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(
                            durationMillis = 1800,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                label =
                    "quickCompareSkeletonProgress"
            )

            progress
        }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val layoutPolicy =
                adaptiveLayoutPolicy(
                    availableWidthDp =
                        maxWidth.value,
                    fontScale =
                        LocalDensity.current.fontScale
                )

            // Measure the real space available to each card instead of changing
            // to one column for every small increase in the phone's font size.
            val twoColumnCardWidth =
                (maxWidth - 44.dp) / 2f

            val minimumTwoColumnCardWidth =
                if (layoutPolicy.fontScale >= 1.30f) {
                    175.dp
                } else {
                    145.dp
                }

            val singleColumn =
                twoColumnCardWidth <
                    minimumTwoColumnCardWidth

            LazyVerticalGrid(
                columns =
                    if (singleColumn) {
                        GridCells.Fixed(1)
                    } else {
                        GridCells.Fixed(2)
                    },
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 96.dp
                ),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                if (!showResults) {
                    items(
                        count = 4,
                        key = { index ->
                            "quick-placeholder-$index"
                        },
                        contentType = {
                            "quick-placeholder"
                        }
                    ) {
                        QuickComparePlaceholderCard(
                            singleColumn =
                                singleColumn,
                            reduceMotionEnabled =
                                reduceMotionEnabled,
                            shimmerProgress =
                                skeletonShimmerProgress
                        )
                    }
                } else {
                    if (
                        cards.isEmpty() &&
                        !isLoading
                    ) {
                        item(
                            key = "quick-compare-empty",
                            span = {
                                GridItemSpan(maxLineSpan)
                            }
                        ) {
                            QuickCompareEmptyState(
                                isSearching = true
                            )
                        }
                    }

                    items(
                        items = cards,
                        key = { card ->
                            card.item.id
                        },
                        contentType = {
                            "quick-compare-product"
                        }
                    ) { card ->
                        QuickCompareCatalogCard(
                            card = card,
                            singleColumn =
                                singleColumn,
                            onClick = {
                                onProductClick(card)
                            }
                        )
                    }

                    if (totalPages > 1) {
                        item(
                            key =
                                "quick-compare-page-" +
                                    currentPage,
                            span = {
                                GridItemSpan(maxLineSpan)
                            }
                        ) {
                            QuickComparePagination(
                                currentPage =
                                    currentPage,
                                totalPages =
                                    totalPages,
                                onPageSelected =
                                    onPageSelected
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickComparePlaceholderCard(
    singleColumn: Boolean,
    reduceMotionEnabled: Boolean,
    shimmerProgress: Float
) {
    val placeholderColor =
        MaterialTheme
            .supremeColors
            .border
            .copy(alpha = 0.48f)

    val shimmerColor =
        MaterialTheme
            .colorScheme
            .onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .drawWithContent {
                drawContent()

                if (!reduceMotionEnabled) {
                    val horizontalCycle =
                        size.width * 1.30f

                    val verticalCycle =
                        size.height * 1.30f

                    val horizontalMovement =
                        horizontalCycle *
                            shimmerProgress

                    val verticalMovement =
                        verticalCycle *
                            shimmerProgress

                    drawRect(
                        brush =
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    shimmerColor.copy(
                                        alpha = 0.025f
                                    ),
                                    shimmerColor.copy(
                                        alpha = 0.075f
                                    ),
                                    shimmerColor.copy(
                                        alpha = 0.18f
                                    ),
                                    shimmerColor.copy(
                                        alpha = 0.075f
                                    ),
                                    shimmerColor.copy(
                                        alpha = 0.025f
                                    ),
                                    Color.Transparent,
                                    Color.Transparent
                                ),
                                start = Offset(
                                    x =
                                        -horizontalCycle +
                                            horizontalMovement,
                                    y =
                                        -verticalCycle +
                                            verticalMovement
                                ),
                                end = Offset(
                                    x = horizontalMovement,
                                    y = verticalMovement
                                ),
                                tileMode =
                                    TileMode.Repeated
                            )
                    )
                }
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme
                    .supremeColors
                    .border
                    .copy(alpha = 0.50f)
        )
    ) {
        if (singleColumn) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(
                            RoundedCornerShape(16.dp)
                        )
                        .background(
                            MaterialTheme
                                .supremeColors
                                .panelMuted
                        )
                )

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(15.dp)
                            .clip(
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                placeholderColor
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(15.dp)
                            .clip(
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                placeholderColor
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.48f)
                            .height(11.dp)
                            .clip(
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                placeholderColor.copy(
                                    alpha = 0.70f
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.64f)
                            .height(25.dp)
                            .clip(
                                RoundedCornerShape(14.dp)
                            )
                            .background(
                                placeholderColor.copy(
                                    alpha = 0.62f
                                )
                            )
                    )
                }
            }
        } else {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.08f)
                        .background(
                            MaterialTheme
                                .supremeColors
                                .panelMuted
                        )
                )

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .height(14.dp)
                            .clip(
                                RoundedCornerShape(7.dp)
                            )
                            .background(
                                placeholderColor
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.70f)
                            .height(14.dp)
                            .clip(
                                RoundedCornerShape(7.dp)
                            )
                            .background(
                                placeholderColor
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(10.dp)
                            .clip(
                                RoundedCornerShape(7.dp)
                            )
                            .background(
                                placeholderColor.copy(
                                    alpha = 0.70f
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.66f)
                            .height(24.dp)
                            .clip(
                                RoundedCornerShape(14.dp)
                            )
                            .background(
                                placeholderColor.copy(
                                    alpha = 0.62f
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCompareCatalogCard(
    card: ProductCardUiState,
    singleColumn: Boolean,
    onClick: () -> Unit
) {
    val item = card.item
    val status =
        quickCompareStatus(card)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme
                    .supremeColors
                    .border
                    .copy(alpha = 0.72f)
        )
    ) {
        if (singleColumn) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                QuickCompareProductImage(
                    imageUrl = item.imageUrl,
                    productName =
                        item.productName,
                    modifier =
                        Modifier.size(126.dp)
                )

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                QuickCompareProductInformation(
                    productName =
                        item.productName,
                    shopPrice =
                        item.shopPrice,
                    status = status,
                    singleColumn = true,
                    modifier =
                        Modifier.weight(1f)
                )
            }
        } else {
            Column {
                QuickCompareProductImage(
                    imageUrl = item.imageUrl,
                    productName =
                        item.productName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.08f)
                )

                QuickCompareProductInformation(
                    productName =
                        item.productName,
                    shopPrice =
                        item.shopPrice,
                    status = status,
                    singleColumn = false,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickCompareProductImage(
    imageUrl: String?,
    productName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                MaterialTheme
                    .supremeColors
                    .imagePanel
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text =
                productName
                    .trim()
                    .firstOrNull()
                    ?.uppercase()
                    ?: "P",
            color = Color(0xFF475569),
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun QuickCompareProductInformation(
    productName: String,
    shopPrice: Double,
    status: QuickCompareStatus,
    singleColumn: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = productName,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface,
            fontSize =
                if (singleColumn) {
                    15.sp
                } else {
                    13.sp
                },
            lineHeight =
                if (singleColumn) {
                    20.sp
                } else {
                    18.sp
                },
            fontWeight = FontWeight.Bold,
            maxLines =
                if (singleColumn) {
                    4
                } else {
                    2
                },
            overflow = TextOverflow.Ellipsis
        )

        Spacer(
            modifier =
                Modifier.padding(top = 5.dp)
        )

        Text(
            text = "SHOP PRICE",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp
        )

        Text(
            text =
                formatIndianPrice(shopPrice),
            color =
                MaterialTheme.colorScheme.primary,
            fontSize =
                if (singleColumn) {
                    17.sp
                } else {
                    15.sp
                },
            fontWeight =
                FontWeight.ExtraBold,
            maxLines = 1
        )

        Spacer(
            modifier =
                Modifier.padding(top = 7.dp)
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color =
                status.color.copy(
                    alpha = 0.13f
                )
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 6.dp
                ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = status.icon,
                    contentDescription = null,
                    tint = status.color,
                    modifier = Modifier.size(14.dp)
                )

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

                Text(
                    text = status.label,
                    color = status.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickCompareEmptyState(
    isSearching: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme
                    .supremeColors
                    .border
        )
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.Search,
                contentDescription = null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,
                modifier = Modifier.size(30.dp)
            )

            Text(
                text =
                    if (isSearching) {
                        "No matching products"
                    } else {
                        "Your inventory is empty"
                    },
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    if (isSearching) {
                        "Try a shorter product name, barcode, Amazon link, or Flipkart link."
                    } else {
                        "Add a product from Inventory before using Quick Compare."
                    },
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun QuickComparePagination(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        TextButton(
            onClick = {
                onPageSelected(currentPage - 1)
            },
            enabled = currentPage > 1
        ) {
            Text(text = "Previous")
        }

        Text(
            text = "Page $currentPage of $totalPages",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        TextButton(
            onClick = {
                onPageSelected(currentPage + 1)
            },
            enabled =
                currentPage < totalPages
        ) {
            Text(text = "Next")
        }
    }
}

@Composable
private fun quickCompareStatus(
    card: ProductCardUiState
): QuickCompareStatus {
    val item = card.item

    val liveAmazon =
        card.amazonResult
            ?.price
            ?.takeIf(::isQuickComparePrice)

    val liveFlipkart =
        card.flipkartResult
            ?.price
            ?.takeIf(::isQuickComparePrice)

    val savedAmazon =
        item.amazonLastPrice
            ?.takeIf(::isQuickComparePrice)

    val savedFlipkart =
        item.flipkartLastPrice
            ?.takeIf(::isQuickComparePrice)

    val hasLivePrice =
        liveAmazon != null ||
                liveFlipkart != null

    val amazonPrice =
        liveAmazon ?: savedAmazon

    val flipkartPrice =
        liveFlipkart ?: savedFlipkart

    val source =
        if (hasLivePrice) {
            "live"
        } else {
            "saved"
        }

    if (card.isRefreshing) {
        return QuickCompareStatus(
            label = "Checking prices",
            icon = Icons.Rounded.Schedule,
            color =
                MaterialTheme
                    .supremeColors
                    .warning
        )
    }

    if (
        amazonPrice == null &&
        flipkartPrice == null
    ) {
        return QuickCompareStatus(
            label = "Needs check",
            icon = Icons.Rounded.Search,
            color =
                MaterialTheme
                    .supremeColors
                    .warning
        )
    }

    val comparison =
        compareWithOnlinePrices(
            shopPrice = item.shopPrice,
            amazonPrice = amazonPrice,
            flipkartPrice = flipkartPrice
        )

    return when (
        comparison.shopPosition
    ) {
        ShopPricePosition.HIGHER ->
            QuickCompareStatus(
                label = "Review • $source",
                icon =
                    Icons.Rounded.PriorityHigh,
                color =
                    MaterialTheme
                        .colorScheme
                        .error
            )

        ShopPricePosition.MATCHED ->
            QuickCompareStatus(
                label = "Matched • $source",
                icon =
                    Icons.Rounded.EmojiEvents,
                color =
                    MaterialTheme
                        .supremeColors
                        .competitive
            )

        ShopPricePosition.LOWER ->
            QuickCompareStatus(
                label =
                    "Competitive • $source",
                icon =
                    Icons.Rounded.EmojiEvents,
                color =
                    MaterialTheme
                        .supremeColors
                        .competitive
            )

        ShopPricePosition.NO_ONLINE_PRICE,
        ShopPricePosition.INVALID_SHOP_PRICE ->
            QuickCompareStatus(
                label = "Needs check",
                icon = Icons.Rounded.Search,
                color =
                    MaterialTheme
                        .supremeColors
                        .warning
            )
    }
}

private fun isQuickComparePrice(
    price: Double
): Boolean =
    price.isFinite() &&
            price > 0.0
