@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.rememberUrlOpener
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.logo_amazon
import com.supreme.priceintelligence.resources.logo_flipkart
import com.supreme.priceintelligence.scanner.ProductBarcodeScanner
import com.supreme.priceintelligence.scanner.rememberCameraPermissionRequester
import com.supreme.priceintelligence.ui.components.OriginalBannerKind
import com.supreme.priceintelligence.ui.components.OriginalStatusBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OriginalDashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    advancedModeEnabled: Boolean = false,
    bottomBannerHeight: Dp = 0.dp
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var sortMenuOpen by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    var scannerOpen by rememberSaveable { mutableStateOf(false) }
    var selectedProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var cameraPermissionDenied by rememberSaveable { mutableStateOf(false) }

    var showNetworkBanner by remember {
        mutableStateOf(false)
    }
    var networkMessage by remember {
        mutableStateOf("")
    }
    var networkBannerKind by remember {
        mutableStateOf(OriginalBannerKind.WARNING)
    }
    var measuredNetworkBannerHeight by remember {
        mutableStateOf(0.dp)
    }

    val density = LocalDensity.current

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

    val floatingBannerBottom = maxOf(
        91.dp,
        keyboardClearance + 8.dp
    )

    val networkBannerClearance by animateDpAsState(
        targetValue = if (showNetworkBanner) {
            measuredNetworkBannerHeight + 20.dp
        } else {
            0.dp
        },
        animationSpec = tween(durationMillis = 180),
        label = "networkBannerClearance"
    )

    val dashboardListState = rememberLazyListState()
    var compactDashboardHeaderVisible by rememberSaveable {
        mutableStateOf(false)
    }
    var decisionCardShouldCollapse by rememberSaveable {
        mutableStateOf(false)
    }

    val headerTransitionThresholdPx = with(density) {
        20.dp.roundToPx()
    }

    val headerHideThresholdPx = with(density) {
        24.dp.roundToPx()
    }

    LaunchedEffect(
        dashboardListState,
        headerTransitionThresholdPx,
        headerHideThresholdPx
    ) {
        var previousIndex =
            dashboardListState.firstVisibleItemIndex
        var previousOffset =
            dashboardListState.firstVisibleItemScrollOffset
        var downwardTravelPx = 0

        snapshotFlow {
            dashboardListState.firstVisibleItemIndex to
                    dashboardListState.firstVisibleItemScrollOffset
        }.collect { position ->
            val currentIndex = position.first
            val currentOffset = position.second

            val atTop =
                currentIndex == 0 &&
                        currentOffset == 0

            val scrollingUp =
                currentIndex < previousIndex ||
                        (
                                currentIndex == previousIndex &&
                                        currentOffset < previousOffset
                                )

            val scrollingDown =
                currentIndex > previousIndex ||
                        (
                                currentIndex == previousIndex &&
                                        currentOffset > previousOffset
                                )

            when {
                atTop -> {
                    compactDashboardHeaderVisible = false
                    downwardTravelPx = 0
                }

                currentIndex == 0 &&
                        currentOffset >= headerTransitionThresholdPx -> {
                    compactDashboardHeaderVisible = true
                    downwardTravelPx = 0
                }

                scrollingUp -> {
                    compactDashboardHeaderVisible = true
                    downwardTravelPx = 0
                }

                scrollingDown -> {
                    val downwardDelta = when {
                        currentIndex > previousIndex ->
                            headerHideThresholdPx

                        currentIndex == previousIndex ->
                            (currentOffset - previousOffset)
                                .coerceAtLeast(0)

                        else ->
                            0
                    }

                    downwardTravelPx += downwardDelta

                    if (
                        downwardTravelPx >=
                        headerHideThresholdPx
                    ) {
                        compactDashboardHeaderVisible = false
                        downwardTravelPx = 0
                    }
                }
            }

            previousIndex = currentIndex
            previousOffset = currentOffset
        }
    }

    // Separate from the compact-header effect above, which reacts to a tiny
    // scroll (20-24dp). This checks something much more direct: whether the
    // decision summary card's own list item is still on screen at all. Only
    // once it has scrolled fully out of view does it fold itself shut, so
    // it doesn't sit expanded — and taking up space — if you scroll back
    // near the top later.
    LaunchedEffect(dashboardListState) {
        snapshotFlow {
            dashboardListState.layoutInfo.visibleItemsInfo.any { info ->
                info.key == "advanced-summary"
            }
        }.collect { summaryCardOnScreen ->
            decisionCardShouldCollapse = !summaryCardOnScreen
        }
    }


    LaunchedEffect(advancedModeEnabled, state.sortOrder) {
        if (
            !advancedModeEnabled &&
            state.sortOrder == SortOrder.BEST_SAVING
        ) {
            viewModel.setSortOrder(SortOrder.MOST_VIEWED)
        }
    }

    LaunchedEffect(state.bloomState) {
        when (state.bloomState) {
            BloomState.ERROR -> {
                networkMessage = "No internet connection"
                networkBannerKind = OriginalBannerKind.ERROR
                showNetworkBanner = true
                delay(5000.milliseconds)
                showNetworkBanner = false
            }

            BloomState.WARNING -> {
                networkMessage = "Slow or unstable connection"
                networkBannerKind = OriginalBannerKind.WARNING
                showNetworkBanner = true
                delay(5000.milliseconds)
                showNetworkBanner = false
            }

            BloomState.SUCCESS,
            BloomState.NONE -> {
                showNetworkBanner = false
            }
        }
    }

    val permissionRequester = rememberCameraPermissionRequester { granted ->
        if (granted) {
            cameraPermissionDenied = false
            searchFocused = false
            focusManager.clearFocus()
            scannerOpen = true
        } else {
            cameraPermissionDenied = true
        }
    }

    if (scannerOpen) {
        ProductBarcodeScanner(
            modifier = Modifier.fillMaxSize(),
            onScanned = { barcode ->
                scannerOpen = false
                viewModel.onSearchSubmitted(barcode)
            },
            onError = {
                scannerOpen = false
                cameraPermissionDenied = true
            },
            onCanceled = {
                scannerOpen = false
            }
        )
        return
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = dashboardListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = 0.dp,
                    bottom = 190.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item(key = "branding") {
                    ProfessionalDashboardBranding(
                        compact = false
                    )
                }

                if (cameraPermissionDenied) {
                    item(key = "feedback") {
                        CompactDashboardFeedback(
                            message =
                                "Camera unavailable • enter the barcode manually",
                            isError = true
                        )
                    }
                }

                item(key = "results") {
                    ProfessionalDashboardResultsRow(
                        state = state,
                        advancedModeEnabled = advancedModeEnabled,
                        sortMenuOpen = sortMenuOpen,
                        onSortMenuChanged = { open ->
                            sortMenuOpen = open
                        },
                        onSortSelected = viewModel::setSortOrder,
                        onRefreshVisible = viewModel::refreshVisiblePrices
                    )
                }

                if (advancedModeEnabled && state.searchQuery.isBlank() && state.allMatchingItems.isNotEmpty()) {
                    item(key = "advanced-summary") {
                        DashboardDecisionSummaryCard(
                            summary = state.allMatchingItems.buildDecisionSummary(state.pageItems),
                            collapseSignal = decisionCardShouldCollapse,
                            refreshTick = state.refreshCollapseTick,
                            activeFilter = state.priceFilter,
                            onFilterToggle = viewModel::setPriceFilter
                        )
                    }
                }

                when {
                    state.isLoading && state.pageItems.isEmpty() -> {
                        item(key = "loading") {
                            DashboardLoadingState()
                        }
                    }

                    state.totalMatchCount == 0 -> {
                        item(key = "empty") {
                            DashboardEmptyState(
                                isSearching = state.searchQuery.isNotBlank() || state.priceFilter != null
                            )
                        }
                    }

                    else -> {
                        items(
                            items = state.pageItems,
                            key = { card -> card.item.id },
                            contentType = {
                                "dashboard-product-card"
                            }
                        ) { card ->
                            ProfessionalDashboardProductCard(
                                card = card,
                                onClick = {
                                    viewModel.recordProductViewed(card.item.id)
                                    selectedProductId = card.item.id
                                    searchFocused = false
                                    focusManager.clearFocus()

                                    if (advancedModeEnabled) {
                                        viewModel.loadPriceHistory(card.item.id)
                                    }
                                }
                            )
                        }

                        if (state.totalPages > 1) {
                            item(key = "pagination") {
                                OriginalPagination(
                                    currentPage = state.currentPage,
                                    totalPages = state.totalPages,
                                    onPrevious = {
                                        viewModel.goToPage(state.currentPage - 1)
                                    },
                                    onNext = {
                                        viewModel.goToPage(state.currentPage + 1)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = compactDashboardHeaderVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(
                animationSpec = tween(durationMillis = 120)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 90)
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0B0F14).copy(alpha = 0.98f),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    ProfessionalDashboardBranding(
                        compact = true
                    )
                }
            }
        }

        ProfessionalDashboardSearchOverlay(
            query = state.searchQuery,
            suggestions = state.suggestions,
            isFocused = searchFocused,
            bottomBannerHeight = bottomBannerHeight,
            additionalBannerHeight = networkBannerClearance,
            onQueryChange = { query ->
                viewModel.onSearchQueryChanged(query)

                if (query.isBlank()) {
                    viewModel.onSearchSubmitted("")
                }
            },
            onSubmit = { query ->
                viewModel.onSearchSubmitted(query)
                searchFocused = false
                focusManager.clearFocus()
            },
            onScanClick = permissionRequester::requestPermission,
            onFocusChange = { focused ->
                searchFocused = focused
            },
            onDismissFocus = {
                searchFocused = false
                focusManager.clearFocus()
            }
        )

        AnimatedVisibility(
            visible = showNetworkBanner,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom =
                        floatingBannerBottom +
                            bottomBannerHeight
                ),
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { height ->
                    height / 2
                }
            ),
            exit = fadeOut() + slideOutVertically(
                targetOffsetY = { height ->
                    height / 2
                }
            )
        ) {
            OriginalStatusBanner(
                message = networkMessage,
                kind = networkBannerKind,
                onDismiss = {
                    showNetworkBanner = false
                },
                modifier = Modifier.onSizeChanged { size ->
                    measuredNetworkBannerHeight =
                        with(density) {
                            size.height.toDp()
                        }
                },
                horizontalPadding = 16.dp
            )
        }
    }

    val selectedCard = state.pageItems.firstOrNull { card ->
        card.item.id == selectedProductId
    }

    if (selectedCard != null) {
        OriginalProfessionalProductDetailDialog(
            card = selectedCard,
            networkState = state.bloomState,
            advancedModeEnabled = advancedModeEnabled,
            isHistoryLoading =
                selectedCard.item.id in state.historyLoadingProductIds,
            onRefresh = {
                viewModel.refreshProduct(selectedCard.item.id)
            },
            onDismiss = {
                selectedProductId = null
            },
            priceHistory = state.priceHistoryByProduct[
                selectedCard.item.id
            ].orEmpty()
        )
    }
}

@Composable
private fun DashboardResultsRow(
    state: DashboardUiState,
    advancedModeEnabled: Boolean,
    sortMenuOpen: Boolean,
    onSortMenuChanged: (Boolean) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onRefreshVisible: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when {
                        state.isLoading -> "Loading products"
                        state.totalMatchCount == 1 ->
                            "About 1 result (${state.searchDurationMs}ms)"

                        else ->
                            "About ${state.totalMatchCount} results (${state.searchDurationMs}ms)"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Text(
                    text = "PRODUCTS",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }

            if (advancedModeEnabled) {
                IconButton(
                    onClick = onRefreshVisible,
                    enabled = state.isConnected && !state.isRefreshingPage
                ) {
                    if (state.isRefreshingPage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Check all visible prices"
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = {
                        onSortMenuChanged(true)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sort,
                        contentDescription = "Sort products",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = {
                        onSortMenuChanged(false)
                    }
                ) {
                    val sortOptions = if (advancedModeEnabled) {
                        SortOrder.entries
                    } else {
                        SortOrder.entries.filterNot { order ->
                            order == SortOrder.BEST_SAVING
                        }
                    }

                    sortOptions.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(originalSortName(order))
                            },
                            onClick = {
                                onSortMenuChanged(false)
                                onSortSelected(order)
                            },
                            trailingIcon = {
                                if (state.sortOrder == order) {
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
        )
    }
}

@Composable
private fun CompactDashboardFeedback(
    message: String,
    isError: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    Color(0xFF3C2A08)
                }
            )
            .semantics {
                liveRegion = LiveRegionMode.Polite
            }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.WifiOff,
            contentDescription = null,
            tint = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                Color(0xFFF59E0B)
            },
            modifier = Modifier.size(17.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DashboardLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun DashboardEmptyState(
    isSearching: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(38.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isSearching) {
                "No matching products"
            } else {
                "No products yet"
            },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isSearching) {
                "Try another name or barcode"
            } else {
                "Add products from Inventory"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun OriginalProductCard(
    card: ProductCardUiState,
    onClick: () -> Unit
) {
    val item = card.item
    val amazonPrice = card.amazonResult?.price ?: item.amazonLastPrice
    val flipkartPrice = card.flipkartResult?.price ?: item.flipkartLastPrice
    val comparison = compareWithOnlinePrices(
        shopPrice = item.shopPrice,
        amazonPrice = amazonPrice,
        flipkartPrice = flipkartPrice
    )

    val hasLivePrice =
        card.amazonResult?.price != null ||
                card.flipkartResult?.price != null

    val hasSavedPrice =
        amazonPrice != null ||
                flipkartPrice != null

    val statusText = when {
        card.isRefreshing -> "CHECKING"

        hasLivePrice &&
                comparison.shopPosition == ShopPricePosition.HIGHER ->
            "LIVE • ONLINE LOWER"

        hasLivePrice &&
                comparison.shopPosition == ShopPricePosition.MATCHED ->
            "LIVE • MATCHED"

        hasLivePrice -> "LIVE • SHOP LOWER"

        hasSavedPrice &&
                comparison.shopPosition == ShopPricePosition.HIGHER ->
            "SAVED • ONLINE LOWER"

        hasSavedPrice -> "SAVED COMPARISON"

        else -> "NOT CHECKED"
    }

    val statusColor = when {
        comparison.shopPosition == ShopPricePosition.HIGHER ->
            MaterialTheme.colorScheme.error

        comparison.shopPosition == ShopPricePosition.LOWER ||
                comparison.shopPosition == ShopPricePosition.MATCHED ->
            MaterialTheme.colorScheme.primary

        else ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                stateDescription = statusText
            },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                comparison.shopPosition == ShopPricePosition.HIGHER ->
                    MaterialTheme.colorScheme.error.copy(alpha = 0.75f)

                hasSavedPrice ->
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)

                else ->
                    MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 116.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactProductImage(
                imageUrl = item.imageUrl,
                productName = item.productName,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.productName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.70f
                        )
                    )
                ) {
                    Text(
                        text = "SUPREME • ${formatIndianPrice(item.shopPrice)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Open product details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CompactProductImage(
    imageUrl: String?,
    productName: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
            .clickable(
                enabled = onClick != null,
                onClick = {
                    onClick?.invoke()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = productName.trim().firstOrNull()?.uppercase() ?: "P",
            color = Color(0xFF475569),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun OriginalPagination(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = currentPage > 1
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Previous page"
            )
        }

        Text(
            text = "$currentPage / $totalPages",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        IconButton(
            onClick = onNext,
            enabled = currentPage < totalPages
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Next page"
            )
        }
    }
}

@Composable
private fun OriginalProductDetailDialog(
    card: ProductCardUiState,
    advancedModeEnabled: Boolean,
    isHistoryLoading: Boolean,
    priceHistory: List<com.supreme.priceintelligence.data.PriceHistoryEntry>,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val item = card.item
    val openUrl = rememberUrlOpener()
    var imageViewerOpen by rememberSaveable { mutableStateOf(false) }

    val amazonPrice = card.amazonResult?.price ?: item.amazonLastPrice
    val flipkartPrice = card.flipkartResult?.price ?: item.flipkartLastPrice

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF10151B),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Product details",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close product details"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            CompactProductImage(
                                imageUrl = item.imageUrl,
                                productName = item.productName,
                                onClick = {
                                    imageViewerOpen = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.productName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = formatIndianPrice(item.shopPrice),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetailerBentoCard(
                            retailerName = "Amazon",
                            logo = Res.drawable.logo_amazon,
                            shopPrice = item.shopPrice,
                            price = amazonPrice,
                            isLive = card.amazonResult?.price != null,
                            modifier = Modifier.weight(1f)
                        )

                        RetailerBentoCard(
                            retailerName = "Flipkart",
                            logo = Res.drawable.logo_flipkart,
                            shopPrice = item.shopPrice,
                            price = flipkartPrice,
                            isLive = card.flipkartResult?.price != null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.amazonUrl
                        ?.takeIf { url -> url.isNotBlank() }
                        ?.let { url ->
                            RetailerLinkButton(
                                label = "Amazon",
                                logo = Res.drawable.logo_amazon,
                                onClick = {
                                    openUrl(url)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                    item.flipkartUrl
                        ?.takeIf { url -> url.isNotBlank() }
                        ?.let { url ->
                            RetailerLinkButton(
                                label = "Flipkart",
                                logo = Res.drawable.logo_flipkart,
                                onClick = {
                                    openUrl(url)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                }

                Spacer(modifier = Modifier.height(9.dp))

                Button(
                    onClick = onRefresh,
                    enabled = !card.isRefreshing &&
                            (
                                    !item.amazonUrl.isNullOrBlank() ||
                                            !item.flipkartUrl.isNullOrBlank()
                                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (card.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (card.isRefreshing) {
                            "Checking prices"
                        } else {
                            "Refresh prices"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (advancedModeEnabled) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Advanced price information",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PriceHistorySection(
                        entries = priceHistory,
                        isLoading = isHistoryLoading,
                        shopPrice = item.shopPrice
                    )
                }
            }
        }
    }

    if (imageViewerOpen) {
        Dialog(
            onDismissRequest = {
                imageViewerOpen = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable {
                        imageViewerOpen = false
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.productName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                IconButton(
                    onClick = {
                        imageViewerOpen = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(18.dp)
                        .background(
                            Color.Black.copy(alpha = 0.55f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close image",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RetailerBentoCard(
    retailerName: String,
    logo: DrawableResource,
    shopPrice: Double,
    price: Double?,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val validPrice = price?.takeIf { value ->
        value.isFinite() && value > 0.0
    }

    val difference = validPrice?.let { value ->
        shopPrice - value
    }

    val comparisonText = when {
        difference == null -> "UNAVAILABLE"
        difference.absoluteValue <= 0.01 -> "MATCHED"
        difference > 0.0 -> "LOWER"
        else -> "HIGHER"
    }

    val comparisonColor = when {
        difference == null ->
            MaterialTheme.colorScheme.onSurfaceVariant

        difference > 0.01 ->
            MaterialTheme.colorScheme.error

        else ->
            MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(logo),
                contentDescription = retailerName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = validPrice?.let(::formatIndianPrice) ?: "—",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = when {
                    validPrice == null -> comparisonText
                    isLive -> "LIVE • $comparisonText"
                    else -> "SAVED • $comparisonText"
                },
                color = comparisonColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun RetailerLinkButton(
    label: String,
    logo: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            horizontal = 10.dp,
            vertical = 8.dp
        )
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(logo),
            contentDescription = null,
            modifier = Modifier
                .width(48.dp)
                .height(22.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(5.dp))

        Icon(
            imageVector = Icons.Rounded.OpenInNew,
            contentDescription = "Open $label",
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun originalSortName(
    order: SortOrder
): String = when (order) {
    SortOrder.MOST_VIEWED -> "Most viewed"
    SortOrder.BEST_SAVING -> "Best saving"
    SortOrder.ALPHABETICAL -> "Alphabetical"
    SortOrder.RECENT -> "Recent"
}