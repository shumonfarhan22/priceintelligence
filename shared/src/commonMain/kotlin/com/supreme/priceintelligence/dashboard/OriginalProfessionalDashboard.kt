package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.SortByAlpha

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.app_logo
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

private enum class DashboardCardGlow {
    NEUTRAL,
    ALERT,
    SAFE
}

private val Brand: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

private val SurfaceAlt: Color
    @Composable
    get() = MaterialTheme.supremeColors.border

private val TextPrimary: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

private val TextMuted: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

private val TextLight: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (MaterialTheme.supremeColors.isDark) {
            0.72f
        } else {
            0.88f
        }
    )

@Composable
internal fun ProfessionalDashboardBranding(
    compact: Boolean,
    showPreviousPage: Boolean = false,
    onPreviousPage: () -> Unit = {}
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
        if (showPreviousPage) {
            IconButton(
                onClick = onPreviousPage,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription =
                        "Go to previous Dashboard page",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

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
                        containerColor =
                            MaterialTheme.supremeColors.panelStrong
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
    showResultLight: Boolean,
    onClick: () -> Unit
) {
    val item = card.item

    val liveAmazonPrice = card.amazonResult?.price
        ?.takeIf(::isUsableDashboardPrice)

    val liveFlipkartPrice = card.flipkartResult?.price
        ?.takeIf(::isUsableDashboardPrice)

    val savedAmazonPrice = item.amazonLastPrice
        ?.takeIf(::isUsableDashboardPrice)

    val savedFlipkartPrice = item.flipkartLastPrice
        ?.takeIf(::isUsableDashboardPrice)

    val hasLivePrice =
        liveAmazonPrice != null ||
                liveFlipkartPrice != null

    val amazonPrice =
        liveAmazonPrice ?: savedAmazonPrice

    val flipkartPrice =
        liveFlipkartPrice ?: savedFlipkartPrice

    val hasComparisonPrice =
        amazonPrice != null || flipkartPrice != null

    val comparison = compareWithOnlinePrices(
        shopPrice = item.shopPrice,
        amazonPrice = amazonPrice,
        flipkartPrice = flipkartPrice
    )

    val priceSourceLabel = if (hasLivePrice) {
        "live"
    } else {
        "saved"
    }

    val glow = when {
        card.isRefreshing || !hasComparisonPrice ->
            DashboardCardGlow.NEUTRAL

        comparison.shopPosition ==
                ShopPricePosition.HIGHER ->
            DashboardCardGlow.ALERT

        else ->
            DashboardCardGlow.SAFE
    }

    val cardStateDescription = when {
        card.isRefreshing ->
            "Checking online prices"

        !hasComparisonPrice ->
            "Online prices need checking"

        comparison.shopPosition ==
                ShopPricePosition.HIGHER ->
            "$priceSourceLabel online price is lower than shop price"

        comparison.shopPosition ==
                ShopPricePosition.MATCHED ->
            "$priceSourceLabel online price matches shop price"

        else ->
            "Shop price is lower than $priceSourceLabel online price"
    }

    val statusIcon = when {
        card.isRefreshing ->
            Icons.Rounded.Schedule

        !hasComparisonPrice ->
            Icons.Rounded.Search

        glow == DashboardCardGlow.ALERT ->
            Icons.Rounded.PriorityHigh

        else ->
            Icons.Rounded.EmojiEvents
    }

    val statusColor = when (glow) {
        DashboardCardGlow.NEUTRAL ->
            MaterialTheme.supremeColors.warning

        DashboardCardGlow.ALERT ->
            MaterialTheme.colorScheme.error

        DashboardCardGlow.SAFE ->
            MaterialTheme.supremeColors.competitive
    }

    val primaryGlow by animateColorAsState(
        targetValue = when (glow) {
            DashboardCardGlow.NEUTRAL ->
                Color.Transparent

            DashboardCardGlow.ALERT ->
                MaterialTheme.colorScheme.error

            DashboardCardGlow.SAFE ->
                MaterialTheme.supremeColors.competitive
        },
        animationSpec = tween(durationMillis = 500),
        label = "dashboardCardPrimaryGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.supremeColors.panel)
                .drawBehind {
                    if (
                        showResultLight &&
                        glow != DashboardCardGlow.NEUTRAL
                    ) {
                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    colors = listOf(
                                        primaryGlow.copy(
                                            alpha = 0.32f
                                        ),
                                        primaryGlow.copy(
                                            alpha = 0.16f
                                        ),
                                        primaryGlow.copy(
                                            alpha = 0.06f
                                        ),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY =
                                        size.height * 0.68f
                                )
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    color =
                        MaterialTheme.supremeColors.border,
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
                    .background(
                        MaterialTheme.supremeColors.imagePanel
                    ),
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = Brand.copy(
                                    alpha =
                                        if (
                                            MaterialTheme.supremeColors.isDark
                                        ) {
                                            0.30f
                                        } else {
                                            0.48f
                                        }
                                ),
                                shape =
                                    RoundedCornerShape(8.dp)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
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
                                formatIndianPrice(
                                    item.shopPrice
                                )
                            }",
                            color =
                                if (
                                    MaterialTheme.supremeColors.isDark
                                ) {
                                    Brand
                                } else {
                                    TextPrimary
                                },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(7.dp))

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                statusColor.copy(
                                    alpha = 0.16f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription =
                                cardStateDescription,
                            tint = statusColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
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
    reduceMotionEnabled: Boolean,
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

    val focusManager =
        LocalFocusManager.current

    val keyboardController =
        LocalSoftwareKeyboardController.current

    val bannerClearanceGap =
        if (bottomBannerHeight > 0.dp) 20.dp else 0.dp

    val restingSearchBarBottom =
        96.dp +
                bottomBannerHeight +
                bannerClearanceGap +
                additionalBannerHeight

    val searchBarBottom =
        if (isFocused || keyboardVisible) {
            maxOf(
                restingSearchBarBottom,
                keyboardClearance + 8.dp
            )
        } else {
            restingSearchBarBottom
        }

    val focusScrimAlpha by animateFloatAsState(
        targetValue = if (isFocused) {
            if (query.isBlank()) {
                0.82f
            } else {
                0.35f
            }
        } else {
            0f
        },
        animationSpec = if (reduceMotionEnabled) {
            snap()
        } else {
            tween(durationMillis = 160)
        },
        label = "dashboardSearchScrim"
    )

    val searchFocusRequester = remember {
        FocusRequester()
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            if (!reduceMotionEnabled) {
                delay(90)
            }

            searchFocusRequester.requestFocus()
        }
    }

    var keyboardWasVisible by remember {
        mutableStateOf(false)
    }

    var keyboardDismissRequested by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        keyboardVisible,
        isFocused,
        keyboardDismissRequested
    ) {
        when {
            keyboardVisible -> {
                keyboardWasVisible = true
            }

            keyboardDismissRequested &&
                !keyboardVisible &&
                isFocused -> {
                keyboardDismissRequested = false
                keyboardWasVisible = false
                onDismissFocus()
            }

            keyboardWasVisible &&
                !keyboardVisible &&
                isFocused -> {
                keyboardWasVisible = false
                onDismissFocus()
            }

            !isFocused -> {
                keyboardDismissRequested = false
                keyboardWasVisible = false
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = !isFocused,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = restingSearchBarBottom
                ),
            enter = if (reduceMotionEnabled) {
                fadeIn(
                    animationSpec = tween(durationMillis = 0)
                )
            } else {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 70,
                        delayMillis = 40
                    )
                ) + scaleIn(
                    animationSpec = tween(durationMillis = 110),
                    initialScale = 0.82f
                )
            },
            exit = if (reduceMotionEnabled) {
                fadeOut(
                    animationSpec = tween(durationMillis = 0)
                )
            } else {
                fadeOut(
                    animationSpec = tween(durationMillis = 70)
                ) + scaleOut(
                    animationSpec = tween(durationMillis = 110),
                    targetScale = 0.82f
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation =
                            if (
                                MaterialTheme.supremeColors.isDark
                            ) {
                                0.dp
                            } else {
                                4.dp
                            },
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.supremeColors.border,
                        shape = CircleShape
                    )
                    .clickable {
                        onFocusChange(true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Open product search",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        if (focusScrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha = focusScrimAlpha
                        )
                    )
                    .clickable(
                        enabled = isFocused,
                        interactionSource =
                            remember {
                                MutableInteractionSource()
                            },
                        indication = null,
                        onClick = {
                            keyboardDismissRequested =
                                true

                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
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
            AnimatedVisibility(
                visible =
                    isFocused &&
                        keyboardVisible &&
                        suggestions.isNotEmpty(),
                enter = if (reduceMotionEnabled) {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 0
                        )
                    )
                } else {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 150
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 190
                        ),
                        initialOffsetY = { height ->
                            height / 5
                        }
                    )
                },
                exit = if (reduceMotionEnabled) {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 0
                        )
                    )
                } else {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 120
                        )
                    ) + slideOutVertically(
                        animationSpec = tween(
                            durationMillis = 150
                        ),
                        targetOffsetY = { height ->
                            height / 5
                        }
                    )
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .heightIn(max = 260.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.supremeColors.panelStrong,
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
                        item(
                            contentType =
                                "dashboard-search-suggestion-heading"
                        ) {
                            Text(
                                text =
                                    if (query.isBlank()) {
                                        "Popular inventory products"
                                    } else {
                                        "Suggested inventory products"
                                    },
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 4.dp
                                )
                            )
                        }

                        items(
                            items = suggestions,
                            key = { suggestion ->
                                suggestion.lowercase()
                            },
                            contentType = {
                                "dashboard-search-suggestion"
                            }
                        ) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSubmit(suggestion)
                                    }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 10.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = Brand,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(
                                    modifier = Modifier.width(12.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = suggestion,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight =
                                            FontWeight.Medium,
                                        maxLines = 1,
                                        overflow =
                                            TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "Tap to search inventory",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isFocused,
                modifier = Modifier.align(Alignment.End),
                enter = if (reduceMotionEnabled) {
                    fadeIn(
                        animationSpec = tween(durationMillis = 0)
                    )
                } else {
                    fadeIn(
                        animationSpec = tween(durationMillis = 90)
                    ) + expandHorizontally(
                        animationSpec = tween(durationMillis = 180),
                        expandFrom = Alignment.End
                    )
                },
                exit = if (reduceMotionEnabled) {
                    fadeOut(
                        animationSpec = tween(durationMillis = 0)
                    )
                } else {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 90,
                            delayMillis = 90
                        )
                    ) + shrinkHorizontally(
                        animationSpec = tween(durationMillis = 180),
                        shrinkTowards = Alignment.End
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation =
                                if (
                                    MaterialTheme.supremeColors.isDark
                                ) {
                                    0.dp
                                } else {
                                    3.dp
                                },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.supremeColors.field)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.supremeColors.border,
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
                        .focusRequester(searchFocusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onFocusChange(true)
                            }
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