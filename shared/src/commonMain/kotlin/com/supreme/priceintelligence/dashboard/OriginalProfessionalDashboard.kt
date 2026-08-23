package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime

import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.SortByAlpha

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.app_logo
import com.supreme.priceintelligence.ui.theme.Brand
import com.supreme.priceintelligence.ui.theme.SurfaceAlt
import com.supreme.priceintelligence.ui.theme.TextLight
import com.supreme.priceintelligence.ui.theme.TextMuted
import com.supreme.priceintelligence.ui.theme.TextPrimary
import org.jetbrains.compose.resources.painterResource

private enum class DashboardCardGlow {
    NEUTRAL,
    ALERT,
    SAFE
}

@Composable
internal fun ProfessionalDashboardBranding(
    compact: Boolean
) {
    val logoSize = if (compact) {
        28.dp
    } else {
        36.dp
    }

    val headerHeight = if (compact) {
        48.dp
    } else {
        56.dp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = "Supreme Price Intelligence logo",
            modifier = Modifier.size(logoSize),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SUPREME",
                color = Brand,
                fontSize = if (compact) {
                    20.sp
                } else {
                    24.sp
                },
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )

            if (!compact) {
                Text(
                    text = "PRICE INTELLIGENCE",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
internal fun ProfessionalDashboardResultsRow(
    state: DashboardUiState,
    advancedModeEnabled: Boolean,
    sortMenuOpen: Boolean,
    onSortMenuChanged: (Boolean) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onRefreshVisible: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Text(
            text = when {
                state.isLoading -> "Loading products"
                state.totalMatchCount == 1 ->
                    "About 1 result (${state.searchDurationMs}ms)"

                else ->
                    "About ${state.totalMatchCount} results (${state.searchDurationMs}ms)"
            },
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRODUCTS",
                color = Brand,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {


                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSortMenuChanged(true)
                            }
                            .padding(
                                horizontal = 8.dp,
                                vertical = 7.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (state.sortOrder) {
                                SortOrder.MOST_VIEWED ->
                                    Icons.AutoMirrored.Rounded.TrendingUp

                                SortOrder.ALPHABETICAL ->
                                    Icons.Rounded.SortByAlpha

                                SortOrder.RECENT ->
                                    Icons.Rounded.Schedule

                                SortOrder.BEST_SAVING ->
                                    Icons.Rounded.Savings
                            },
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = professionalSortName(
                                state.sortOrder
                            ),
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = {
                            onSortMenuChanged(false)
                        },
                        containerColor = Color(0xFF14181D)
                    ) {
                        val availableOrders = if (advancedModeEnabled) {
                            SortOrder.entries
                        } else {
                            SortOrder.entries.filterNot { order ->
                                order == SortOrder.BEST_SAVING
                            }
                        }

                        availableOrders.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = professionalSortName(order),
                                        color = if (
                                            state.sortOrder == order
                                        ) {
                                            Brand
                                        } else {
                                            TextPrimary
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (order) {
                                            SortOrder.MOST_VIEWED ->
                                                Icons.AutoMirrored.Rounded.TrendingUp

                                            SortOrder.ALPHABETICAL ->
                                                Icons.Rounded.SortByAlpha

                                            SortOrder.RECENT ->
                                                Icons.Rounded.Schedule

                                            SortOrder.BEST_SAVING ->
                                                Icons.Rounded.Savings
                                        },
                                        contentDescription = null,
                                        tint = if (
                                            state.sortOrder == order
                                        ) {
                                            Brand
                                        } else {
                                            TextMuted
                                        }
                                    )
                                },
                                onClick = {
                                    onSortMenuChanged(false)
                                    onSortSelected(order)
                                },
                                trailingIcon = {
                                    if (state.sortOrder == order) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Brand,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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
internal fun ProfessionalDashboardProductCard(
    card: ProductCardUiState,
    onClick: () -> Unit
) {
    val item = card.item

    val liveAmazonPrice = card.amazonResult?.price
        ?.takeIf(::isUsableDashboardPrice)

    val liveFlipkartPrice = card.flipkartResult?.price
        ?.takeIf(::isUsableDashboardPrice)

    val hasLivePrice =
        liveAmazonPrice != null ||
                liveFlipkartPrice != null

    val liveComparison = compareWithOnlinePrices(
        shopPrice = item.shopPrice,
        amazonPrice = liveAmazonPrice,
        flipkartPrice = liveFlipkartPrice
    )

    val glow = when {
        card.isRefreshing || !hasLivePrice ->
            DashboardCardGlow.NEUTRAL

        liveComparison.shopPosition ==
                ShopPricePosition.HIGHER ->
            DashboardCardGlow.ALERT

        else ->
            DashboardCardGlow.SAFE
    }

    val cardStateDescription = when {
        card.isRefreshing ->
            "Checking online prices"

        !hasLivePrice ->
            "Online prices not checked"

        liveComparison.shopPosition ==
                ShopPricePosition.HIGHER ->
            "Online price is lower than shop price"

        liveComparison.shopPosition ==
                ShopPricePosition.MATCHED ->
            "Online price matches shop price"

        else ->
            "Shop price is lower than online price"
    }

    val primaryGlow by animateColorAsState(
        targetValue = when (glow) {
            DashboardCardGlow.NEUTRAL ->
                Color.Transparent

            DashboardCardGlow.ALERT ->
                Color(0xFFE11D48)

            DashboardCardGlow.SAFE ->
                Color(0xFF10B981)
        },
        animationSpec = tween(durationMillis = 500),
        label = "dashboardCardPrimaryGlow"
    )

    val secondaryGlow by animateColorAsState(
        targetValue = when (glow) {
            DashboardCardGlow.NEUTRAL ->
                Color.Transparent

            DashboardCardGlow.ALERT ->
                Color(0xFFEF4444)

            DashboardCardGlow.SAFE ->
                Color(0xFF34D399)
        },
        animationSpec = tween(durationMillis = 500),
        label = "dashboardCardSecondaryGlow"
    )

    val borderColor = when (glow) {
        DashboardCardGlow.NEUTRAL ->
            Color.White.copy(alpha = 0.08f)

        DashboardCardGlow.ALERT ->
            Color(0xFFE11D48).copy(alpha = 0.50f)

        DashboardCardGlow.SAFE ->
            Color(0xFF10B981).copy(alpha = 0.50f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (glow != DashboardCardGlow.NEUTRAL) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryGlow.copy(alpha = 0.45f),
                                secondaryGlow.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick)
                .semantics {
                    stateDescription = cardStateDescription
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.productName
                        .trim()
                        .firstOrNull()
                        ?.uppercase()
                        ?: "P",
                    color = Color(0xFF475569),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.productName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.productName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = Brand.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sell,
                        contentDescription = null,
                        tint = Brand,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Supreme Price: ${
                            formatIndianPrice(item.shopPrice)
                        }",
                        color = Brand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun ProfessionalDashboardSearchOverlay(
    query: String,
    suggestions: List<String>,
    isFocused: Boolean,
    bottomBannerHeight: Dp,
    additionalBannerHeight: Dp,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onScanClick: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onDismissFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardBottom =
        WindowInsets.ime
            .asPaddingValues()
            .calculateBottomPadding()

    val systemNavigationBottom =
        WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()

    val keyboardClearance = (
        keyboardBottom - systemNavigationBottom
    ).coerceAtLeast(0.dp)

    val keyboardVisible = keyboardClearance > 0.dp

    val bannerClearanceGap =
        if (bottomBannerHeight > 0.dp) 20.dp else 0.dp

    val restingSearchBarBottom =
        96.dp +
                bottomBannerHeight +
                bannerClearanceGap +
                additionalBannerHeight

    val keyboardSearchBarBottom =
        keyboardClearance +
                8.dp +
                bottomBannerHeight +
                additionalBannerHeight

    val searchBarBottom = max(
        restingSearchBarBottom,
        keyboardSearchBarBottom
    )

    var keyboardWasVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(keyboardVisible, isFocused) {
        when {
            keyboardVisible -> {
                keyboardWasVisible = true
            }

            keyboardWasVisible && isFocused -> {
                keyboardWasVisible = false
                onDismissFocus()
            }

            !isFocused -> {
                keyboardWasVisible = false
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (isFocused) {
            // A blank, focused search box (before typing) still gets the
            // heavy curtain — there's nothing behind it to show yet. Once
            // there's a query, lighten it so the already-filtered product
            // list is visible through it, instead of hidden until you
            // dismiss the search box.
            val scrimAlpha = if (query.isBlank()) 0.82f else 0.35f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(onClick = onDismissFocus)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xFF0B0F14)
                                    .copy(alpha = 0.10f),
                                Color(0xFF0B0F14)
                                    .copy(alpha = 0.58f),
                                Color(0xFF0B0F14)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = searchBarBottom
                )
        ) {
            if (
                isFocused &&
                suggestions.isNotEmpty() &&
                query.isNotBlank()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .heightIn(max = 220.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF14181D).copy(alpha = 0.98f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = SurfaceAlt
                    )
                ) {
                    LazyColumn(
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                vertical = 4.dp
                            )
                    ) {
                        items(suggestions) { suggestion ->
                            Text(
                                text = suggestion,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSubmit(suggestion)
                                    }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    )
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F1216))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF1F252B),
                        shape = RoundedCornerShape(12.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search products",
                    tint = Brand,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(20.dp)
                )

                Spacer(modifier = Modifier.width(7.dp))

                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = "Search...",
                            color = TextLight
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onFocusChanged { focusState ->
                            onFocusChange(focusState.isFocused)
                        },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onQueryChange("")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = TextMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Brand
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) {
                                onSubmit(query)
                            }
                        }
                    )
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(SurfaceAlt)
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(56.dp)
                        .clip(
                            RoundedCornerShape(
                                topEnd = 12.dp,
                                bottomEnd = 12.dp
                            )
                        )
                        .clickable(onClick = onScanClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Scan barcode",
                        tint = Brand,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun professionalSortName(order: SortOrder): String = when (order) {
    SortOrder.MOST_VIEWED -> "Most Viewed"
    SortOrder.BEST_SAVING -> "Best Saving"
    SortOrder.ALPHABETICAL -> "A–Z"
    SortOrder.RECENT -> "Recently Added"
}

private fun isUsableDashboardPrice(price: Double): Boolean {
    return price.isFinite() && price > 0.0
}