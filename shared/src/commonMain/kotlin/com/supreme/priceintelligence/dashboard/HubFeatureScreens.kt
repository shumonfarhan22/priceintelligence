package com.supreme.priceintelligence.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.scanner.ProductBarcodeScanner
import com.supreme.priceintelligence.scanner.rememberCameraPermissionRequester
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

@Composable
internal fun QuickCompareScreen(
    viewModel: DashboardViewModel,
    advancedModeEnabled: Boolean,
    reduceMotionEnabled: Boolean,
    customization: AppCustomization,
    onNavigateHome: () -> Unit,
    previewMode: Boolean = false,
    focusCatalogPreview: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val quickCompareSearchState =
        rememberTextFieldState(state.searchDraft)
    val focusManager = LocalFocusManager.current
    val keyboardController =
        LocalSoftwareKeyboardController.current

    LaunchedEffect(state.searchDraft) {
        if (
            state.searchDraft !=
            quickCompareSearchState.text.toString()
        ) {
            quickCompareSearchState
                .setTextAndPlaceCursorAtEnd(
                    state.searchDraft
                )
        }
    }

    val searchFocusRequester = remember {
        FocusRequester()
    }

    var scannerOpen by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedProductId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    var pendingExactQuery by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var searchSubmitted by rememberSaveable {
        mutableStateOf(false)
    }

    var catalogRevealed by rememberSaveable {
        mutableStateOf(previewMode)
    }

    val quickCompareGridState =
        rememberLazyGridState()

    var quickCompareSearchVisible by rememberSaveable {
        mutableStateOf(true)
    }

    var quickCompareSearchFocused by remember {
        mutableStateOf(false)
    }

    val quickCompareSearchHideThresholdPx =
        with(LocalDensity.current) {
            24.dp.roundToPx()
        }

    val selectedCard =
        state.pageItems.firstOrNull { card ->
            card.item.id == selectedProductId
        }

    fun openProduct(
        card: ProductCardUiState
    ) {
        pendingExactQuery = null
        selectedProductId = card.item.id
        keyboardController?.hide()
        focusManager.clearFocus()

        viewModel.recordProductViewed(
            card.item.id
        )

        if (advancedModeEnabled) {
            viewModel.loadPriceHistory(
                card.item.id
            )
        }
    }

    fun submitQuery(
        enteredQuery: String
    ) {
        val cleanQuery = enteredQuery.trim()

        if (cleanQuery.isBlank()) {
            return
        }

        searchSubmitted = true
        catalogRevealed = true
        pendingExactQuery = cleanQuery
        viewModel.onSearchSubmitted(cleanQuery)
        viewModel.onSearchFocusChanged(false)
    }

    val permissionRequester =
        rememberCameraPermissionRequester { granted ->
            if (granted) {
                keyboardController?.hide()
                focusManager.clearFocus()
                scannerOpen = true
            }
        }

    LaunchedEffect(Unit) {
        if (!previewMode) {
            viewModel.prepareQuickCompare()
            delay(120)
            quickCompareSearchFocused = true
        }
    }

    LaunchedEffect(
        quickCompareGridState,
        quickCompareSearchHideThresholdPx,
        quickCompareSearchFocused
    ) {
        if (quickCompareSearchFocused) {
            quickCompareSearchVisible = true
            return@LaunchedEffect
        }

        var previousIndex =
            quickCompareGridState.firstVisibleItemIndex

        var previousOffset =
            quickCompareGridState.firstVisibleItemScrollOffset

        var downwardTravelPx = 0

        snapshotFlow {
            quickCompareGridState.firstVisibleItemIndex to
                quickCompareGridState.firstVisibleItemScrollOffset
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
                    quickCompareSearchVisible = true
                    downwardTravelPx = 0
                }

                scrollingUp -> {
                    quickCompareSearchVisible = true
                    downwardTravelPx = 0
                }

                scrollingDown -> {
                    val downwardDelta =
                        if (currentIndex > previousIndex) {
                            quickCompareSearchHideThresholdPx
                        } else {
                            (currentOffset - previousOffset)
                                .coerceAtLeast(0)
                        }

                    downwardTravelPx += downwardDelta

                    if (
                        downwardTravelPx >=
                        quickCompareSearchHideThresholdPx
                    ) {
                        quickCompareSearchVisible = false
                        downwardTravelPx = 0
                    }
                }
            }

            previousIndex = currentIndex
            previousOffset = currentOffset
        }
    }

    LaunchedEffect(
        pendingExactQuery,
        state.pageItems,
        state.isLoading
    ) {
        val query =
            pendingExactQuery
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: return@LaunchedEffect

        val exactCard =
            state.pageItems.firstOrNull { card ->
                card.item.productName.equals(
                    query,
                    ignoreCase = true
                ) ||
                        card.item.barcode?.equals(
                            query,
                            ignoreCase = true
                        ) == true ||
                        card.item.amazonUrl?.equals(
                            query,
                            ignoreCase = true
                        ) == true ||
                        card.item.flipkartUrl?.equals(
                            query,
                            ignoreCase = true
                        ) == true
            }

        if (exactCard != null) {
            openProduct(exactCard)
        } else if (
            !state.isLoading &&
            state.pageItems.isEmpty()
        ) {
            pendingExactQuery = null
        }
    }

    if (scannerOpen) {
        ProductBarcodeScanner(
            modifier = Modifier.fillMaxSize(),
            hapticFeedbackEnabled =
                customization.hapticsEnabled,
            onScanned = { barcode ->
                scannerOpen = false
                quickCompareSearchState
                    .setTextAndPlaceCursorAtEnd(barcode)
                submitQuery(barcode)
                quickCompareSearchFocused = false
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            },
            onError = {
                scannerOpen = false
            },
            onCanceled = {
                scannerOpen = false
            }
        )

        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedVisibility(
                visible =
                    quickCompareSearchVisible &&
                        !focusCatalogPreview,
                enter =
                    if (reduceMotionEnabled) {
                        fadeIn(
                            animationSpec =
                                tween(durationMillis = 0)
                        )
                    } else {
                        expandVertically(
                            animationSpec =
                                tween(durationMillis = 180),
                            expandFrom = Alignment.Top
                        ) + fadeIn(
                            animationSpec =
                                tween(durationMillis = 140)
                        )
                    },
                exit =
                    if (reduceMotionEnabled) {
                        fadeOut(
                            animationSpec =
                                tween(durationMillis = 0)
                        )
                    } else {
                        shrinkVertically(
                            animationSpec =
                                tween(durationMillis = 160),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(
                            animationSpec =
                                tween(durationMillis = 110)
                        )
                    }
            ) {
                QuickCompareHeader(
                    onNavigateHome = onNavigateHome
                )
            }

        QuickCompareCatalogGrid(
            cards = state.pageItems,
            query = state.searchQuery,
            gridState = quickCompareGridState,
            showResults = catalogRevealed,
            reduceMotionEnabled =
                reduceMotionEnabled,
            isLoading = state.isLoading,
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            onPageSelected = { page ->
                viewModel.goToPage(page)
            },
            onProductClick = { card ->
                openProduct(card)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        }

        ProfessionalDashboardSearchOverlay(
            searchState = quickCompareSearchState,
            suggestions =
                if (state.searchDraft.isNotBlank()) {
                    state.suggestions
                } else {
                    emptyList()
                },
            isFocused = quickCompareSearchFocused,
            bottomBannerHeight = 0.dp,
            additionalBannerHeight = 0.dp,
            reduceMotionEnabled =
                reduceMotionEnabled,
            onQueryChange = { query ->
                searchSubmitted = false
                pendingExactQuery = null

                viewModel.onSearchQueryChanged(
                    query = query,
                    suggestionsEnabled =
                        query.isNotBlank()
                )
            },
            onSubmit = ::submitQuery,
            onScanClick =
                permissionRequester::requestPermission,
            onFocusChange = { focused ->
                val reopeningSearch =
                    focused && catalogRevealed

                if (focused) {
                    catalogRevealed = false
                    quickCompareSearchVisible = true

                    if (reopeningSearch) {
                        searchSubmitted = false
                        pendingExactQuery = null
                        quickCompareSearchState
                            .setTextAndPlaceCursorAtEnd("")
                    }
                } else {
                    catalogRevealed = true
                }

                quickCompareSearchFocused = focused

                viewModel.onSearchFocusChanged(
                    focused = focused,
                    suggestionsEnabled =
                        focused &&
                            !reopeningSearch &&
                            state.searchDraft.isNotBlank()
                )
            },
            onDismissFocus = {
                quickCompareSearchFocused = false
                catalogRevealed = true
                keyboardController?.hide()
                focusManager.clearFocus()

                viewModel.onSearchFocusChanged(
                    focused = false,
                    suggestionsEnabled = false
                )
            },
            scrimFollowsSuggestions = true,
            morphSearchButton = true,
            modifier = Modifier.fillMaxSize()
        )
    }

    if (selectedCard != null) {
        OriginalProfessionalProductDetailDialog(
            card = selectedCard,
            networkState = state.bloomState,
            advancedModeEnabled =
                advancedModeEnabled,
            reduceMotionEnabled =
                reduceMotionEnabled,
            insightCustomization =
                customization.insightCustomization,
            isHistoryLoading =
                selectedCard.item.id in
                        state.historyLoadingProductIds,
            priceHistory =
                state.priceHistoryByProduct[
                    selectedCard.item.id
                ].orEmpty(),
            onRefresh = {
                viewModel.refreshProduct(
                    selectedCard.item.id
                )
            },
            onDismiss = {
                selectedProductId = null
                pendingExactQuery = null
            }
        )
    }
}

@Composable
private fun QuickCompareHeader(
    onNavigateHome: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateHome
        ) {
            Icon(
                imageVector =
                    Icons.AutoMirrored
                        .Rounded
                        .ArrowBack,
                contentDescription =
                    "Back to launch page"
            )
        }

        Surface(
            shape = RoundedCornerShape(13.dp),
            color =
                MaterialTheme
                    .colorScheme
                    .primary
                    .copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(9.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "QUICK COMPARE",
                color =
                    MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text = "Open one comparison quickly",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickCompareGuide() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            1.dp,
            MaterialTheme.supremeColors.border
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "One product, fewer steps",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "An exact name, barcode, or saved retailer link opens the Product Details screen automatically.",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                QuickCompareFeature(
                    text = "Search",
                    modifier = Modifier.weight(1f)
                )

                QuickCompareFeature(
                    text = "Scan",
                    modifier = Modifier.weight(1f)
                )

                QuickCompareFeature(
                    text = "Refresh",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickCompareFeature(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color =
            MaterialTheme
                .colorScheme
                .primary
                .copy(alpha = 0.09f)
    ) {
        Text(
            text = text,
            color =
                MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 8.dp
            )
        )
    }
}

@Composable
private fun QuickCompareSuggestion(
    suggestion: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            1.dp,
            MaterialTheme.supremeColors.border
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 11.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = suggestion,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun HubPriceMovementScreen(
    viewModel: DashboardViewModel,
    reduceMotionEnabled: Boolean,
    customization: AppCustomization,
    notificationTarget:
    PriceMovementNotificationTarget?,
    onNotificationConsumed: (String) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(
        notificationTarget?.requestId
    ) {
        viewModel.loadShopPriceMovement()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    )

    ShopPriceMovementDialog(
        snapshot = state.shopPriceMovement,
        isLoading =
            state.isShopPriceMovementLoading,
        errorMessage =
            state.shopPriceMovementError,
        reduceMotionEnabled =
            reduceMotionEnabled,
        customization = customization,
        notificationTarget =
            notificationTarget,
        useInternalTransition = false,
        onRefresh =
            viewModel::loadShopPriceMovement,
        onDismiss = {
            notificationTarget
                ?.requestId
                ?.let(onNotificationConsumed)

            onNavigateHome()
        }
    )
}
