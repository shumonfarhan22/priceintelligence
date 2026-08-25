@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.supreme.priceintelligence.inventory

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.supreme.priceintelligence.scanner.ProductBarcodeScanner
import com.supreme.priceintelligence.scanner.rememberCameraPermissionRequester
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogException
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitPickerException
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun OriginalInventoryScreen(
    viewModel: InventoryViewModel,
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    advancedModeEnabled: Boolean,
    onAdvancedModeChanged: (Boolean) -> Unit,
    bottomBannerHeight: Dp = 0.dp,
    reduceMotionEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val inventoryListState = rememberLazyListState()

    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var scannerOpen by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }

    val backupSaver = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onError = { failure: FileKitDialogException ->
            pendingBackupJson = null
            viewModel.reportBackupError(
                failure.message ?: "The backup file could not be saved"
            )
        },
        onResult = { file: PlatformFile? ->
            val backupJson = pendingBackupJson
            pendingBackupJson = null

            if (file != null && backupJson != null) {
                coroutineScope.launch {
                    try {
                        file.writeString(backupJson)
                        viewModel.reportBackupSaved()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        viewModel.reportBackupError(
                            "The backup file could not be written"
                        )
                    }
                }
            }
        }
    )

    val backupPicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("json")),
        onError = { failure: FileKitPickerException ->
            viewModel.reportBackupError(
                failure.message ?: "The backup file could not be opened"
            )
        },
        onResult = { file: PlatformFile? ->
            if (file != null) {
                coroutineScope.launch {
                    try {
                        viewModel.restoreBackupJson(file.readString())
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: IllegalArgumentException) {
                        viewModel.reportBackupError(
                            error.message
                                ?: "This is not a valid Price Intelligence backup"
                        )
                    } catch (_: Exception) {
                        viewModel.reportBackupError(
                            "The backup file could not be restored"
                        )
                    }
                }
            }
        }
    )

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
            onScanned = { barcode ->
                viewModel.onFormFieldChanged(barcode = barcode)
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
            OriginalInventoryHeader(
                totalProducts = visibleProducts.size,
                isRefreshing = state.isRefreshing,
                selectedCount = state.selectedItemIds.size,
                isAllSelected = allVisibleSelected,
                menuOpen = menuOpen,
                onMenuOpenChanged = { open ->
                    menuOpen = open
                },
                onRefresh = viewModel::refreshInventory,
                onExportBackup = {
                    menuOpen = false

                    coroutineScope.launch {
                        try {
                            pendingBackupJson = viewModel.createBackupJson()

                            backupSaver.launch(
                                suggestedName = "price-intelligence-backup",
                                defaultExtension = "json",
                                allowedExtensions = setOf("json")
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            pendingBackupJson = null
                            viewModel.reportBackupError(
                                "The backup could not be prepared"
                            )
                        }
                    }
                },
                onImportBackup = {
                    menuOpen = false
                    backupPicker.launch()
                },
                onSettings = {
                    menuOpen = false
                    settingsOpen = true
                },
                onSelectAll = viewModel::selectAllVisible,
                onClearSelection = viewModel::clearSelection,
                onDeleteSelected = viewModel::queueSelectedForDelete
            )

            ProfessionalInventorySearchField(
                value = state.directoryQuery,
                onValueChange = viewModel::onDirectoryQueryChanged,
                onClear = {
                    viewModel.onDirectoryQueryChanged("")
                    focusManager.clearFocus()
                },
                onDone = {
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
                            120.dp + bottomBannerHeight
                        } else {
                            184.dp + bottomBannerHeight
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
                    editorOpen = true
                },
                modifier = Modifier
                    .padding(
                        end = 14.dp,
                        bottom = 96.dp
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
            statusMessage = state.statusMessage,
            statusIsError = state.statusIsError,
            onProductNameChanged = { value ->
                viewModel.onFormFieldChanged(productName = value)
            },
            onPurchaseCostChanged = { value ->
                viewModel.onFormFieldChanged(purchaseCost = value)
            },
            onShopPriceChanged = { value ->
                viewModel.onFormFieldChanged(shopPrice = value)
            },
            onBarcodeChanged = { value ->
                viewModel.onFormFieldChanged(barcode = value)
            },
            onScanBarcode = cameraPermissionRequester::requestPermission,
            onAmazonUrlChanged = { value ->
                viewModel.onFormFieldChanged(amazonUrl = value)
            },
            onFlipkartUrlChanged = { value ->
                viewModel.onFormFieldChanged(flipkartUrl = value)
            },
            onClear = {
                viewModel.clearFormFields()
            },
            onSave = {
                viewModel.saveProduct {
                    editorOpen = false
                }
            },
            onDismiss = {
                viewModel.clearForm()
                editorOpen = false
            }
        )
    }

    if (settingsOpen) {
        AdvancedModeDialog(
            themeMode = themeMode,
            onThemeModeChanged = onThemeModeChanged,
            enabled = advancedModeEnabled,
            onEnabledChanged = onAdvancedModeChanged,
            onDismiss = {
                settingsOpen = false
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
    menuOpen: Boolean,
    onMenuOpenChanged: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onSettings: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    if (selectedCount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
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
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row {
                Text(
                    text = "SUPREME ",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "INVENTORY",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
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

        Box {
            IconButton(
                onClick = {
                    onMenuOpenChanged(true)
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Inventory menu"
                )
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = {
                    onMenuOpenChanged(false)
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Export backup")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Upload,
                            contentDescription = null
                        )
                    },
                    onClick = onExportBackup
                )

                DropdownMenuItem(
                    text = {
                        Text("Import backup")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null
                        )
                    },
                    onClick = onImportBackup
                )

                DropdownMenuItem(
                    text = {
                        Text("Settings")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null
                        )
                    },
                    onClick = onSettings
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
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.90f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
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

                    IconButton(onClick = onDismiss) {
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