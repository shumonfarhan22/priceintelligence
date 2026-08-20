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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Color
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

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "SEARCH CATALOGUE",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp
                )

                Spacer(modifier = Modifier.height(7.dp))

                OutlinedTextField(
                    value = state.directoryQuery,
                    onValueChange = viewModel::onDirectoryQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    label = {
                        Text("Find a saved product")
                    },
                    placeholder = {
                        Text("Name, barcode, Amazon or Flipkart link")
                    },
                    shape = RoundedCornerShape(16.dp),
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
                                Text(
                                    text = "Clear",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )
            }
        }

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
    val countLabel = when {
        isRefreshing -> "Refreshing your inventory…"
        isSearching && shownProductCount == 1 -> "1 matching product"
        isSearching -> "$shownProductCount matching products"
        shownProductCount == 1 -> "1 saved product"
        else -> "$shownProductCount saved products"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "SUPREME CATALOGUE",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Inventory",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = countLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onAddProduct,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text(
                        text = "Add product",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isRefreshing) "Refreshing…" else "Refresh",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDataSafety,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Backup & restore",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "SELECTION MODE",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp
                    )

                    Text(
                        text = if (selectedCount == 1) {
                            "1 product selected"
                        } else {
                            "$selectedCount products selected"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                TextButton(
                    onClick = onClear,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Tap products to add or remove them from this selection.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = if (isAllSelected) onClear else onSelectAll,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text(
                        text = if (isAllSelected) {
                            "Deselect all"
                        } else {
                            "Select shown"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Delete $selectedCount",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
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
        color = backgroundColor.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = textColor.copy(alpha = 0.42f)
        )
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
                    text = if (isSearching) "SEARCH RESULT" else "PRODUCT CATALOGUE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = if (isSearching) {
                        "No matching products"
                    } else {
                        "Your inventory is empty"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isSearching) {
                        "Try a different product name, barcode, Amazon link, or Flipkart link."
                    } else {
                        "Press “Add product” above to save your first item and begin comparing prices."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InventoryGroupHeader(
    brandName: String,
    productCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val groupShape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(groupShape)
            .background(
                if (isExpanded) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isExpanded) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = groupShape
            )
            .semantics {
                role = Role.Button
                stateDescription = if (isExpanded) "Expanded" else "Collapsed"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = brandName.take(1),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.width(11.dp))

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
                    "1 saved product"
                } else {
                    "$productCount saved products"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(50.dp),
            color = if (isExpanded) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)
            }
        ) {
            Text(
                text = if (isExpanded) "CLOSE" else "OPEN",
                color = if (isExpanded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                )
            )
        }
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
    val cardShape = RoundedCornerShape(22.dp)
    val amazonLinked = !item.amazonUrl.isNullOrBlank()
    val flipkartLinked = !item.flipkartUrl.isNullOrBlank()

    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
        isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.54f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }

    val borderColor = if (isHighlighted || isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardColor)
            .semantics {
                if (isSelectionMode) {
                    selected = isSelected
                    role = Role.Checkbox
                }

                contentDescription = buildString {
                    append(item.productName)
                    append(", shop price ")
                    append(displayPrice(item.shopPrice))

                    if (isSelected) {
                        append(", selected")
                    }

                    if (isSelectionMode) {
                        append(". Tap to change selection.")
                    } else {
                        append(". Tap to edit. Long press to select.")
                    }
                }
            }
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        onEdit()
                    }
                },
                onLongClick = onToggleSelection
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = cardShape
            )
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CATALOGUE PRODUCT",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.productName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
                    )
                ) {
                    Text(
                        text = "SELECTED",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(11.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.46f),
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "YOUR SHOP PRICE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp
                )

                Text(
                    text = displayPrice(item.shopPrice),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = "LOCAL",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item.barcode?.takeIf { it.isNotBlank() }?.let { barcode ->
            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = "Barcode • $barcode",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(11.dp))

        Text(
            text = "RETAILER LINKS",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.7.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InventoryRetailerBadge(
                name = "Amazon",
                isLinked = amazonLinked,
                accent = Color(0xFFFFA41C),
                modifier = Modifier.weight(1f)
            )

            InventoryRetailerBadge(
                name = "Flipkart",
                isLinked = flipkartLinked,
                accent = Color(0xFF2874F0),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isSelectionMode) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                }
            ) {
                Text(
                    text = if (isSelected) {
                        "Selected • tap this card to remove it"
                    } else {
                        "Tap this card to add it to the selection"
                    },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Edit product",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
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

@Composable
private fun InventoryRetailerBadge(
    name: String,
    isLinked: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = if (isLinked) {
            accent.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isLinked) {
                accent.copy(alpha = 0.46f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 9.dp
            )
        ) {
            Text(
                text = name,
                color = if (isLinked) {
                    accent
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = if (isLinked) "LINKED" else "NOT LINKED",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
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
