@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.supreme.priceintelligence.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.supreme.priceintelligence.data.InventoryItem
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
import kotlin.math.roundToLong

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var isEditorOpen by rememberSaveable {
        mutableStateOf(false)
    }
    var isDataSafetyOpen by rememberSaveable {
        mutableStateOf(false)
    }
    var isScannerOpen by rememberSaveable {
        mutableStateOf(false)
    }
    var pendingBackupJson by remember {
        mutableStateOf<String?>(null)
    }

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
                        isDataSafetyOpen = false
                        viewModel.reportBackupSaved()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        viewModel.reportBackupError("The backup file could not be written")
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
                        isDataSafetyOpen = false
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: IllegalArgumentException) {
                        viewModel.reportBackupError(
                            error.message ?: "This is not a valid Price Intelligence backup"
                        )
                    } catch (_: Exception) {
                        viewModel.reportBackupError("The backup file could not be restored")
                    }
                }
            }
        }
    )

    val cameraPermissionRequester = rememberCameraPermissionRequester { granted ->
        if (granted) {
            focusManager.clearFocus()
            isScannerOpen = true
        } else {
            viewModel.reportError(
                "Camera permission is needed to scan a barcode. You can still type it manually."
            )
        }
    }

    if (isScannerOpen) {
        ProductBarcodeScanner(
            modifier = Modifier.fillMaxSize(),
            onScanned = { barcode ->
                viewModel.onFormFieldChanged(barcode = barcode)
                isScannerOpen = false
            },
            onError = { message ->
                isScannerOpen = false
                viewModel.reportError(message)
            },
            onCanceled = {
                isScannerOpen = false
            }
        )
        return
    }

    val pendingIds = state.pendingDeletes.map { item ->
        item.id
    }.toSet()

    val visibleProducts = state.products.filter { item ->
        item.id !in pendingIds
    }

    val groupedProducts = visibleProducts
        .groupBy { item ->
            item.productName
                .trim()
                .substringBefore(" ")
                .uppercase()
                .ifBlank { "OTHER" }
        }
        .toList()
        .sortedBy { group ->
            group.first
        }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        InventoryTitleRow(
            shownProductCount = visibleProducts.size,
            isSearching = state.directoryQuery.isNotBlank(),
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refreshInventory,
            onDataSafety = {
                isDataSafetyOpen = true
            },
            onAddProduct = {
                viewModel.clearSelection()
                viewModel.clearForm()
                isEditorOpen = true
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isSelectionMode) {
            InventorySelectionBar(
                selectedCount = state.selectedItemIds.size,
                isAllSelected = visibleProducts.isNotEmpty() &&
                    visibleProducts.all { item -> item.id in state.selectedItemIds },
                onSelectAll = viewModel::selectAllVisible,
                onClear = viewModel::clearSelection,
                onDelete = viewModel::queueSelectedForDelete
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        state.statusMessage?.let { message ->
            InventoryStatusMessage(
                message = message,
                isError = state.statusIsError
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = state.directoryQuery,
            onValueChange = viewModel::onDirectoryQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search name, barcode, or product link")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            trailingIcon = {
                if (state.directoryQuery.isNotBlank()) {
                    TextButton(
                        onClick = {
                            viewModel.onDirectoryQueryChanged("")
                            focusManager.clearFocus()
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (visibleProducts.isEmpty()) {
            EmptyInventoryMessage(
                isSearching = state.directoryQuery.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedProducts.forEach { group ->
                    val brandName = group.first
                    val brandProducts = group.second

                    val isExpanded =
                        state.directoryQuery.isNotBlank() ||
                            brandName in state.expandedGroups

                    item(
                        key = "group:$brandName"
                    ) {
                        InventoryGroupHeader(
                            brandName = brandName,
                            productCount = brandProducts.size,
                            isExpanded = isExpanded,
                            onClick = {
                                viewModel.toggleGroup(brandName)
                            }
                        )
                    }

                    if (isExpanded) {
                        items(
                            items = brandProducts,
                            key = { item ->
                                "product:${item.id}"
                            }
                        ) { item ->
                            InventoryProductRow(
                                item = item,
                                isHighlighted = item.id == state.highlightedItemId,
                                isSelected = item.id in state.selectedItemIds,
                                isSelectionMode = state.isSelectionMode,
                                onToggleSelection = {
                                    viewModel.toggleSelection(item.id)
                                },
                                onEdit = {
                                    viewModel.startEditing(item)
                                    isEditorOpen = true
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

    if (isEditorOpen) {
        ProductEditorDialog(
            form = state.form,
            statusMessage = state.statusMessage,
            statusIsError = state.statusIsError,
            onProductNameChanged = { value ->
                viewModel.onFormFieldChanged(productName = value)
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
            onSave = {
                viewModel.saveProduct {
                    isEditorOpen = false
                }
            },
            onDismiss = {
                viewModel.clearForm()
                isEditorOpen = false
            }
        )
    }

    if (isDataSafetyOpen) {
        DataSafetyDialog(
            onBackup = {
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
                        viewModel.reportBackupError("The backup could not be prepared")
                    }
                }
            },
            onRestore = {
                backupPicker.launch()
            },
            onDismiss = {
                isDataSafetyOpen = false
            }
        )
    }
}

@Composable
private fun InventoryTitleRow(
    shownProductCount: Int,
    isSearching: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDataSafety: () -> Unit,
    onAddProduct: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Supreme Inventory",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = when {
                isRefreshing -> "Refreshing your inventory..."
                isSearching -> "$shownProductCount search result(s)"
                else -> "$shownProductCount saved product(s)"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isRefreshing) {
                        "Refreshing..."
                    } else {
                        "Refresh"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onAddProduct,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "+ Add product",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDataSafety,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Backup & restore inventory",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InventorySelectionBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (selectedCount == 1) "1 product selected" else "$selectedCount products selected",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Long-press a product to start, then tap more products.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = if (isAllSelected) onClear else onSelectAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isAllSelected) "Deselect all" else "Select shown")
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete selected", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = onClear,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Cancel selection")
            }
        }
    }
}

@Composable
private fun InventoryStatusMessage(
    message: String,
    isError: Boolean
) {
    val backgroundColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            )
        )
    }
}

@Composable
private fun EmptyInventoryMessage(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSearching) {
                "No matching products"
            } else {
                "Your inventory is empty"
            },
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSearching) {
                "Try a different name, barcode, or link."
            } else {
                "Press “Add product” to save your first item."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun InventoryGroupHeader(
    brandName: String,
    productCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val groupShape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(groupShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics {
                role = Role.Button
                stateDescription = if (isExpanded) "Expanded" else "Collapsed"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = brandName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = if (productCount == 1) {
                    "1 product"
                } else {
                    "$productCount products"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Text(
            text = if (isExpanded) {
                "Hide ▲"
            } else {
                "Show ▼"
            },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InventoryProductRow(
    item: InventoryItem,
    isHighlighted: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)

    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isHighlighted -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardColor)
            .semantics {
                selected = isSelected
                role = Role.Checkbox
                contentDescription = buildString {
                    append(item.productName)
                    append(", shop price ")
                    append(displayPrice(item.shopPrice))
                    if (isSelected) append(", selected")
                    append(". Long press to select.")
                }
            }
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection() else onEdit()
                },
                onLongClick = onToggleSelection
            )
            .border(
                width = 1.dp,
                color = if (isHighlighted || isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = cardShape
            )
            .padding(14.dp)
    ) {
        Text(
            text = item.productName,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Shop price: ${displayPrice(item.shopPrice)}",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        item.barcode?.takeIf { it.isNotBlank() }?.let { barcode ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Barcode: $barcode",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        val linkedStores = buildList {
            if (!item.amazonUrl.isNullOrBlank()) add("Amazon")
            if (!item.flipkartUrl.isNullOrBlank()) add("Flipkart")
        }

        if (linkedStores.isNotEmpty()) {
            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Links: ${linkedStores.joinToString()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isSelectionMode) {
            Text(
                text = if (isSelected) "Selected • tap to remove" else "Tap to select",
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Edit",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


private fun displayPrice(price: Double): String {
    if (!price.isFinite()) return "Price unavailable"

    val paise = (price * 100).roundToLong()
    val wholePart = paise / 100
    val decimalPart = (paise % 100)
        .toString()
        .padStart(2, '0')

    return if (decimalPart == "00") {
        "₹$wholePart"
    } else {
        "₹$wholePart.$decimalPart"
    }
}
