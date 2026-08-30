@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.platform.LocalDensity
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
import com.supreme.priceintelligence.settings.PriceEmphasis
import com.supreme.priceintelligence.ui.layout.adaptiveLayoutPolicy
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.app_logo
import com.supreme.priceintelligence.ui.input.dismissPlatformKeyboard
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import com.supreme.priceintelligence.ui.input.withPlatformTextInput
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
    previousPageContentDescription: String =
        "Go back",
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
            .heightIn(min = headerHeight),
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
                        previousPageContentDescription,
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
            modifier = Modifier.weight(1f),
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
    compact: Boolean = false,
    priceFocused: Boolean = false,
    priceEmphasis: PriceEmphasis =
        PriceEmphasis.NORMAL,
    onClick: () -> Unit
) {
    val item = card.item
    val outsidePadding = if (compact) 5.dp else 8.dp
    val contentPadding = if (compact) 10.dp else 14.dp
    val imageSize = when {
        compact -> 82.dp
        priceFocused -> 98.dp
        else -> 108.dp
    }
    val imagePadding = if (compact) 7.dp else 10.dp
    val contentGap = if (compact) 11.dp else 15.dp
    val titlePriceGap = if (compact) 8.dp else 12.dp
    val titleSize = if (compact || priceFocused) 14.sp else 15.sp
    val priceSize = when {
        priceFocused -> 13.sp
        compact -> 11.sp
        else -> 12.sp
    }
    val priceWeight =
        if (
            priceFocused ||
            priceEmphasis == PriceEmphasis.BOLD
        ) {
            FontWeight.ExtraBold
        } else {
            FontWeight.Medium
        }
    val statusSize = if (compact) 26.dp else 28.dp

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = outsidePadding),
        contentAlignment = Alignment.Center
    ) {
        val layoutPolicy = adaptiveLayoutPolicy(
            availableWidthDp = maxWidth.value,
            fontScale = LocalDensity.current.fontScale
        )

        // A larger font does not automatically mean that the phone has a
        // narrow display. Only use the narrow layout when the available
        // card width is genuinely small.
        val constrainedLayout =
            maxWidth < 330.dp

        val resolvedImageSize =
            when {
                constrainedLayout ->
                    if (compact) {
                        60.dp
                    } else {
                        68.dp
                    }

                layoutPolicy.isLargeText ->
                    when {
                        compact -> 76.dp
                        priceFocused -> 88.dp
                        else -> 92.dp
                    }

                else -> imageSize
            }

        val resolvedContentGap =
            if (constrainedLayout) {
                8.dp
            } else {
                contentGap
            }

        val productTitleMaxLines =
            if (layoutPolicy.isLargeText) {
                3
            } else {
                2
            }

        val formattedShopPrice =
            formatIndianPrice(item.shopPrice)

        val priceTextColor =
            MaterialTheme.colorScheme.primary

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        if (compact) {
                            112.dp
                        } else {
                            138.dp
                        }
                )
                .clip(RoundedCornerShape(22.dp))
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
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable(onClick = onClick)
                .semantics {
                    stateDescription = cardStateDescription
                }
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(resolvedImageSize)
                    .clip(RoundedCornerShape(16.dp))
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
                            .padding(imagePadding),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(
                    resolvedContentGap
                )
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.productName,
                    color = TextPrimary,
                    fontSize = titleSize,
                    lineHeight =
                        if (compact || priceFocused) {
                            18.sp
                        } else {
                            20.sp
                        },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = productTitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(titlePriceGap))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .primary
                                    .copy(
                                        alpha =
                                            if (
                                                MaterialTheme
                                                    .supremeColors
                                                    .isDark
                                            ) {
                                                0.09f
                                            } else {
                                                0.07f
                                            }
                                    )
                            )
                            .padding(
                                horizontal =
                                    if (compact) 8.dp else 10.dp,
                                vertical =
                                    if (compact) 5.dp else 6.dp
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
                            text =
                                "PRICE • $formattedShopPrice",
                            color = priceTextColor,
                            fontSize = priceSize,
                            fontWeight = priceWeight,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(7.dp))

                    Box(
                        modifier = Modifier
                            .size(statusSize)
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
                            modifier = Modifier.size(
                                if (compact) 14.dp else 15.dp
                            )
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
    searchState: TextFieldState,
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
    scrimFollowsSuggestions: Boolean = false,
    morphSearchButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    val query = searchState.text.toString()
    val currentOnQueryChange by
        rememberUpdatedState(onQueryChange)

    LaunchedEffect(searchState) {
        snapshotFlow {
            searchState.text.toString()
        }
            .distinctUntilChanged()
            .collectLatest { changedQuery ->
                if (changedQuery.isNotBlank()) {
                    delay(80L)
                }

                currentOnQueryChange(changedQuery)
            }
    }

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
        20.dp +
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
        targetValue = if (
            isFocused &&
            (!scrimFollowsSuggestions || suggestions.isNotEmpty())
        ) {
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
                delay(
                    if (morphSearchButton) {
                        135
                    } else {
                        90
                    }
                )
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

    val submitSearch: (String) -> Unit = { submittedQuery ->
        if (morphSearchButton) {
            keyboardDismissRequested = true
        }

        onSubmit(submittedQuery)

        if (morphSearchButton) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            dismissPlatformKeyboard()

            if (!keyboardVisible) {
                keyboardDismissRequested = false
                keyboardWasVisible = false
                onDismissFocus()
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible =
                !morphSearchButton &&
                    !isFocused,
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

        if (isFocused) {
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
                                        submitSearch(suggestion)
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

            if (morphSearchButton) {
                MorphingQuickCompareSearchBar(
                    isFocused = isFocused,
                    searchState = searchState,
                    reduceMotionEnabled =
                        reduceMotionEnabled,
                    searchFocusRequester =
                        searchFocusRequester,
                    onSubmit = submitSearch,
                    onScanClick = onScanClick,
                    onOpen = {
                        onFocusChange(true)
                    }
                )
            } else {
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
                    state = searchState,
                    placeholder = {
                        Text(
                            text = "Search...",
                            color = TextLight
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(searchFocusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onFocusChange(true)
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
                    ).withPlatformTextInput(),
                    onKeyboardAction = {
                        if (query.isNotBlank()) {
                            submitSearch(query)
                        }
                    },
                    lineLimits = TextFieldLineLimits.SingleLine
                )

                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = searchState::clearText
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = TextMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

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
}

@Composable
private fun MorphingQuickCompareSearchBar(
    isFocused: Boolean,
    searchState: TextFieldState,
    reduceMotionEnabled: Boolean,
    searchFocusRequester: FocusRequester,
    onSubmit: (String) -> Unit,
    onScanClick: () -> Unit,
    onOpen: () -> Unit
) {
    val query = searchState.text.toString()
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        val animationDuration = 125

        val containerWidth by animateDpAsState(
            targetValue =
                if (isFocused) maxWidth else 56.dp,
            animationSpec =
                if (reduceMotionEnabled) {
                    snap()
                } else {
                    tween(
                        durationMillis =
                            animationDuration
                    )
                },
            label = "quickCompareSearchWidth"
        )

        val cornerRadius by animateDpAsState(
            targetValue =
                if (isFocused) 12.dp else 28.dp,
            animationSpec =
                if (reduceMotionEnabled) {
                    snap()
                } else {
                    tween(
                        durationMillis =
                            animationDuration
                    )
                },
            label = "quickCompareSearchShape"
        )

        val containerColor by animateColorAsState(
            targetValue =
                if (isFocused) {
                    MaterialTheme.supremeColors.field
                } else {
                    MaterialTheme.colorScheme.primary
                },
            animationSpec =
                if (reduceMotionEnabled) {
                    snap()
                } else {
                    tween(durationMillis = 95)
                },
            label = "quickCompareSearchColor"
        )

        val shape = RoundedCornerShape(cornerRadius)

        Surface(
            modifier = Modifier
                .width(containerWidth)
                .height(56.dp)
                .clickable(
                    enabled = !isFocused,
                    onClick = onOpen
                ),
            shape = shape,
            color = containerColor,
            border =
                androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme
                            .supremeColors
                            .border
                ),
            shadowElevation =
                if (
                    MaterialTheme.supremeColors.isDark
                ) {
                    0.dp
                } else {
                    3.dp
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedVisibility(
                    visible = !isFocused,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(56.dp)
                        .fillMaxHeight(),
                    enter =
                        if (reduceMotionEnabled) {
                            fadeIn(
                                animationSpec =
                                    tween(0)
                            )
                        } else {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 45,
                                    delayMillis = 70
                                )
                            )
                        },
                    exit =
                        if (reduceMotionEnabled) {
                            fadeOut(
                                animationSpec =
                                    tween(0)
                            )
                        } else {
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = 35
                                )
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Rounded.Search,
                            contentDescription =
                                "Open product search",
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isFocused,
                    modifier = Modifier.fillMaxSize(),
                    enter =
                        if (reduceMotionEnabled) {
                            fadeIn(
                                animationSpec =
                                    tween(0)
                            )
                        } else {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 45,
                                    delayMillis = 70
                                )
                            )
                        },
                    exit =
                        if (reduceMotionEnabled) {
                            fadeOut(
                                animationSpec =
                                    tween(0)
                            )
                        } else {
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = 35
                                )
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector =
                                Icons.Rounded.Search,
                            contentDescription =
                                "Search products",
                            tint = Brand,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .size(20.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(7.dp)
                        )

                        TextField(
                            state = searchState,
                            placeholder = {
                                Text(
                                    text = "Search...",
                                    color = TextLight
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .focusRequester(
                                    searchFocusRequester
                                )
                                .onFocusChanged {
                                    focusState ->

                                    if (
                                        focusState.isFocused
                                    ) {
                                        onOpen()
                                    }
                                },
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor =
                                        Color.Transparent,
                                    unfocusedContainerColor =
                                        Color.Transparent,
                                    disabledContainerColor =
                                        Color.Transparent,
                                    focusedIndicatorColor =
                                        Color.Transparent,
                                    unfocusedIndicatorColor =
                                        Color.Transparent,
                                    disabledIndicatorColor =
                                        Color.Transparent,
                                    focusedTextColor =
                                        TextPrimary,
                                    unfocusedTextColor =
                                        TextPrimary,
                                    cursorColor = Brand
                                ),
                            keyboardOptions =
                                KeyboardOptions(
                                    imeAction =
                                        ImeAction.Search
                                ).withPlatformTextInput(),
                            onKeyboardAction = {
                                if (
                                    query.isNotBlank()
                                ) {
                                    onSubmit(query)
                                }
                            },
                            lineLimits =
                                TextFieldLineLimits.SingleLine
                        )

                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = searchState::clearText
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Rounded.Close,
                                        contentDescription =
                                            "Clear search",
                                        tint = TextMuted,
                                        modifier =
                                            Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

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
                                .clickable(
                                    onClick = onScanClick
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Rounded.CameraAlt,
                                contentDescription =
                                    "Scan barcode",
                                tint = Brand,
                                modifier =
                                    Modifier.size(22.dp)
                            )
                        }
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
