package com.supreme.priceintelligence.inventory

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.ui.theme.supremeColors

@Composable
internal fun OriginalProductEditorDialog(
    form: InventoryFormState,
    statusMessage: String?,
    statusIsError: Boolean,
    onProductNameChanged: (String) -> Unit,
    onPurchaseCostChanged: (String) -> Unit,
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
    val purchaseCostFocusRequester = remember { FocusRequester() }
    val sellingPriceFocusRequester = remember { FocusRequester() }
    val barcodeFocusRequester = remember { FocusRequester() }
    val amazonFocusRequester = remember { FocusRequester() }
    val flipkartFocusRequester = remember { FocusRequester() }

    val glowPrimary by animateColorAsState(
        targetValue = when {
            statusMessage == null ->
                Color.Transparent

            statusIsError ->
                MaterialTheme.colorScheme.error

            else ->
                MaterialTheme.supremeColors.competitive
        },
        label = "editorGlowPrimary"
    )

    val glowSecondary by animateColorAsState(
        targetValue = when {
            statusMessage == null ->
                Color.Transparent

            statusIsError ->
                Color(0xFFEF4444)

            else ->
                Color(0xFF34D399)
        },
        label = "editorGlowSecondary"
    )

    KeyboardAwareEditorDialog(
        onDismissRequest = onDismiss
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background.copy(
                        alpha = 0.98f
                    )
                )
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .height(600.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowPrimary.copy(alpha = 0.40f),
                                glowSecondary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            val availableDialogHeight =
                (maxHeight - 16.dp).coerceAtLeast(320.dp)

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = availableDialogHeight)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.supremeColors.panel,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.supremeColors.border
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 20.dp,
                                end = 8.dp,
                                top = 8.dp,
                                bottom = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (form.isEditing) {
                                "Edit Product"
                            } else {
                                "Add New Product"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onDismiss
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close product editor",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 20.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        OriginalEditorField(
                            label = "Product Name",
                            placeholder = "e.g., Hawkins 3.5L Cooker",
                            value = form.productName,
                            onValueChange = onProductNameChanged,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    purchaseCostFocusRequester.requestFocus()
                                }
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OriginalEditorField(
                                label = "Purchase Cost (Optional)",
                                placeholder = "₹ 0.00",
                                value = form.purchaseCost,
                                onValueChange = onPurchaseCostChanged,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(
                                        purchaseCostFocusRequester
                                    ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        sellingPriceFocusRequester
                                            .requestFocus()
                                    }
                                )
                            )

                            OriginalEditorField(
                                label = "Selling Price",
                                placeholder = "₹ 0.00",
                                value = form.shopPrice,
                                onValueChange = onShopPriceChanged,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(
                                        sellingPriceFocusRequester
                                    ),
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
                        }

                        OriginalEditorField(
                            label = "Barcode (Optional)",
                            placeholder = "Scan or enter barcode",
                            value = form.barcode,
                            onValueChange = onBarcodeChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(barcodeFocusRequester),
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
                                IconButton(
                                    onClick = onScanBarcode
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = "Scan barcode",
                                        tint =
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        OriginalEditorField(
                            label = "Amazon URL (Optional)",
                            placeholder = "https://amazon.in/...",
                            value = form.amazonUrl,
                            onValueChange = onAmazonUrlChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(amazonFocusRequester),
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

                        OriginalEditorField(
                            label = "Flipkart URL (Optional)",
                            placeholder = "https://flipkart.com/...",
                            value = form.flipkartUrl,
                            onValueChange = onFlipkartUrlChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(flipkartFocusRequester),
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

                        if (statusMessage != null) {
                            Text(
                                text = statusMessage,
                                color = if (statusIsError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(
                                    Alignment.CenterHorizontally
                                )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onClear,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.20f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = "Clear Form",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = onSave,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor =
                                        MaterialTheme.colorScheme.primary,
                                    contentColor =
                                        MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = "SAVE ITEM",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OriginalEditorField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            modifier = Modifier.padding(
                start = 4.dp,
                bottom = 6.dp
            )
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = trailingIcon,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =
                    MaterialTheme.supremeColors.panelMuted,
                unfocusedContainerColor =
                    MaterialTheme.supremeColors.panel,
                focusedBorderColor =
                    MaterialTheme.colorScheme.primary,
                unfocusedBorderColor =
                    MaterialTheme.supremeColors.border,
                focusedTextColor =
                    MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor =
                    MaterialTheme.colorScheme.onSurface,
                cursorColor =
                    MaterialTheme.colorScheme.primary
            )
        )
    }
}