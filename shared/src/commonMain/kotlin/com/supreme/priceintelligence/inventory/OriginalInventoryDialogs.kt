package com.supreme.priceintelligence.inventory

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
internal fun OriginalProductEditorDialog(
    form: InventoryFormState,
    statusMessage: String?,
    statusIsError: Boolean,
    onProductNameChanged: (String) -> Unit,
    onShopPriceChanged: (String) -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onScanBarcode: () -> Unit,
    onAmazonUrlChanged: (String) -> Unit,
    onFlipkartUrlChanged: (String) -> Unit,
    onClear: () -> Unit,
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
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .imePadding(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (form.isEditing) {
                                "EDIT PRODUCT"
                            } else {
                                "ADD NEW PRODUCT"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "SUPREME INVENTORY",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close product editor"
                        )
                    }
                }

                OutlinedTextField(
                    value = form.productName,
                    onValueChange = onProductNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Product name")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            priceFocusRequester.requestFocus()
                        }
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    OutlinedTextField(
                        value = form.shopPrice,
                        onValueChange = onShopPriceChanged,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(priceFocusRequester),
                        label = {
                            Text("Shop price")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                barcodeFocusRequester.requestFocus()
                            }
                        )
                    )

                    OutlinedTextField(
                        value = form.barcode,
                        onValueChange = onBarcodeChanged,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(barcodeFocusRequester),
                        label = {
                            Text("Barcode")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                amazonFocusRequester.requestFocus()
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = onScanBarcode) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = "Scan barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }

                OutlinedTextField(
                    value = form.amazonUrl,
                    onValueChange = onAmazonUrlChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amazonFocusRequester),
                    label = {
                        Text("Amazon link")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            flipkartFocusRequester.requestFocus()
                        }
                    )
                )

                OutlinedTextField(
                    value = form.flipkartUrl,
                    onValueChange = onFlipkartUrlChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(flipkartFocusRequester),
                    label = {
                        Text("Flipkart link")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    )
                )

                statusMessage?.let { message ->
                    Text(
                        text = message,
                        color = if (statusIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 49.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ClearAll,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.padding(horizontal = 3.dp))

                        Text(
                            text = "CLEAR",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 49.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.padding(horizontal = 3.dp))

                        Text(
                            text = if (form.isEditing) {
                                "SAVE"
                            } else {
                                "SAVE ITEM"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}