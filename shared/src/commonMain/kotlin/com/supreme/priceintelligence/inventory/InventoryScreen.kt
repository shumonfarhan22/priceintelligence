package com.supreme.priceintelligence.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.supreme.priceintelligence.data.InventoryItem
import kotlin.math.roundToLong

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var isEditorOpen by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        InventoryTitleRow(
            shownProductCount = state.products.size,
            isSearching = state.directoryQuery.isNotBlank(),
            onAddProduct = {
                viewModel.clearForm()
                isEditorOpen = true
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

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
            trailingIcon = {
                if (state.directoryQuery.isNotBlank()) {
                    TextButton(
                        onClick = {
                            viewModel.onDirectoryQueryChanged("")
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.products.isEmpty()) {
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
                items(
                    items = state.products,
                    key = { item -> item.id }
                ) { item ->
                    InventoryProductRow(
                        item = item,
                        isHighlighted = item.id == state.highlightedItemId,
                        onEdit = {
                            viewModel.startEditing(item)
                            isEditorOpen = true
                        }
                    )
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
}

@Composable
private fun InventoryTitleRow(
    shownProductCount: Int,
    isSearching: Boolean,
    onAddProduct: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Inventory",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = if (isSearching) {
                    "$shownProductCount search result(s)"
                } else {
                    "$shownProductCount saved product(s)"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        Button(
            onClick = onAddProduct,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "+ Add product",
                fontWeight = FontWeight.Bold
            )
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
        modifier = Modifier.fillMaxWidth(),
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
private fun InventoryProductRow(
    item: InventoryItem,
    isHighlighted: Boolean,
    onEdit: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)

    val cardColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardColor)
            .border(
                width = 1.dp,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = cardShape
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
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
            }

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedButton(
                onClick = onEdit,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Edit")
            }
        }

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
    }
}

@Composable
private fun ProductEditorDialog(
    form: InventoryFormState,
    statusMessage: String?,
    statusIsError: Boolean,
    onProductNameChanged: (String) -> Unit,
    onShopPriceChanged: (String) -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onAmazonUrlChanged: (String) -> Unit,
    onFlipkartUrlChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .imePadding(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (form.isEditing) {
                        "Edit product"
                    } else {
                        "Add product"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Only the product name and shop price are required.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = form.productName,
                    onValueChange = onProductNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Product name")
                    },
                    placeholder = {
                        Text("Example: Samsung Galaxy S25")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = form.shopPrice,
                    onValueChange = onShopPriceChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Your shop price")
                    },
                    placeholder = {
                        Text("Example: 49999")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = form.barcode,
                    onValueChange = onBarcodeChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Barcode — optional")
                    },
                    placeholder = {
                        Text("Type the barcode number")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = form.amazonUrl,
                    onValueChange = onAmazonUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Amazon link — optional")
                    },
                    placeholder = {
                        Text("https://amazon.in/...")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = form.flipkartUrl,
                    onValueChange = onFlipkartUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Flipkart link — optional")
                    },
                    placeholder = {
                        Text("https://flipkart.com/...")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    singleLine = true
                )

                statusMessage?.let { message ->
                    Text(
                        text = message,
                        color = if (statusIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (form.isEditing) {
                                "Save changes"
                            } else {
                                "Save product"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun displayPrice(price: Double): String {
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