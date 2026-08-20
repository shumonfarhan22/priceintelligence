package com.supreme.priceintelligence.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun DataSafetyDialog(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Protect your inventory",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Save your products, saved prices, and price history before changing phones or reinstalling the app.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Button(
                    onClick = onBackup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save backup", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Restore from backup", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Restore only adds missing products. It never deletes or replaces products already on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
internal fun ProductEditorDialog(
    form: InventoryFormState,
    statusMessage: String?,
    statusIsError: Boolean,
    onProductNameChanged: (String) -> Unit,
    onShopPriceChanged: (String) -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onScanBarcode: () -> Unit,
    onAmazonUrlChanged: (String) -> Unit,
    onFlipkartUrlChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val priceFocusRequester = remember { FocusRequester() }
    val barcodeFocusRequester = remember { FocusRequester() }
    val amazonFocusRequester = remember { FocusRequester() }
    val flipkartFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .imePadding(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
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
                        "UPDATE INVENTORY"
                    } else {
                        "NEW INVENTORY PRODUCT"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp
                )

                Text(
                    text = if (form.isEditing) {
                        "Edit product"
                    } else {
                        "Add product"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Product name and shop price are required. Barcode and retailer links are optional.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                OutlinedTextField(
                    value = form.productName,
                    onValueChange = onProductNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Product name") },
                    placeholder = { Text("Example: Prestige 3 Liters Cooker") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { priceFocusRequester.requestFocus() }),
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.shopPrice,
                    onValueChange = onShopPriceChanged,
                    modifier = Modifier.fillMaxWidth().focusRequester(priceFocusRequester),
                    label = { Text("Your shop price") },
                    placeholder = { Text("Example: 1500") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { barcodeFocusRequester.requestFocus() }),
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.barcode,
                    onValueChange = onBarcodeChanged,
                    modifier = Modifier.fillMaxWidth().focusRequester(barcodeFocusRequester),
                    label = { Text("Barcode — optional") },
                    placeholder = { Text("Type the barcode number") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { amazonFocusRequester.requestFocus() }),
                    trailingIcon = {
                        TextButton(onClick = onScanBarcode) { Text("Scan") }
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.amazonUrl,
                    onValueChange = onAmazonUrlChanged,
                    modifier = Modifier.fillMaxWidth().focusRequester(amazonFocusRequester),
                    label = { Text("Amazon link — optional") },
                    placeholder = { Text("https://amazon.in/...") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { flipkartFocusRequester.requestFocus() }),
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.flipkartUrl,
                    onValueChange = onFlipkartUrlChanged,
                    modifier = Modifier.fillMaxWidth().focusRequester(flipkartFocusRequester),
                    label = { Text("Flipkart link — optional") },
                    placeholder = { Text("https://flipkart.com/...") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (form.isEditing) "Save changes" else "Save product",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
