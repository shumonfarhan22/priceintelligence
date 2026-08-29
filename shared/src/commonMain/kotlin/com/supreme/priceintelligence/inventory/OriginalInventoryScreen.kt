@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.supreme.priceintelligence.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.dashboard.formatIndianPrice
import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.settings.AppThemeMode
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.ui.theme.supremeColors
import com.supreme.priceintelligence.ui.components.ScrollAwareHeader
import com.supreme.priceintelligence.ui.components.rememberScrollAwareHeaderVisible
import com.supreme.priceintelligence.scanner.ProductBarcodeScanner
import com.supreme.priceintelligence.scanner.rememberCameraPermissionRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class InventoryScannerTarget {
    DIRECTORY_SEARCH,
    EDITOR_BARCODE
}

@Composable
fun OriginalInventoryScreen(
    viewModel: InventoryViewModel,
    customization: AppCustomization,
    onNavigateHome: () -> Unit = {},
    bottomBannerHeight: Dp = 0.dp,
    reduceMotionEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val inventoryListState = rememberLazyListState()
    val directorySearchState =
        rememberTextFieldState(state.directoryQuery)
    val editorTextState = rememberInventoryEditorTextState()

    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var scannerOpen by rememberSaveable { mutableStateOf(false) }
    var scannerTarget by rememberSaveable {
        mutableStateOf(InventoryScannerTarget.EDITOR_BARCODE)
    }

    LaunchedEffect(directorySearchState) {
        snapshotFlow {
            directorySearchState.text.toString()
        }
            .distinctUntilChanged()
            .collectLatest { query ->
                if (query.isNotBlank()) {
                    delay(250L)
                }

                viewModel.onDirectoryQueryChanged(
                    query = query,
                    debounceMillis = 0L
                )
            }
    }

    val cameraPermissionRequester =
        rememberCameraPermissionRequester { granted ->
            if (granted) {
                focusManager.clearFocus()
                scannerOpen = true
            } else {
                viewModel.reportError(
                    "Camera permission is needed to scan a barcode"
                )
            }
        }

    if (scannerOpen) {
        ProductBarcodeScanner(
            modifier = Modifier.fillMaxSize(),
            hapticFeedbackEnabled =
                customization.hapticsEnabled,
            onScanned = { barcode ->
                when (scannerTarget) {
                    InventoryScannerTarget.DIRECTORY_SEARCH ->
                        directorySearchState
                            .setTextAndPlaceCursorAtEnd(barcode)

                    InventoryScannerTarget.EDITOR_BARCODE ->
                        editorTextState.barcode
                            .setTextAndPlaceCursorAtEnd(barcode)
                }

                scannerOpen = false
            },
            onError = { message ->
                scannerOpen = false
                viewModel.reportError(message)
            },
            onCanceled = {
                scannerOpen = false
            }
        )
        return
    }

    val pendingIds = remember(state.pendingDeletes) {
        state.pendingDeletes
            .map { item -> item.id }
            .toSet()
    }

    val visibleProducts = remember(
        state.products,
        pendingIds,
        state.highlightedItemId
    ) {
        state.products
            .filterNot { item ->
                item.id in pendingIds
            }
            .let { products ->
                val highlightedId = state.highlightedItemId

                if (highlightedId == null) {
                    products
                } else {
                    products.sortedByDescending { item ->
                        item.id == highlightedId
                    }
                }
            }
    }

    val groupedProducts = remember(
        visibleProducts,
        state.highlightedItemId
    ) {
        visibleProducts
            .groupBy { item ->
                item.productName
                    .trim()
                    .substringBefore(" ")
                    .uppercase()
                    .ifBlank { "OTHER" }
            }
            .toList()
            .sortedWith(
                compareBy<Pair<String, List<InventoryItem>>> { group ->
                    if (
                        group.second.any { item ->
                            item.id == state.highlightedItemId
                        }
                    ) {
                        0
                    } else {
                        1
                    }
                }.thenBy { group ->
                    group.first
                }
            )
    }

    val allVisibleSelected = remember(
        visibleProducts,
        state.selectedItemIds
    ) {
        visibleProducts.isNotEmpty() &&
                visibleProducts.all { item ->
                    item.id in state.selectedItemIds
                }
    }

    val inventoryHeaderVisible =
        rememberScrollAwareHeaderVisible(
            listState = inventoryListState,
            forceVisible =
                state.isSelectionMode ||
                    visibleProducts.isEmpty()
        )

    LaunchedEffect(
        state.highlightedItemId,
        visibleProducts.size,
        reduceMotionEnabled
    ) {
        if (
            state.highlightedItemId != null &&
            visibleProducts.isNotEmpty()
        ) {
            if (reduceMotionEnabled) {
                inventoryListState.scrollToItem(0)
            } else {
                inventoryListState.animateScrollToItem(0)
            }
        } else if (
            inventoryListState.firstVisibleItemIndex <= 1
        ) {
            inventoryListState.scrollToItem(0)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ScrollAwareHeader(
                visible = inventoryHeaderVisible,
                reduceMotionEnabled = reduceMotionEnabled
            ) {
                OriginalInventoryHeader(
                    totalProducts = visibleProducts.size,
                    isRefreshing = state.isRefreshing,
                    selectedCount =
                        state.selectedItemIds.size,
                    isAllSelected = allVisibleSelected,
                    onNavigateHome = onNavigateHome,
                    onRefresh =
                        viewModel::refreshInventory,
                    onSelectAll =
                        viewModel::selectAllVisible,
                    onClearSelection =
                        viewModel::clearSelection,
                    onDeleteSelected =
                        viewModel::queueSelectedForDelete
                )
            }

            ProfessionalInventorySearchField(
                state = directorySearchState,
                onClear = {
                    directorySearchState.clearText()
                    focusManager.clearFocus()
                },
                onScan = {
                    scannerTarget =
                        InventoryScannerTarget.DIRECTORY_SEARCH
                    cameraPermissionRequester.requestPermission()
                },
                onDone = {
                    viewModel.onDirectoryQueryChanged(
                        query = directorySearchState.text.toString(),
                        debounceMillis = 0L
                    )
                    focusManager.clearFocus()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (visibleProducts.isEmpty()) {
                CompactInventoryEmptyState(
                    isSearching = state.directoryQuery.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = inventoryListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        bottom = if (state.isSelectionMode) {
                            76.dp + bottomBannerHeight
                        } else {
                            104.dp + bottomBannerHeight
                        }
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedProducts.forEach { group ->
                        val groupName = group.first
                        val products = group.second
                        val expanded =
                            state.directoryQuery.isNotBlank() ||
                                    groupName in state.expandedGroups

                        item(
                            key = "group:$groupName",
                            contentType = "inventory-group-header"
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec =
                                            if (reduceMotionEnabled) {
                                                null
                                            } else {
                                                tween(durationMillis = 160)
                                            },
                                        placementSpec =
                                            if (reduceMotionEnabled) {
                                                null
                                            } else {
                                                tween(durationMillis = 220)
                                            },
                                        fadeOutSpec =
                                            if (reduceMotionEnabled) {
                                                null
                                            } else {
                                                tween(durationMillis = 120)
                                            }
                                    )
                            ) {
                                ProfessionalInventoryGroupHeader(
                                    groupName = groupName,
                                    productCount = products.size,
                                    expanded = expanded,
                                    reduceMotionEnabled =
                                        reduceMotionEnabled,
                                    onClick = {
                                        viewModel.toggleGroup(groupName)
                                    }
                                )
                            }
                        }

                        if (expanded) {
                            items(
                                items = products,
                                key = { item -> "product:${item.id}" },
                                contentType = {
                                    "inventory-product-row"
                                }
                            ) { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(
                                            fadeInSpec =
                                                if (reduceMotionEnabled) {
                                                    null
                                                } else {
                                                    tween(durationMillis = 160)
                                                },
                                            placementSpec =
                                                if (reduceMotionEnabled) {
                                                    null
                                                } else {
                                                    tween(durationMillis = 220)
                                                },
                                            fadeOutSpec =
                                                if (reduceMotionEnabled) {
                                                    null
                                                } else {
                                                    tween(durationMillis = 120)
                                                }
                                        )
                                ) {
                                    ProfessionalInventoryProductRow(
                                        item = item,
                                        selected =
                                            item.id in state.selectedItemIds,
                                        highlighted =
                                            item.id == state.highlightedItemId,
                                        selectionMode = state.isSelectionMode,
                                        onToggleSelection = {
                                            viewModel.toggleSelection(item.id)
                                        },
                                        onEdit = {
                                            editorTextState.load(item)
                                            viewModel.startEditing(item)
                                            editorOpen = true
                                        },
                                        onDelete = {
                                            viewModel.queueDelete(setOf(item))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(
                                alpha = 0.35f
                            ),
                            MaterialTheme.colorScheme.background.copy(
                                alpha = 0.78f
                            )
                        )
                    )
                )
        )

        AnimatedVisibility(
            visible = !state.isSelectionMode && bottomBannerHeight <= 0.dp,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FloatingActionButton(
                onClick = {
                    viewModel.clearSelection()
                    viewModel.clearForm()
                    editorTextState.clear()
                    editorOpen = true
                },
                modifier = Modifier
                    .padding(
                        end = 14.dp,
                        bottom = 16.dp
                    ),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add product",
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }

    if (editorOpen) {
        OriginalProductEditorDialog(
            form = state.form,
            textState = editorTextState,
            statusMessage = state.statusMessage,
            statusIsError = state.statusIsError,
            onScanBarcode = {
                scannerTarget =
                    InventoryScannerTarget.EDITOR_BARCODE
                cameraPermissionRequester.requestPermission()
            },
            onSave = { editedForm, onSaved ->
                viewModel.saveProduct(
                    form = editedForm,
                    onSuccess = onSaved
                )
            },
            reduceMotionEnabled = reduceMotionEnabled,
            onDismiss = {
                viewModel.clearForm()
                editorOpen = false
            }
        )
    }

}

@Composable
private fun OriginalInventoryHeader(
    totalProducts: Int,
    isRefreshing: Boolean,
    selectedCount: Int,
    isAllSelected: Boolean,
    onNavigateHome: () -> Unit,
    onRefresh: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val largeText =
        LocalDensity.current.fontScale >= 1.10f

    if (selectedCount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel selection"
                )
            }

            Text(
                text = "$selectedCount selected",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = if (isAllSelected) {
                    onClearSelection
                } else {
                    onSelectAll
                }
            ) {
                Text(
                    text = if (isAllSelected) "None" else "All",
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onDeleteSelected) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete selected products",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateHome,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector =
                    Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to launch page",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Surface(
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.secondary
                .copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(9.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (largeText) {
                Text(
                    text = "SUPREME",
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    fontSize = 19.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "INVENTORY",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 19.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black
                )
            } else {
                Row {
                    Text(
                        text = "SUPREME ",
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "INVENTORY",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Text(
                text = if (totalProducts == 1) {
                    "1 Total Product"
                } else {
                    "$totalProducts Total Products"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        IconButton(
            onClick = onRefresh,
            enabled = !isRefreshing
        ) {
            if (isRefreshing) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh inventory"
                )
            }
        }

    }
}

@Composable
private fun CompactInventoryStatus(
    message: String,
    isError: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        shape = RoundedCornerShape(10.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        Text(
            text = message,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
        )
    }
}

@Composable
private fun OriginalInventoryGroupHeader(
    groupName: String,
    productCount: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.55f
                )
            )
            .semantics {
                role = Role.Button
                stateDescription = if (expanded) {
                    "Expanded"
                } else {
                    "Collapsed"
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = groupName,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = productCount.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = if (expanded) {
                Icons.Rounded.ExpandLess
            } else {
                Icons.Rounded.ExpandMore
            },
            contentDescription = if (expanded) {
                "Collapse $groupName"
            } else {
                "Expand $groupName"
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun OriginalInventoryProductRow(
    item: InventoryItem,
    selected: Boolean,
    highlighted: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val swipeState = rememberSwipeToDismissBoxState()

    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue == SwipeToDismissBoxValue.Settled) {
            swipeState.reset()
        }
    }

    SwipeToDismissBox(
        state = swipeState,
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !selectionMode,
        gesturesEnabled = !selectionMode,
        onDismiss = { direction ->
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (swipeState.currentValue != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete ${item.productName}",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 22.dp)
                    )
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    this.selected = selected
                    role = Role.Checkbox
                    contentDescription = buildString {
                        append(item.productName)
                        append(", ")
                        append(formatIndianPrice(item.shopPrice))
                        if (selected) append(", selected")
                    }
                }
                .combinedClickable(
                    onClick = {
                        if (selectionMode) {
                            onToggleSelection()
                        } else {
                            onEdit()
                        }
                    },
                    onLongClick = onToggleSelection
                ),
            shape = RoundedCornerShape(13.dp),
            color = when {
                selected ->
                    MaterialTheme.colorScheme.primaryContainer

                highlighted ->
                    MaterialTheme.colorScheme.primaryContainer

                else ->
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (selected || highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 82.dp)
                    .padding(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InventoryProductImage(
                    item = item,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.width(11.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
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

                    Surface(
                        shape = RoundedCornerShape(7.dp),
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        )
                    }
                }

                if (selectionMode) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = if (selected) {
                            "Selected"
                        } else {
                            "Not selected"
                        },
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                } else {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit ${item.productName}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryProductImage(
    item: InventoryItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.productName
                .trim()
                .firstOrNull()
                ?.uppercase()
                ?: "P",
            color = Color(0xFF475569),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.productName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun CompactInventoryEmptyState(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) {
                Icons.Rounded.Search
            } else {
                Icons.Rounded.Inventory2
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isSearching) {
                "No matching products"
            } else {
                "Inventory is empty"
            },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isSearching) {
                "Try another search"
            } else {
                "Tap + to add a product"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AdvancedModeDialog(
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    priceChangeNotificationsEnabled: Boolean,
    onPriceChangeNotificationsChanged: (Boolean) -> Unit,
    reduceMotionEnabled: Boolean,
    onDismiss: () -> Unit
) {
    val dialogMotionProgress = remember {
        Animatable(0f)
    }

    var dismissRequested by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        dismissRequested,
        reduceMotionEnabled
    ) {
        if (dismissRequested) {
            if (reduceMotionEnabled) {
                dialogMotionProgress.snapTo(0f)
            } else {
                dialogMotionProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 160
                    )
                )
            }

            onDismiss()
        } else {
            if (reduceMotionEnabled) {
                dialogMotionProgress.snapTo(1f)
            } else {
                dialogMotionProgress.snapTo(0f)
                dialogMotionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 200
                    )
                )
            }
        }
    }

    val requestDismiss: () -> Unit = {
        if (!dismissRequested) {
            dismissRequested = true
        }
    }

    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .graphicsLayer {
                    val progress =
                        dialogMotionProgress.value

                    alpha = progress
                    scaleX =
                        0.96f + (0.04f * progress)
                    scaleY =
                        0.96f + (0.04f * progress)
                },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            ),
            shadowElevation =
                if (MaterialTheme.supremeColors.isDark) {
                    0.dp
                } else {
                    14.dp
                }
        ) {
            Column(
                modifier = Modifier.padding(17.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(9.dp))

                    Text(
                        text = "Settings",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = requestDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close settings"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Appearance",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Choose how the app should look",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { option ->
                        val isSelected = option == themeMode

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onThemeModeChanged(option)
                                }
                                .semantics {
                                    role = Role.RadioButton
                                    selected = isSelected
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.displayName(),
                                    color = if (isSelected) {
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimaryContainer
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = "System follows the light or dark setting of your phone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Advanced mode",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "History and extra comparison tools",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChanged
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The original compact design stays unchanged when this is off.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Price change alerts",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Notify me when the daily check finds a new Amazon or Flipkart price",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked =
                            priceChangeNotificationsEnabled,
                        onCheckedChange =
                            onPriceChangeNotificationsChanged
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Manual price checks do not send notifications.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun AppThemeMode.displayName(): String =
    when (this) {
        AppThemeMode.SYSTEM -> "System"
        AppThemeMode.LIGHT -> "Light"
        AppThemeMode.DARK -> "Dark"
    }
