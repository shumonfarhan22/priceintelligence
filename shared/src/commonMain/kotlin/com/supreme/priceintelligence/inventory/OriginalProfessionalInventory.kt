@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.supreme.priceintelligence.inventory

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.supreme.priceintelligence.dashboard.formatIndianPrice
import com.supreme.priceintelligence.data.InventoryItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ProfessionalInventoryCard: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

private val ProfessionalInventoryBorder: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline

private val ProfessionalInventoryText: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

private val ProfessionalInventoryMuted: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

private val ProfessionalInventoryEmerald: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

private val ProfessionalInventoryBackground: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

private val ProfessionalInventoryDelete: Color
    @Composable
    get() = MaterialTheme.colorScheme.error

@Composable
internal fun ProfessionalInventorySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        placeholder = {
            Text(
                text = "Search inventory...",
                color = ProfessionalInventoryMuted
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = ProfessionalInventoryMuted
            )
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear inventory search",
                        tint = ProfessionalInventoryMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onDone()
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ProfessionalInventoryCard,
            unfocusedContainerColor = ProfessionalInventoryCard,
            disabledContainerColor = ProfessionalInventoryCard,
            focusedBorderColor = ProfessionalInventoryBorder,
            unfocusedBorderColor = ProfessionalInventoryBorder,
            disabledBorderColor = ProfessionalInventoryBorder,
            focusedTextColor = ProfessionalInventoryText,
            unfocusedTextColor = ProfessionalInventoryText,
            disabledTextColor = ProfessionalInventoryMuted,
            cursorColor = ProfessionalInventoryEmerald,
            focusedLeadingIconColor = ProfessionalInventoryMuted,
            unfocusedLeadingIconColor = ProfessionalInventoryMuted,
            focusedTrailingIconColor = ProfessionalInventoryMuted,
            unfocusedTrailingIconColor = ProfessionalInventoryMuted
        )
    )
}

@Composable
internal fun ProfessionalInventoryGroupHeader(
    groupName: String,
    productCount: Int,
    expanded: Boolean,
    reduceMotionEnabled: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reduceMotionEnabled) {
            snap()
        } else {
            tween(durationMillis = 200)
        },
        label = "inventoryGroupChevron"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.55f
                )
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                stateDescription = if (expanded) {
                    "Expanded"
                } else {
                    "Collapsed"
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = groupName,
            color = ProfessionalInventoryEmerald,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = if (productCount == 1) {
                "1 item"
            } else {
                "$productCount items"
            },
            color = ProfessionalInventoryMuted,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) {
                "Collapse $groupName"
            } else {
                "Expand $groupName"
            },
            tint = ProfessionalInventoryMuted,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
        )
    }
}

@Composable
internal fun ProfessionalInventoryProductRow(
    item: InventoryItem,
    selected: Boolean,
    highlighted: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember(item.id) {
        Animatable(0f)
    }
    val density = LocalDensity.current
    val maxSwipe = with(density) {
        -80.dp.toPx()
    }
    val rightEdgeWidth = with(density) {
        56.dp.toPx()
    }

    LaunchedEffect(selectionMode) {
        if (selectionMode && offsetX.value != 0f) {
            offsetX.animateTo(0f)
        }
    }

    val swipeProgress = if (maxSwipe == 0f) {
        0f
    } else {
        (offsetX.value / maxSwipe).coerceIn(0f, 1f)
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (highlighted || selected) {
            ProfessionalInventoryEmerald
        } else {
            ProfessionalInventoryBorder
        },
        animationSpec = tween(durationMillis = 800),
        label = "inventoryCardBorder"
    )

    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (highlighted || selected) {
            ProfessionalInventoryEmerald.copy(alpha = 0.15f)
        } else {
            ProfessionalInventoryCard
        },
        animationSpec = tween(durationMillis = 800),
        label = "inventoryCardBackground"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                ProfessionalInventoryDelete.copy(
                    alpha = 0.20f * swipeProgress
                )
            )
            .border(
                width = 1.dp,
                color = ProfessionalInventoryDelete.copy(
                    alpha = 0.5f * swipeProgress
                ),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        if (!selectionMode && swipeProgress > 0f) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        offsetX.animateTo(0f)
                        onDelete()
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete ${item.productName}",
                    tint = ProfessionalInventoryDelete
                )
            }
        }

        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = offsetX.value.roundToInt(),
                        y = 0
                    )
                }
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(animatedBackgroundColor)
                .border(
                    width = 1.dp,
                    color = animatedBorderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .semantics {
                    this.selected = selected
                    role = Role.Checkbox
                    contentDescription = buildString {
                        append(item.productName)
                        append(", ")
                        append(formatIndianPrice(item.shopPrice))

                        if (selected) {
                            append(", selected")
                        }
                    }
                }
                .pointerInput(selectionMode) {
                    detectTapGestures(
                        onTap = {
                            when {
                                selectionMode -> {
                                    onToggleSelection()
                                }

                                offsetX.value != 0f -> {
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                }
                            }
                        },
                        onLongPress = {
                            if (!selectionMode) {
                                onToggleSelection()
                            }
                        }
                    )
                }
                .pointerInput(selectionMode, maxSwipe) {
                    if (!selectionMode) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    val destination =
                                        if (offsetX.value < maxSwipe / 2f) {
                                            maxSwipe
                                        } else {
                                            0f
                                        }

                                    offsetX.animateTo(destination)
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f)
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                val startedNearRightEdge =
                                    change.position.x >=
                                        size.width - rightEdgeWidth

                                val isOpeningSwipe =
                                    dragAmount < 0f

                                if (
                                    (
                                        startedNearRightEdge &&
                                            isOpeningSwipe
                                    ) ||
                                    offsetX.value < 0f
                                ) {
                                    change.consume()

                                    coroutineScope.launch {
                                        val newOffset =
                                            (offsetX.value + dragAmount)
                                                .coerceIn(
                                                    minimumValue = maxSwipe,
                                                    maximumValue = 0f
                                                )

                                        offsetX.snapTo(newOffset)
                                    }
                                }
                            }
                        )
                    }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = ProfessionalInventoryEmerald,
                        uncheckedColor = ProfessionalInventoryBorder,
                        checkmarkColor = ProfessionalInventoryBackground
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            ProfessionalInventoryProductImage(
                item = item,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.productName,
                    color = ProfessionalInventoryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.size(
                        width = 0.dp,
                        height = 10.dp
                    )
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            width = 1.dp,
                            color = ProfessionalInventoryEmerald.copy(
                                alpha = 0.30f
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sell,
                        contentDescription = null,
                        tint = ProfessionalInventoryEmerald,
                        modifier = Modifier.size(12.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Supreme Price: ${
                            formatIndianPrice(item.shopPrice)
                        }",
                        color = ProfessionalInventoryEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!selectionMode) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit ${item.productName}",
                        tint = ProfessionalInventoryMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfessionalInventoryProductImage(
    item: InventoryItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.productName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentScale = ContentScale.Fit
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = null,
                tint = ProfessionalInventoryMuted
            )
        }
    }
}