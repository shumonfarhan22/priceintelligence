package com.supreme.priceintelligence.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supreme.priceintelligence.settings.CustomRetailerChartColors
import com.supreme.priceintelligence.settings.normalizePaletteHex
import com.supreme.priceintelligence.ui.theme.paletteColorFromHex
import com.supreme.priceintelligence.ui.theme.supremeColors

private enum class RetailerGraphColorRole {
    AMAZON,
    FLIPKART
}

@Composable
internal fun CustomRetailerGraphColorsDialog(
    initialColors: CustomRetailerChartColors,
    onApply: (CustomRetailerChartColors) -> Unit,
    onDismiss: () -> Unit
) {
    var workingColors by remember(initialColors) {
        mutableStateOf(initialColors)
    }

    var selectedRole by remember {
        mutableStateOf(
            RetailerGraphColorRole.AMAZON
        )
    }

    var hexInput by remember {
        mutableStateOf(initialColors.amazonHex)
    }

    val normalizedHex =
        normalizePaletteHex(hexInput)

    fun selectRole(
        role: RetailerGraphColorRole
    ) {
        selectedRole = role

        hexInput =
            when (role) {
                RetailerGraphColorRole.AMAZON ->
                    workingColors.amazonHex

                RetailerGraphColorRole.FLIPKART ->
                    workingColors.flipkartHex
            }
    }

    fun useColor(
        value: String
    ) {
        val normalized =
            normalizePaletteHex(value)
                ?: return

        workingColors =
            when (selectedRole) {
                RetailerGraphColorRole.AMAZON ->
                    workingColors.copy(
                        amazonHex = normalized
                    )

                RetailerGraphColorRole.FLIPKART ->
                    workingColors.copy(
                        flipkartHex = normalized
                    )
            }

        hexInput = normalized
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(24.dp),
            color =
                MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.supremeColors.border
            )
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 18.dp,
                            top = 12.dp,
                            end = 8.dp,
                            bottom = 10.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.ShowChart,
                        contentDescription = null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "GRAPH COLOURS",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary,
                            fontSize = 11.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )

                        Text(
                            text =
                                "Amazon and Flipkart line colours",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector =
                                Icons.Rounded.Close,
                            contentDescription =
                                "Close graph colour editor"
                        )
                    }
                }

                HorizontalDivider(
                    color =
                        MaterialTheme.supremeColors.divider
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(15.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        RetailerColorSelector(
                            label = "Amazon",
                            hex =
                                workingColors.amazonHex,
                            selected =
                                selectedRole ==
                                        RetailerGraphColorRole
                                            .AMAZON,
                            onClick = {
                                selectRole(
                                    RetailerGraphColorRole
                                        .AMAZON
                                )
                            },
                            modifier =
                                Modifier.weight(1f)
                        )

                        RetailerColorSelector(
                            label = "Flipkart",
                            hex =
                                workingColors.flipkartHex,
                            selected =
                                selectedRole ==
                                        RetailerGraphColorRole
                                            .FLIPKART,
                            onClick = {
                                selectRole(
                                    RetailerGraphColorRole
                                        .FLIPKART
                                )
                            },
                            modifier =
                                Modifier.weight(1f)
                        )
                    }

                    Text(
                        text =
                            "Choose a suggested colour",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    GraphColourSwatches(
                        selectedHex =
                            when (selectedRole) {
                                RetailerGraphColorRole.AMAZON ->
                                    workingColors.amazonHex

                                RetailerGraphColorRole.FLIPKART ->
                                    workingColors.flipkartHex
                            },
                        onSelected = ::useColor
                    )

                    Text(
                        text =
                            "Or enter any exact hex colour",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { value ->
                            if (value.length <= 7) {
                                hexInput = value
                            }
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                if (
                                    selectedRole ==
                                    RetailerGraphColorRole
                                        .AMAZON
                                ) {
                                    "Amazon hex colour"
                                } else {
                                    "Flipkart hex colour"
                                }
                            )
                        },
                        placeholder = {
                            Text("#FF9900")
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(
                                        color =
                                            paletteColorFromHex(
                                                hexInput,
                                                Color.Transparent
                                            ),
                                        shape = CircleShape
                                    )
                            )
                        },
                        supportingText = {
                            Text(
                                if (normalizedHex == null) {
                                    "Use six characters, for example #FF9900"
                                } else {
                                    normalizedHex
                                }
                            )
                        },
                        isError = normalizedHex == null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            normalizedHex?.let(::useColor)
                        },
                        enabled = normalizedHex != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Use this graph colour")
                    }

                    Text(
                        text =
                            "These colours affect Product Details and Price Movement graphs.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }

                HorizontalDivider(
                    color =
                        MaterialTheme.supremeColors.divider
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onApply(workingColors)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Colours")
                    }
                }
            }
        }
    }
}

@Composable
private fun RetailerColorSelector(
    label: String,
    hex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color =
        paletteColorFromHex(
            hex,
            Color.Transparent
        )

    Surface(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(14.dp),
        color =
            if (selected) {
                color.copy(alpha = 0.15f)
            } else {
                MaterialTheme.supremeColors.panelMuted
            },
        border = BorderStroke(
            width =
                if (selected) {
                    2.dp
                } else {
                    1.dp
                },
            color =
                if (selected) {
                    color
                } else {
                    MaterialTheme.supremeColors.border
                }
        )
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
            )

            Spacer(Modifier.width(8.dp))

            Column {
                Text(
                    text = label,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = hex,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun GraphColourSwatches(
    selectedHex: String,
    onSelected: (String) -> Unit
) {
    val colours = remember {
        listOf(
            "#FF9900",
            "#2874F0",
            "#10B981",
            "#8B7CF6",
            "#E08A5B",
            "#2DD4BF",
            "#D6A63D",
            "#C084FC",
            "#FB7185",
            "#60A5FA",
            "#22D3EE",
            "#A78BFA",
            "#F59E0B",
            "#38BDF8",
            "#F472B6",
            "#14B8A6",
            "#F43F5E",
            "#3B82F6",
            "#A855F7",
            "#22C55E"
        )
    }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        colours
            .chunked(5)
            .forEach { rowColours ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    rowColours.forEach { hex ->
                        val selected =
                            normalizePaletteHex(
                                selectedHex
                            ) ==
                                    normalizePaletteHex(hex)

                        Surface(
                            onClick = {
                                onSelected(hex)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape =
                                RoundedCornerShape(10.dp),
                            color =
                                paletteColorFromHex(
                                    hex,
                                    Color.Transparent
                                ),
                            border = BorderStroke(
                                width =
                                    if (selected) {
                                        3.dp
                                    } else {
                                        1.dp
                                    },
                                color =
                                    if (selected) {
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                    } else {
                                        MaterialTheme
                                            .supremeColors
                                            .border
                                    }
                            )
                        ) {}
                    }
                }
            }
    }
}