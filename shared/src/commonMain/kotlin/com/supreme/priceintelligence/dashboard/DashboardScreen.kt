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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.rememberUrlOpener
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Dashboard",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Compare your shop price with live online prices",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            OutlinedButton(
                onClick = viewModel::refresh,
                enabled = !state.isLoading
            ) {
                Text(if (state.isLoading) "Loading" else "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search product name, barcode, or link")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    TextButton(
                        onClick = {
                            viewModel.onSearchSubmitted(state.searchQuery)
                            focusManager.clearFocus()
                        }
                    ) {
                        Text("Search")
                    }
                }
            }
        )

        if (state.suggestions.isNotEmpty() && state.searchQuery.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
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
                                .clickable {
                                    viewModel.onSearchSubmitted(suggestion)
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
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
                    CircularProgressIndicator()
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
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.pageItems,
                        key = { card -> card.item.id }
                    ) { card ->
                        DashboardProductCard(
                            card = card,
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
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
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
            text = if (isSearching) "No matches found" else "Dashboard is empty",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSearching) {
                "No saved product matches “$query”.\nTry a shorter product name or a barcode."
            } else {
                "Open Inventory below and add your first product.\nIt will then appear here for price comparison."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DashboardProductCard(
    card: ProductCardUiState,
    onRefresh: () -> Unit
) {
    val item = card.item
    val openUrl = rememberUrlOpener()
    val amazonPrice = card.amazonResult?.price ?: item.amazonLastPrice
    val flipkartPrice = card.flipkartResult?.price ?: item.flipkartLastPrice
    val availablePrices = listOfNotNull(item.shopPrice, amazonPrice, flipkartPrice)
    val lowestPrice = availablePrices.minOrNull()
    val canRefresh = !item.amazonUrl.isNullOrBlank() || !item.flipkartUrl.isNullOrBlank()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.productName.trim().firstOrNull()?.uppercase() ?: "P",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

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
                }

                OutlinedButton(
                    onClick = onRefresh,
                    enabled = canRefresh && !card.isRefreshing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (card.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Check")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            PriceRow(
                seller = "Your shop",
                price = item.shopPrice,
                isLowest = item.shopPrice == lowestPrice,
                actionLabel = null,
                onAction = null
            )

            Spacer(modifier = Modifier.height(8.dp))

            PriceRow(
                seller = "Amazon",
                price = amazonPrice,
                isLowest = amazonPrice != null && amazonPrice == lowestPrice,
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
private fun PriceRow(
    seller: String,
    price: Double?,
    isLowest: Boolean,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isLowest) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = seller,
            color = if (isLowest) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (isLowest) {
            Text(
                text = "LOWEST",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Text(
            text = price?.let(::formatPrice) ?: "Not checked",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.width(6.dp))

            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(actionLabel)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = currentPage > 1
        ) {
            Text("Previous")
        }

        Text(
            text = "Page $currentPage of $totalPages",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Button(
            onClick = onNext,
            enabled = currentPage < totalPages,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Next")
        }
    }
}

private fun SortOrder.displayName(): String = when (this) {
    SortOrder.MOST_VIEWED -> "Most viewed"
    SortOrder.ALPHABETICAL -> "A to Z"
    SortOrder.RECENT -> "Recently added"
}

private fun formatPrice(value: Double): String {
    val paise = (value * 100).roundToLong()
    val whole = paise / 100
    val decimal = (paise % 100).absoluteValue
    return if (decimal == 0L) {
        "₹$whole"
    } else {
        "₹$whole.${decimal.toString().padStart(2, '0')}"
    }
}
