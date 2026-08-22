@file:OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)

package com.supreme.priceintelligence.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.rememberUrlOpener
import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.scanner.ProductBarcodeScanner
import com.supreme.priceintelligence.scanner.rememberCameraPermissionRequester
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isScannerOpen by rememberSaveable { mutableStateOf(false) }
    var cameraPermissionDenied by rememberSaveable { mutableStateOf(false) }

    val cameraPermissionRequester = rememberCameraPermissionRequester { granted ->
        if (granted) {
            cameraPermissionDenied = false
            focusManager.clearFocus()
            isScannerOpen = true
        } else {
            cameraPermissionDenied = true
        }
    }

    if (isScannerOpen) {
        ProductBarcodeScanner(
            modifier = Modifier.fillMaxSize(),
            onScanned = { barcode ->
                isScannerOpen = false
                viewModel.onSearchSubmitted(barcode)
            },
            onError = {
                isScannerOpen = false
                cameraPermissionDenied = true
            },
            onCanceled = {
                isScannerOpen = false
            }
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "PRICE DASHBOARD",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Make every price decision with confidence",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 29.sp
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Compare your shop with Amazon and Flipkart using live or clearly marked saved prices.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = viewModel::refreshVisiblePrices,
                    enabled = state.isConnected &&
                        state.pageItems.any { card ->
                            !card.item.amazonUrl.isNullOrBlank() ||
                                !card.item.flipkartUrl.isNullOrBlank()
                        } &&
                        !state.isRefreshingPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (state.isRefreshingPage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Checking visible products",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Check visible prices",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Find a product")
                    },
                    placeholder = {
                        Text("Name, barcode, Amazon or Flipkart link")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.onSearchSubmitted(state.searchQuery)
                            focusManager.clearFocus()
                        }
                    ),
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    viewModel.onSearchQueryChanged("")
                                    viewModel.onSearchSubmitted("")
                                    focusManager.clearFocus()
                                }
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                )

                if (state.suggestions.isNotEmpty() && state.searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        state.suggestions.forEach { suggestion ->
                            Text(
                                text = suggestion,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.55f
                                        )
                                    )
                                    .clickable {
                                        viewModel.onSearchSubmitted(suggestion)
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 13.dp, vertical = 12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = cameraPermissionRequester::requestPermission,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Scan barcode",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.onSearchSubmitted(state.searchQuery)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Search",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (cameraPermissionDenied) {
            DashboardFeedbackBanner(
                message = "Camera access is off. Allow it in phone settings, or type the barcode.",
                isError = true
            )
        }

        when (state.bloomState) {
            BloomState.ERROR -> DashboardFeedbackBanner(
                message = "No internet connection. Saved prices are still available.",
                isError = true
            )

            BloomState.WARNING -> DashboardFeedbackBanner(
                message = "The online stores did not return a price. Please try again.",
                isError = false
            )

            else -> Unit
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    state.isLoading -> "Loading products..."
                    state.totalMatchCount == 1 -> "1 product"
                    else -> "${state.totalMatchCount} products"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Box {
                TextButton(
                    onClick = {
                        showSortMenu = true
                    }
                ) {
                    Text("Sort: ${state.sortOrder.displayName()}")
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = {
                        showSortMenu = false
                    }
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(order.displayName())
                            },
                            onClick = {
                                showSortMenu = false
                                viewModel.setSortOrder(order)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoading && state.pageItems.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 20.dp,
                                vertical = 26.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Preparing your price dashboard",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = "Loading saved products and price information.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            state.totalMatchCount == 0 -> {
                EmptyDashboardMessage(
                    isSearching = state.searchQuery.isNotBlank(),
                    query = state.searchQuery,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "decision-summary") {
                            DashboardDecisionSummaryCard(
                                summary = state.allMatchingItems.buildDecisionSummary(state.pageItems)
                            )
                        }

                        items(
                            items = state.pageItems,
                            key = { card -> card.item.id }
                        ) { card ->
                            DashboardProductCard(
                                card = card,
                                onOpenDetails = {
                                    selectedProductId = card.item.id
                                    viewModel.loadPriceHistory(card.item.id)
                                    focusManager.clearFocus()
                                },
                                onRefresh = {
                                    viewModel.refreshProduct(card.item.id)
                                }
                            )
                        }

                        if (state.totalPages > 1) {
                            item(key = "pagination") {
                                DashboardPagination(
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
    }

    val selectedCard = state.pageItems.firstOrNull { card ->
        card.item.id == selectedProductId
    }

    if (selectedCard != null) {
        DashboardProductDetailDialog(
            card = selectedCard,
            priceHistory = state.priceHistoryByProduct[selectedCard.item.id].orEmpty(),
            isHistoryLoading = selectedCard.item.id in state.historyLoadingProductIds,
            onRefresh = {
                viewModel.refreshProduct(selectedCard.item.id)
            },
            onDismiss = {
                selectedProductId = null
            }
        )
    }
}

@Composable
private fun DashboardFeedbackBanner(
    message: String,
    isError: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    Color(0xFF4A3510)
                }
            )
            .semantics {
                liveRegion = LiveRegionMode.Polite
            }
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Text(
            text = message,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                Color(0xFFFBBF24)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyDashboardMessage(
    isSearching: Boolean,
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 22.dp,
                    vertical = 26.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSearching) "?" else "+",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSearching) "SEARCH RESULT" else "GET STARTED",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = if (isSearching) {
                        "No matches found"
                    } else {
                        "Your dashboard is ready"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isSearching) {
                        "No saved product matches “$query”. Check the spelling or try a shorter name, barcode, or retailer link."
                    } else {
                        "Open Inventory below and add your first product. It will appear here for shop, Amazon, and Flipkart price comparison."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProductImage(
    imageUrl: String?,
    productName: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    var imageLoaded by remember(imageUrl) {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F5F9))
            .clickable(
                enabled = imageLoaded && onClick != null,
                onClick = {
                    onClick?.invoke()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!imageLoaded) {
            Text(
                text = productName.trim().firstOrNull()?.uppercase() ?: "P",
                color = Color(0xFF475569),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(7.dp),
                contentScale = ContentScale.Fit,
                onLoading = {
                    imageLoaded = false
                },
                onSuccess = {
                    imageLoaded = true
                },
                onError = {
                    imageLoaded = false
                }
            )
        }
    }
}

@Composable
private fun DashboardProductCard(
    card: ProductCardUiState,
    onOpenDetails: () -> Unit,
    onRefresh: () -> Unit
) {
    val item = card.item
    val openUrl = rememberUrlOpener()
    val amazonPrice = card.amazonResult?.price ?: item.amazonLastPrice
    val flipkartPrice = card.flipkartResult?.price ?: item.flipkartLastPrice
    val comparison = compareWithOnlinePrices(item.shopPrice, amazonPrice, flipkartPrice)
    val availablePrices = listOfNotNull(item.shopPrice, amazonPrice, flipkartPrice)
    val lowestPrice = availablePrices.minOrNull()
    val canRefresh = !item.amazonUrl.isNullOrBlank() || !item.flipkartUrl.isNullOrBlank()
    val onlinePrices = listOfNotNull(amazonPrice, flipkartPrice)
    val onlineSavings = comparison.shopDifference?.takeIf { saving -> saving > 0.01 }
    val onlineIsCheaper = onlineSavings != null
    val hasOnlinePrice = onlinePrices.isNotEmpty()
    val liveCheckAttempted = card.amazonResult != null || card.flipkartResult != null
    val hasLivePrice = card.amazonResult?.price != null || card.flipkartResult?.price != null
    val latestCheck = maxOf(
        item.amazonLastChecked ?: 0L,
        item.flipkartLastChecked ?: 0L
    )
    val liveCheckFailed = liveCheckAttempted && !hasLivePrice
    val shopIsLowest = item.shopPrice == lowestPrice
    val shopIsCompetitive =
        comparison.shopPosition == ShopPricePosition.LOWER ||
            comparison.shopPosition == ShopPricePosition.MATCHED

    val verdictText = when {
        card.isRefreshing -> "Checking Amazon and Flipkart prices now..."

        liveCheckFailed && hasOnlinePrice ->
            "Live check failed • the saved comparison is still shown below."

        liveCheckFailed ->
            "Live prices are unavailable. Try checking again later."

        onlineSavings != null -> buildString {
            append("Online is ")
            append(formatIndianPrice(onlineSavings))
            append(" cheaper than your shop")
            comparison.shopDifferencePercent?.let { percent ->
                append(" (")
                append(formatPercent(percent))
                append(")")
            }
            append(".")
        }

        comparison.shopPosition == ShopPricePosition.LOWER ->
            comparison.shopDifference?.let { difference ->
                "Your shop is ${formatIndianPrice(difference.absoluteValue)} cheaper than the lowest online price."
            } ?: "Your shop has the lower price."

        comparison.shopPosition == ShopPricePosition.MATCHED ->
            "Your shop matches the lowest available online price."

        hasOnlinePrice ->
            "A saved online comparison is available below."

        else ->
            "Check online prices to compare your shop with Amazon and Flipkart."
    }

    val verdictColor = when {
        liveCheckFailed || onlineIsCheaper -> MaterialTheme.colorScheme.error
        shopIsCompetitive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val verdictBackground = when {
        liveCheckFailed || onlineIsCheaper -> MaterialTheme.colorScheme.errorContainer
        shopIsCompetitive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.13f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = when {
                    card.isRefreshing -> "Checking online prices"
                    liveCheckAttempted && !hasLivePrice -> "Live check failed"
                    onlineIsCheaper -> "Online price is cheaper"
                    hasOnlinePrice -> "Saved online comparison available"
                    else -> "Online price not checked"
                }
            },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = when {
                onlineIsCheaper -> MaterialTheme.colorScheme.error
                hasOnlinePrice -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                ProductImage(
                    imageUrl = item.imageUrl,
                    productName = item.productName,
                    modifier = Modifier.size(88.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.productName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    item.barcode?.takeIf { it.isNotBlank() }?.let { barcode ->
                        Text(
                            text = "Barcode: $barcode",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (shopIsLowest) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = buildString {
                                append("SHOP • ")
                                append(formatIndianPrice(item.shopPrice))
                                if (shopIsLowest) append(" • LOWEST")
                            },
                            color = if (shopIsLowest) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = when {
                            card.isRefreshing -> "Checking online prices..."
                            liveCheckAttempted && !hasLivePrice && hasOnlinePrice ->
                                "Live check failed • showing saved prices"
                            liveCheckAttempted && !hasLivePrice -> "Live prices unavailable"
                            hasLivePrice && onlineIsCheaper -> "Live online price is lower"
                            hasLivePrice -> "Live prices updated"
                            hasOnlinePrice -> "Saved comparison available"
                            else -> "Online prices not checked"
                        },
                        color = when {
                            liveCheckAttempted && !hasLivePrice -> MaterialTheme.colorScheme.error
                            onlineIsCheaper -> MaterialTheme.colorScheme.error
                            hasOnlinePrice -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!card.isRefreshing && latestCheck > 0L) {
                        Text(
                            text = "Saved ${formatTimeAgo(latestCheck)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDetails,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "View details",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onRefresh,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = canRefresh && !card.isRefreshing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (card.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.width(7.dp))

                        Text("Checking")
                    } else {
                        Text(
                            text = if (canRefresh) {
                                "Check prices"
                            } else {
                                "No links"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(verdictBackground)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = verdictText,
                    color = verdictColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            PriceRow(
                seller = "Amazon",
                price = amazonPrice,
                isLowest = amazonPrice != null && amazonPrice == lowestPrice,
                sourceLabel = when {
                    card.amazonResult?.price != null -> "LIVE"
                    amazonPrice != null -> "SAVED"
                    else -> null
                },
                actionLabel = if (item.amazonUrl.isNullOrBlank()) null else "Open",
                onAction = item.amazonUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    { openUrl(url) }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PriceRow(
                seller = "Flipkart",
                price = flipkartPrice,
                isLowest = flipkartPrice != null && flipkartPrice == lowestPrice,
                sourceLabel = when {
                    card.flipkartResult?.price != null -> "LIVE"
                    flipkartPrice != null -> "SAVED"
                    else -> null
                },
                actionLabel = if (item.flipkartUrl.isNullOrBlank()) null else "Open",
                onAction = item.flipkartUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    { openUrl(url) }
                }
            )

            if (!canRefresh) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Add an Amazon or Flipkart link in Inventory to check online prices.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun DashboardProductDetailDialog(
    card: ProductCardUiState,
    priceHistory: List<PriceHistoryEntry>,
    isHistoryLoading: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val item = card.item
    val openUrl = rememberUrlOpener()
    val amazonPrice = card.amazonResult?.price ?: item.amazonLastPrice
    val flipkartPrice = card.flipkartResult?.price ?: item.flipkartLastPrice
    val canRefresh = !item.amazonUrl.isNullOrBlank() || !item.flipkartUrl.isNullOrBlank()
    val latestCheck = maxOf(
        item.amazonLastChecked ?: 0L,
        item.flipkartLastChecked ?: 0L
    )
    val hasLiveResult = card.amazonResult?.price != null || card.flipkartResult?.price != null
    var showZoomedImage by remember(item.imageUrl) {
        mutableStateOf(false)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "PRICE COMPARISON",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.productName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "Close",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item.barcode?.takeIf { barcode -> barcode.isNotBlank() }?.let { barcode ->
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Barcode: $barcode",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        card.isRefreshing -> "Checking online prices now..."
                        hasLiveResult -> "Prices checked just now"
                        latestCheck > 0L -> "Last checked ${formatTimeAgo(latestCheck)}"
                        else -> "Online prices have not been checked yet"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                ProductImage(
                    imageUrl = item.imageUrl,
                    productName = item.productName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    onClick = {
                        showZoomedImage = true
                    }
                )

                if (!item.imageUrl.isNullOrBlank()) {
                    Text(
                        text = "Tap the image to enlarge it",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                DetailPriceComparison(
                    seller = "Your shop",
                    price = item.shopPrice,
                    shopPrice = item.shopPrice,
                    isShopPrice = true,
                    sourceLabel = null,
                    onOpen = null
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailPriceComparison(
                    seller = "Amazon",
                    price = amazonPrice,
                    shopPrice = item.shopPrice,
                    isShopPrice = false,
                    sourceLabel = when {
                        card.amazonResult?.price != null -> "Live price"
                        amazonPrice != null -> savedPriceLabel(item.amazonLastChecked)
                        else -> null
                    },
                    onOpen = item.amazonUrl?.takeIf { url -> url.isNotBlank() }?.let { url ->
                        { openUrl(url) }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailPriceComparison(
                    seller = "Flipkart",
                    price = flipkartPrice,
                    shopPrice = item.shopPrice,
                    isShopPrice = false,
                    sourceLabel = when {
                        card.flipkartResult?.price != null -> "Live price"
                        flipkartPrice != null -> savedPriceLabel(item.flipkartLastChecked)
                        else -> null
                    },
                    onOpen = item.flipkartUrl?.takeIf { url -> url.isNotBlank() }?.let { url ->
                        { openUrl(url) }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                PriceHistorySection(
                    entries = priceHistory,
                    isLoading = isHistoryLoading,
                    shopPrice = item.shopPrice
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    enabled = canRefresh && !card.isRefreshing,
                    shape = RoundedCornerShape(14.dp),
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

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Checking prices")
                    } else {
                        Text(
                            if (canRefresh) {
                                "Refresh live prices"
                            } else {
                                "Add a retailer link in Inventory"
                            }
                        )
                    }
                }
            }
        }
    }

    if (showZoomedImage && !item.imageUrl.isNullOrBlank()) {
        ZoomedProductImageDialog(
            imageUrl = item.imageUrl,
            productName = item.productName,
            onDismiss = {
                showZoomedImage = false
            }
        )
    }
}

@Composable
private fun ZoomedProductImageDialog(
    imageUrl: String,
    productName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun DetailPriceComparison(
    seller: String,
    price: Double?,
    shopPrice: Double,
    isShopPrice: Boolean,
    sourceLabel: String?,
    onOpen: (() -> Unit)?
) {
    val difference = if (price == null) null else shopPrice - price
    val onlineIsCheaper = !isShopPrice && difference != null && difference > 0.01
    val shopIsCompetitive = !isShopPrice && difference != null && difference <= 0.01
    val comparisonShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(comparisonShape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
            )
            .border(
                width = 1.dp,
                color = when {
                    onlineIsCheaper -> MaterialTheme.colorScheme.error
                    shopIsCompetitive || isShopPrice -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                },
                shape = comparisonShape
            )
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = seller,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = price?.let(::formatIndianPrice) ?: "Price unavailable",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                if (sourceLabel != null) {
                    Text(
                        text = sourceLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (onOpen != null) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Open site",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

        }

        if (!isShopPrice) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = priceDifferenceMessage(difference),
                color = when {
                    onlineIsCheaper -> MaterialTheme.colorScheme.error
                    shopIsCompetitive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PriceRow(
    seller: String,
    price: Double?,
    isLowest: Boolean,
    sourceLabel: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    val priceRowShape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(priceRowShape)
            .background(
                if (isLowest) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isLowest) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
                },
                shape = priceRowShape
            )
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = seller,
                color = if (isLowest) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            val labels = listOfNotNull(
                sourceLabel,
                "LOWEST".takeIf { isLowest }
            ).joinToString(" • ")
            if (labels.isNotEmpty()) {
                Text(
                    text = labels,
                    color = if (isLowest) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Text(
            text = price?.let(::formatIndianPrice) ?: "Not checked",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.width(6.dp))

            TextButton(
                onClick = onAction,
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DashboardPagination(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PAGE $currentPage OF $totalPages",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = currentPage > 1,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Previous",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onNext,
                    enabled = currentPage < totalPages,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun SortOrder.displayName(): String = when (this) {
    SortOrder.MOST_VIEWED -> "Most viewed"
    SortOrder.BEST_SAVING -> "Best online saving"
    SortOrder.ALPHABETICAL -> "A to Z"
    SortOrder.RECENT -> "Recently updated"
}

private fun priceDifferenceMessage(difference: Double?): String = when {
    difference == null -> "No saved online price"
    difference.absoluteValue <= 0.01 -> "Matches your shop price"
    difference > 0.0 -> "${formatIndianPrice(difference)} cheaper than your shop"
    else -> "${formatIndianPrice(difference.absoluteValue)} higher than your shop"
}

internal fun formatTimeAgo(timeMs: Long): String {
    val elapsedMs = (
        Clock.System.now().toEpochMilliseconds() - timeMs
    ).coerceAtLeast(0L)
    val elapsedMinutes = elapsedMs / 60_000L
    val elapsedHours = elapsedMinutes / 60L
    val elapsedDays = elapsedHours / 24L

    return when {
        elapsedDays > 0L -> "$elapsedDays day${if (elapsedDays == 1L) "" else "s"} ago"
        elapsedHours > 0L -> "$elapsedHours hour${if (elapsedHours == 1L) "" else "s"} ago"
        elapsedMinutes > 0L -> "$elapsedMinutes minute${if (elapsedMinutes == 1L) "" else "s"} ago"
        else -> "recently"
    }
}

private fun savedPriceLabel(timeMs: Long?): String =
    if (timeMs != null && timeMs > 0L) {
        "Saved price • ${formatTimeAgo(timeMs)}"
    } else {
        "Saved price"
    }

internal fun formatPercent(value: Double): String {
    val tenths = (value.absoluteValue * 10.0).roundToLong()
    val whole = tenths / 10
    val decimal = tenths % 10
    return if (decimal == 0L) "$whole%" else "$whole.$decimal%"
}
