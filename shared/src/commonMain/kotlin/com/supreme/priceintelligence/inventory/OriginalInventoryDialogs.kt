@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.supreme.priceintelligence.inventory

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.ui.layout.adaptiveLayoutPolicy
import com.supreme.priceintelligence.ui.input.BarcodeInputTransformation
import com.supreme.priceintelligence.ui.input.DecimalNumberInputTransformation
import com.supreme.priceintelligence.ui.input.KeyboardAccessoryAction
import com.supreme.priceintelligence.ui.input.PlatformProductNameTextField
import com.supreme.priceintelligence.ui.input.dismissKeyboardOnUnhandledTap
import com.supreme.priceintelligence.ui.input.rememberKeyboardDismissAction
import com.supreme.priceintelligence.ui.input.rememberPlatformTextInputOptions
import com.supreme.priceintelligence.ui.theme.supremeColors

@Composable
internal fun OriginalProductEditorDialog(
    form: InventoryFormState,
    textState: InventoryEditorTextState,
    statusMessage: String?,
    statusIsError: Boolean,
    onScanBarcode: () -> Unit,
    onSave: (
        form: InventoryFormState,
        onSaved: () -> Unit
    ) -> Unit,
    reduceMotionEnabled: Boolean,
    onDismiss: () -> Unit
) {
    val dismissKeyboard = rememberKeyboardDismissAction()
    val currentFontScale =
        LocalDensity.current.fontScale

    val purchaseCostFocusRequester =
        remember { FocusRequester() }
    val sellingPriceFocusRequester = remember { FocusRequester() }
    val barcodeFocusRequester = remember { FocusRequester() }
    val amazonFocusRequester = remember { FocusRequester() }
    val flipkartFocusRequester = remember { FocusRequester() }

    var calculatorTarget by remember {
        mutableStateOf<PriceEditorField?>(null)
    }

    var browserSite by remember {
        mutableStateOf<RetailerBrowserSite?>(null)
    }

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
                        durationMillis = 170
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
                        durationMillis = 210
                    )
                )
            }
        }
    }

    val requestDismiss: () -> Unit = {
        if (!dismissRequested) {
            dismissKeyboard()
            dismissRequested = true
        }
    }

    val dialogScrimAlpha =
        if (MaterialTheme.supremeColors.isDark) {
            0.78f
        } else {
            0.52f
        }

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
        onDismissRequest = {
            if (browserSite != null) {
                browserSite = null
            } else {
                requestDismiss()
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .dismissKeyboardOnUnhandledTap(dismissKeyboard)
                .background(
                    MaterialTheme.supremeColors.scrim.copy(
                        alpha =
                            dialogScrimAlpha *
                                dialogMotionProgress.value
                    )
                )
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .height(600.dp)
                    .graphicsLayer {
                        alpha = dialogMotionProgress.value
                    }
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
                (maxHeight - 16.dp)
                    .coerceAtLeast(320.dp)

            val editorLayout =
                adaptiveLayoutPolicy(
                    availableWidthDp =
                        maxWidth.value,
                    fontScale =
                        currentFontScale
                )

            val stackEditorRows =
                editorLayout.shouldStack(
                    minimumWidthForRowDp = 360f
                )

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = availableDialogHeight)
                    .wrapContentHeight()
                    .graphicsLayer {
                        val progress =
                            dialogMotionProgress.value

                        alpha = progress
                        scaleX =
                            0.96f + (0.04f * progress)
                        scaleY =
                            0.96f + (0.04f * progress)
                    },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.supremeColors.panelStrong,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.supremeColors.border
                ),
                shadowElevation =
                    if (MaterialTheme.supremeColors.isDark) {
                        0.dp
                    } else {
                        14.dp
                    }
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
                            onClick = requestDismiss
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
                            state = textState.productName,
                            usePlatformProductNameField = true,
                            isPlatformFieldReady =
                                !dismissRequested &&
                                    dialogMotionProgress.value >= 0.999f,
                            onImeAction = {
                                purchaseCostFocusRequester.requestFocus()
                            }
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PRICE DETAILS",
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                GlassCalculatorButton(
                                    onClick = {
                                        dismissKeyboard()
                                        calculatorTarget =
                                            PriceEditorField.SELLING_PRICE
                                    }
                                )
                            }

                            if (stackEditorRows) {
                                Column(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            12.dp
                                        )
                                ) {
                                    OriginalEditorField(
                                        label =
                                            "Purchase Cost",
                                        placeholder = "₹ 0.00",
                                        state =
                                            textState.purchaseCost,
                                        optional = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(
                                                purchaseCostFocusRequester
                                            ),
                                        keyboardOptions =
                                            KeyboardOptions(
                                                keyboardType =
                                                    KeyboardType
                                                        .Decimal,
                                                imeAction =
                                                    ImeAction.Next
                                            ),
                                        inputTransformation =
                                            DecimalNumberInputTransformation,
                                        keyboardAccessoryAction =
                                            KeyboardAccessoryAction.NEXT,
                                        onImeAction = {
                                            sellingPriceFocusRequester
                                                .requestFocus()
                                        }
                                    )

                                    OriginalEditorField(
                                        label =
                                            "Selling Price",
                                        placeholder = "₹ 0.00",
                                        state =
                                            textState.shopPrice,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(
                                                sellingPriceFocusRequester
                                            ),
                                        keyboardOptions =
                                            KeyboardOptions(
                                                keyboardType =
                                                    KeyboardType
                                                        .Decimal,
                                                imeAction =
                                                    ImeAction.Next
                                            ),
                                        inputTransformation =
                                            DecimalNumberInputTransformation,
                                        keyboardAccessoryAction =
                                            KeyboardAccessoryAction.NEXT,
                                        onImeAction = {
                                            barcodeFocusRequester
                                                .requestFocus()
                                        }
                                    )
                                }
                            } else {
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            12.dp
                                        )
                                ) {
                                    OriginalEditorField(
                                        label =
                                            "Purchase Cost",
                                        placeholder = "₹ 0.00",
                                        state =
                                            textState.purchaseCost,
                                        optional = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(
                                                purchaseCostFocusRequester
                                            ),
                                        keyboardOptions =
                                            KeyboardOptions(
                                                keyboardType =
                                                    KeyboardType
                                                        .Decimal,
                                                imeAction =
                                                    ImeAction.Next
                                            ),
                                        inputTransformation =
                                            DecimalNumberInputTransformation,
                                        keyboardAccessoryAction =
                                            KeyboardAccessoryAction.NEXT,
                                        onImeAction = {
                                            sellingPriceFocusRequester
                                                .requestFocus()
                                        }
                                    )

                                    OriginalEditorField(
                                        label =
                                            "Selling Price",
                                        placeholder = "₹ 0.00",
                                        state =
                                            textState.shopPrice,
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(
                                                sellingPriceFocusRequester
                                            ),
                                        keyboardOptions =
                                            KeyboardOptions(
                                                keyboardType =
                                                    KeyboardType
                                                        .Decimal,
                                                imeAction =
                                                    ImeAction.Next
                                            ),
                                        inputTransformation =
                                            DecimalNumberInputTransformation,
                                        keyboardAccessoryAction =
                                            KeyboardAccessoryAction.NEXT,
                                        onImeAction = {
                                            barcodeFocusRequester
                                                .requestFocus()
                                        }
                                    )
                                }
                            }
                        }

                        OriginalEditorField(
                            label = "Barcode",
                            placeholder = "Scan or enter barcode",
                            state = textState.barcode,
                            optional = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(
                                    barcodeFocusRequester
                                ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            inputTransformation =
                                BarcodeInputTransformation,
                            keyboardAccessoryAction =
                                KeyboardAccessoryAction.DONE,
                            onImeAction = {
                                dismissKeyboard()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = onScanBarcode
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Rounded.CameraAlt,
                                        contentDescription =
                                            "Scan barcode",
                                        tint =
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        val amazonUrl =
                            textState.amazonUrl.text.toString()
                        val flipkartUrl =
                            textState.flipkartUrl.text.toString()
                        val hasRetailerUrl =
                            amazonUrl.isNotBlank() ||
                                flipkartUrl.isNotBlank()

                        if (!hasRetailerUrl) {
                            if (stackEditorRows) {
                                Column(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            12.dp
                                        )
                                ) {
                                    RetailerBrowserButton(
                                        label = "Amazon",
                                        onClick = {
                                            dismissKeyboard()
                                            browserSite =
                                                RetailerBrowserSite
                                                    .AMAZON
                                        },
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )

                                    RetailerBrowserButton(
                                        label = "Flipkart",
                                        onClick = {
                                            dismissKeyboard()
                                            browserSite =
                                                RetailerBrowserSite
                                                    .FLIPKART
                                        },
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            12.dp
                                        )
                                ) {
                                    RetailerBrowserButton(
                                        label = "Amazon",
                                        onClick = {
                                            dismissKeyboard()
                                            browserSite =
                                                RetailerBrowserSite
                                                    .AMAZON
                                        },
                                        modifier =
                                            Modifier.weight(1f)
                                    )

                                    RetailerBrowserButton(
                                        label = "Flipkart",
                                        onClick = {
                                            dismissKeyboard()
                                            browserSite =
                                                RetailerBrowserSite
                                                    .FLIPKART
                                        },
                                        modifier =
                                            Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            if (amazonUrl.isBlank()) {
                                RetailerBrowserButton(
                                    label = "Amazon",
                                    onClick = {
                                        dismissKeyboard()
                                        browserSite =
                                            RetailerBrowserSite.AMAZON
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OriginalEditorField(
                                    label = "Amazon URL",
                                    placeholder =
                                        "https://amazon.in/product",
                                    state = textState.amazonUrl,
                                    optional = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(
                                            amazonFocusRequester
                                        ),
                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType =
                                                KeyboardType.Uri,
                                            imeAction = if (
                                                flipkartUrl
                                                    .isNotBlank()
                                            ) {
                                                ImeAction.Next
                                            } else {
                                                ImeAction.Done
                                            }
                                        ),
                                    onImeAction = {
                                        if (flipkartUrl.isNotBlank()) {
                                            flipkartFocusRequester
                                                .requestFocus()
                                        } else {
                                            dismissKeyboard()
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                dismissKeyboard()
                                                browserSite =
                                                    RetailerBrowserSite.AMAZON
                                            }
                                        ) {
                                            Icon(
                                                imageVector =
                                                    Icons.Rounded.Language,
                                                contentDescription =
                                                    "Browse Amazon India",
                                                tint =
                                                    MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }

                            if (flipkartUrl.isBlank()) {
                                RetailerBrowserButton(
                                    label = "Flipkart",
                                    onClick = {
                                        dismissKeyboard()
                                        browserSite =
                                            RetailerBrowserSite.FLIPKART
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OriginalEditorField(
                                    label = "Flipkart URL",
                                    placeholder =
                                        "https://flipkart.com/product",
                                    state = textState.flipkartUrl,
                                    optional = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(
                                            flipkartFocusRequester
                                        ),
                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType =
                                                KeyboardType.Uri,
                                            imeAction =
                                                ImeAction.Done
                                    ),
                                    onImeAction = {
                                        dismissKeyboard()
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                dismissKeyboard()
                                                browserSite =
                                                    RetailerBrowserSite.FLIPKART
                                            }
                                        ) {
                                            Icon(
                                                imageVector =
                                                    Icons.Rounded.Language,
                                                contentDescription =
                                                    "Browse Flipkart",
                                                tint =
                                                    MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }

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

                        EditorActionButtons(
                            stackVertically =
                                stackEditorRows,
                            onClear = textState::clear,
                            onSave = {
                                onSave(
                                    textState.formWithEditingItem(
                                        form.editingItem
                                    ),
                                    requestDismiss
                                )
                            }
                        )
                    }
                }
            }

            browserSite?.let { site ->
                RetailerBrowserDialog(
                    site = site,
                    onUseLink = { url ->
                        when (site) {
                            RetailerBrowserSite.AMAZON ->
                                textState.amazonUrl
                                    .setTextAndPlaceCursorAtEnd(url)

                            RetailerBrowserSite.FLIPKART ->
                                textState.flipkartUrl
                                    .setTextAndPlaceCursorAtEnd(url)
                        }

                        browserSite = null
                    },
                    onDismiss = {
                        browserSite = null
                    }
                )
            }
        }
    }

    calculatorTarget?.let { target ->
        ProductPriceCalculatorDialog(
            initialField = target,
            purchaseCost =
                textState.purchaseCost.text.toString(),
            sellingPrice =
                textState.shopPrice.text.toString(),
            onApply = { field, value ->
                when (field) {
                    PriceEditorField.PURCHASE_COST ->
                        textState.purchaseCost
                            .setTextAndPlaceCursorAtEnd(value)

                    PriceEditorField.SELLING_PRICE ->
                        textState.shopPrice
                            .setTextAndPlaceCursorAtEnd(value)
                }
            },
            onDismiss = {
                calculatorTarget = null
            }
        )
    }

}

@Composable
private fun GlassCalculatorButton(
    onClick: () -> Unit
) {
    val primaryColor =
        MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color =
            MaterialTheme.supremeColors.panelMuted,
        contentColor = primaryColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.supremeColors.border
        )
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 38.dp)
                .padding(
                    horizontal = 11.dp,
                    vertical = 8.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.Calculate,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = "Calculator",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EditorActionButtons(
    stackVertically: Boolean,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    val clearButton:
        @Composable (Modifier) -> Unit =
        { buttonModifier ->
            OutlinedButton(
                onClick = onClear,
                modifier = buttonModifier
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme
                            .supremeColors
                            .border
                ),
                colors =
                    ButtonDefaults
                        .outlinedButtonColors(
                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )
            ) {
                Text(
                    text = "Clear Form",
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }

    val saveButton:
        @Composable (Modifier) -> Unit =
        { buttonModifier ->
            Button(
                onClick = onSave,
                modifier = buttonModifier
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        contentColor =
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                    )
            ) {
                Text(
                    text = "SAVE ITEM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

    if (stackVertically) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            saveButton(Modifier.fillMaxWidth())
            clearButton(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            clearButton(Modifier.weight(1f))
            saveButton(Modifier.weight(1f))
        }
    }
}

@Composable
private fun OriginalEditorField(
    label: String,
    placeholder: String,
    state: TextFieldState,
    optional: Boolean = false,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputTransformation: InputTransformation? = null,
    usePlatformProductNameField: Boolean = false,
    isPlatformFieldReady: Boolean = true,
    keyboardAccessoryAction: KeyboardAccessoryAction =
        KeyboardAccessoryAction.NONE,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                start = 4.dp,
                bottom = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )

            if (optional) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Optional field",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (usePlatformProductNameField) {
            PlatformProductNameTextField(
                state = state,
                placeholder = placeholder,
                onNext = { onImeAction?.invoke() },
                isReadyForInteraction = isPlatformFieldReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            )
        } else {
            val platformKeyboardOptions =
                rememberPlatformTextInputOptions(
                    keyboardOptions = keyboardOptions,
                    accessoryAction = keyboardAccessoryAction,
                    onAccessoryAction = {
                        onImeAction?.invoke()
                    }
                )

            OutlinedTextField(
                state = state,
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
                shape = RoundedCornerShape(12.dp),
                inputTransformation = inputTransformation,
                keyboardOptions = platformKeyboardOptions,
                onKeyboardAction = if (onImeAction == null) {
                    null
                } else {
                    { onImeAction() }
                },
                lineLimits = TextFieldLineLimits.SingleLine,
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
}

@Composable
private fun RetailerBrowserButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.supremeColors.border
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.supremeColors.panel,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.Language,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(5.dp))

        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = "$label link is optional",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
        )
    }
}
