package com.supreme.priceintelligence.inventory

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supreme.priceintelligence.network.Retailer
import com.supreme.priceintelligence.network.normalizeRetailerUrl
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal enum class PriceEditorField(
    val displayName: String
) {
    PURCHASE_COST("Purchase Cost"),
    SELLING_PRICE("Selling Price")
}

internal enum class RetailerBrowserSite(
    val displayName: String,
    val startUrl: String,
    val retailer: Retailer
) {
    AMAZON(
        displayName = "Amazon India",
        startUrl = "https://www.amazon.in/",
        retailer = Retailer.AMAZON
    ),
    FLIPKART(
        displayName = "Flipkart",
        startUrl = "https://www.flipkart.com/",
        retailer = Retailer.FLIPKART
    )
}

@Composable
internal fun ProductPriceCalculatorDialog(
    initialField: PriceEditorField,
    purchaseCost: String,
    sellingPrice: String,
    onApply: (PriceEditorField, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFieldName by rememberSaveable {
        mutableStateOf(initialField.name)
    }

    val selectedField = PriceEditorField.entries
        .firstOrNull { field ->
            field.name == selectedFieldName
        }
        ?: PriceEditorField.PURCHASE_COST

    fun valueFor(field: PriceEditorField): String =
        when (field) {
            PriceEditorField.PURCHASE_COST ->
                purchaseCost

            PriceEditorField.SELLING_PRICE ->
                sellingPrice
        }

    var expression by rememberSaveable {
        mutableStateOf(
            valueFor(initialField).ifBlank { "0" }
        )
    }

    var errorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    fun selectField(field: PriceEditorField) {
        selectedFieldName = field.name
        expression = valueFor(field).ifBlank { "0" }
        errorMessage = null
    }

    fun calculate(): Double? {
        val result = evaluatePriceExpression(expression)

        val value = result.getOrElse { error ->
            errorMessage =
                error.message ?: "Check the calculation"
            return null
        }

        if (value <= 0.0) {
            errorMessage =
                "Price must be greater than zero"
            return null
        }

        errorMessage = null
        return value
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.supremeColors.scrim.copy(
                        alpha = 0.78f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(24.dp),
                color =
                    MaterialTheme.supremeColors.panelStrong,
                border = BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme.supremeColors.border
                ),
                shadowElevation =
                    if (MaterialTheme.supremeColors.isDark) {
                        0.dp
                    } else {
                        16.dp
                    }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "PRICE CALCULATOR",
                                color =
                                    MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )

                            Text(
                                text =
                                    "Calculate without leaving the product form",
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Rounded.Close,
                                contentDescription =
                                    "Close calculator"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        PriceFieldSelector(
                            label = "Purchase Cost",
                            value = purchaseCost,
                            selected =
                                selectedField ==
                                        PriceEditorField.PURCHASE_COST,
                            onClick = {
                                selectField(
                                    PriceEditorField.PURCHASE_COST
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        PriceFieldSelector(
                            label = "Selling Price",
                            value = sellingPrice,
                            selected =
                                selectedField ==
                                        PriceEditorField.SELLING_PRICE,
                            onClick = {
                                selectField(
                                    PriceEditorField.SELLING_PRICE
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp),
                        shape = RoundedCornerShape(16.dp),
                        color =
                            MaterialTheme.supremeColors.field,
                        border = BorderStroke(
                            width = 1.dp,
                            color =
                                MaterialTheme.supremeColors.border
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(
                                horizontal = 16.dp
                            ),
                            contentAlignment =
                                Alignment.CenterEnd
                        ) {
                            Text(
                                text =
                                    expression.ifBlank { "0" },
                                modifier =
                                    Modifier.fillMaxWidth(),
                                color =
                                    MaterialTheme.colorScheme.onSurface,
                                fontSize = 27.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }
                    }

                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            color =
                                MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }

                    CalculatorKeypad(
                        onKey = { key ->
                            when (key) {
                                "AC" -> {
                                    expression = "0"
                                }

                                "BACKSPACE" -> {
                                    expression = expression
                                        .dropLast(1)
                                        .ifBlank { "0" }
                                }

                                "=" -> {
                                    calculate()?.let { value ->
                                        expression =
                                            formatCalculatorValue(
                                                value
                                            )
                                    }
                                }

                                else -> {
                                    expression =
                                        appendCalculatorKey(
                                            expression =
                                                expression,
                                            key = key
                                        )
                                }
                            }

                            errorMessage = null
                        }
                    )

                    Button(
                        onClick = {
                            calculate()?.let { value ->
                                onApply(
                                    selectedField,
                                    formatCalculatorValue(
                                        value
                                    )
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector =
                                Icons.Rounded.Check,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                "Use result for ${selectedField.displayName}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceFieldSelector(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(
            onClick = onClick
        ),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(
                alpha = 0.14f
            )
        } else {
            MaterialTheme.supremeColors.panelMuted
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.supremeColors.border
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (value.isBlank()) {
                    "₹ 0"
                } else {
                    "₹ $value"
                },
                color =
                    MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CalculatorKeypad(
    onKey: (String) -> Unit
) {
    val keys = listOf(
        "AC",
        "BACKSPACE",
        "%",
        "÷",
        "7",
        "8",
        "9",
        "×",
        "4",
        "5",
        "6",
        "-",
        "1",
        "2",
        "3",
        "+",
        "00",
        "0",
        ".",
        "="
    )

    Column(
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        keys.chunked(4).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    val isOperator = key in setOf(
                        "%",
                        "÷",
                        "×",
                        "-",
                        "+",
                        "="
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clickable {
                                onKey(key)
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            key == "=" ->
                                MaterialTheme.colorScheme.primary

                            isOperator ->
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.14f
                                )

                            else ->
                                MaterialTheme.supremeColors.panelMuted
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isOperator) {
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.38f
                                )
                            } else {
                                MaterialTheme.supremeColors.border
                            }
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "BACKSPACE") {
                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription =
                                        "Delete last calculator character",
                                    modifier =
                                        Modifier.size(21.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    color = if (key == "=") {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontSize = 18.sp,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun appendCalculatorKey(
    expression: String,
    key: String
): String {
    val current = expression.ifBlank { "0" }

    val operators = setOf(
        '+',
        '-',
        '×',
        '÷'
    )

    if (key.firstOrNull() in operators) {
        if (current.lastOrNull() in operators) {
            return current.dropLast(1) + key
        }

        return current + key
    }

    if (key == ".") {
        val activeNumber = current.takeLastWhile {
                character ->
            character !in operators &&
                    character != '%'
        }

        if ('.' in activeNumber) {
            return current
        }
    }

    return if (
        current == "0" &&
        key.firstOrNull()?.isDigit() == true
    ) {
        key
    } else {
        current + key
    }
}

@Composable
internal fun RetailerBrowserDialog(
    site: RetailerBrowserSite,
    onUseLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentUrl by remember(site) {
        mutableStateOf(site.startUrl)
    }

    var isLoading by remember(site) {
        mutableStateOf(true)
    }

    val entryProgress = remember(site) {
        Animatable(0f)
    }

    val settleOffset = remember(site) {
        Animatable(0f)
    }

    var dragOffsetPx by remember(site) {
        mutableFloatStateOf(0f)
    }

    var isDragging by remember(site) {
        mutableStateOf(false)
    }

    var dismissing by remember(site) {
        mutableStateOf(false)
    }

    val animationScope = rememberCoroutineScope()

    LaunchedEffect(site) {
        entryProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 280
            )
        )
    }

    val acceptedUrl = remember(
        currentUrl,
        site
    ) {
        normalizeRetailerUrl(
            value = currentUrl,
            retailer = site.retailer
        )
    }

    fun closeWithAnimation() {
        if (dismissing) return

        dismissing = true
        animationScope.launch {
            entryProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 210
                )
            )
            onDismiss()
        }
    }

    fun useLinkWithAnimation(url: String) {
        if (dismissing) return

        dismissing = true
        animationScope.launch {
            entryProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 210
                )
            )
            onUseLink(url)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
    ) {
        val density = LocalDensity.current
        val browserHeightPx = with(density) {
            maxHeight.toPx()
        }.coerceAtLeast(1f)

        val dismissThresholdPx =
            browserHeightPx * 0.42f

            val visibleDragOffset = if (isDragging) {
                dragOffsetPx
            } else {
                settleOffset.value
            }

            val openingOffset =
                browserHeightPx *
                    (1f - entryProgress.value)

            val totalOffset =
                (openingOffset + visibleDragOffset)
                    .coerceIn(0f, browserHeightPx)

            val dragFraction =
                (visibleDragOffset / browserHeightPx)
                    .coerceIn(0f, 1f)

            val headerDragState =
                rememberDraggableState { delta ->
                    dragOffsetPx =
                        (dragOffsetPx + delta)
                            .coerceIn(
                                minimumValue = 0f,
                                maximumValue =
                                    browserHeightPx
                            )
                }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.supremeColors.scrim.copy(
                            alpha =
                                0.62f *
                                    entryProgress.value *
                                    (1f - dragFraction)
                        )
                    )
            ) {
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            x = 0,
                            y = totalOffset.roundToInt()
                        )
                    },
                color =
                    MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .draggable(
                                state = headerDragState,
                                orientation =
                                    Orientation.Vertical,
                                enabled = !dismissing,
                                onDragStarted = {
                                    settleOffset.stop()
                                    dragOffsetPx =
                                        settleOffset.value
                                    isDragging = true
                                },
                                onDragStopped = { velocity ->
                                    if (!dismissing) {
                                        val releasedOffset =
                                            dragOffsetPx
                                        isDragging = false
                                        dragOffsetPx = 0f
                                        settleOffset.snapTo(
                                            releasedOffset
                                        )

                                        val shouldDismiss =
                                            releasedOffset >=
                                                dismissThresholdPx ||
                                                velocity >= 1_400f

                                        if (shouldDismiss) {
                                            dismissing = true
                                            settleOffset.animateTo(
                                                targetValue =
                                                    browserHeightPx,
                                                animationSpec = tween(
                                                    durationMillis = 190
                                                )
                                            )
                                            onDismiss()
                                        } else {
                                            settleOffset.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio =
                                                        Spring.DampingRatioNoBouncy,
                                                    stiffness =
                                                        Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    }
                                }
                            ),
                        color =
                            MaterialTheme.supremeColors.panelStrong,
                        border = BorderStroke(
                            width = 1.dp,
                            color =
                                MaterialTheme.supremeColors.border
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp),
                                contentAlignment =
                                    Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(4.dp)
                                        .background(
                                            color =
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant
                                                    .copy(alpha = 0.55f),
                                            shape =
                                                RoundedCornerShape(50)
                                        )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 5.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        closeWithAnimation()
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Rounded.Close,
                                        contentDescription =
                                            "Close ${site.displayName} browser"
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = site.displayName,
                                        color =
                                            MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        text =
                                            "INCOGNITO  •  $currentUrl",
                                        color =
                                            MaterialTheme.colorScheme.primary,
                                        fontSize = 9.sp,
                                        fontWeight =
                                            FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow =
                                            TextOverflow.Ellipsis
                                    )
                                }

                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier =
                                            Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.width(6.dp)
                                    )
                                }

                                IconButton(
                                    enabled =
                                        acceptedUrl != null &&
                                            !dismissing,
                                    onClick = {
                                        acceptedUrl?.let { url ->
                                            useLinkWithAnimation(
                                                url
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Rounded.Check,
                                        contentDescription =
                                            "Use this ${site.displayName} link",
                                        tint = if (
                                            acceptedUrl != null
                                        ) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                                .copy(alpha = 0.4f)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    PlatformRetailerWebView(
                        initialUrl = site.startUrl,
                        onUrlChanged = { url ->
                            currentUrl = url
                        },
                        onLoadingChanged = { loading ->
                            isLoading = loading
                        },
                        onUseLink = { url ->
                            normalizeRetailerUrl(
                                value = url,
                                retailer = site.retailer
                            )?.let { accepted ->
                                useLinkWithAnimation(accepted)
                            }
                        },
                        onBrowserClosed = {
                            closeWithAnimation()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

@Composable
internal expect fun PlatformRetailerWebView(
    initialUrl: String,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onUseLink: (String) -> Unit,
    onBrowserClosed: () -> Unit,
    modifier: Modifier = Modifier
)