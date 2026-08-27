package com.supreme.priceintelligence.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supreme.priceintelligence.settings.AppColorPalette
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.settings.CustomAppColorPalette
import com.supreme.priceintelligence.settings.CustomPaletteRole
import com.supreme.priceintelligence.settings.normalizePaletteHex
import com.supreme.priceintelligence.ui.theme.paletteColorFromHex
import com.supreme.priceintelligence.ui.theme.semanticPalette
import com.supreme.priceintelligence.ui.theme.supremeColors

@Composable
internal fun AppColorPaletteControl(
    customization: AppCustomization,
    onPaletteSelected: (AppColorPalette) -> Unit,
    onEditCustomPalette: () -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "App colour palette",
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val columnCount =
                if (maxWidth >= 248.dp) {
                    2
                } else {
                    1
                }

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                AppColorPalette.entries
                    .chunked(columnCount)
                    .forEach { paletteRow ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    IntrinsicSize.Min
                                ),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            paletteRow.forEach {
                                    palette ->

                                AppColorPaletteTile(
                                    palette = palette,
                                    customization =
                                        customization,
                                    selected =
                                        customization
                                            .appColorPalette ==
                                                palette,
                                    onClick = {
                                        onPaletteSelected(
                                            palette
                                        )

                                        if (
                                            palette ==
                                            AppColorPalette
                                                .CUSTOM
                                        ) {
                                            onEditCustomPalette()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }

                            if (
                                columnCount > 1 &&
                                paletteRow.size == 1
                            ) {
                                Spacer(
                                    modifier =
                                        Modifier.weight(1f)
                                )
                            }
                        }
                    }
            }
        }

        if (
            customization.appColorPalette ==
            AppColorPalette.CUSTOM
        ) {
            OutlinedButton(
                onClick = onEditCustomPalette,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(7.dp))

                Text("Edit custom colours")
            }
        }

        Text(
            text =
                "Colours are automatically adjusted when needed so text and controls remain visible.",
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun AppColorPaletteTile(
    palette: AppColorPalette,
    customization: AppCustomization,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewPalette =
        customization
            .copy(appColorPalette = palette)
            .semanticPalette(
                isDarkTheme =
                    MaterialTheme
                        .supremeColors
                        .isDark
            )

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 94.dp)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
        shape = RoundedCornerShape(14.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
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
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.supremeColors.border
                }
        )
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {
                PalettePreviewBar(
                    previewPalette.primary,
                    Modifier.weight(1f)
                )
                PalettePreviewBar(
                    previewPalette.secondary,
                    Modifier.weight(1f)
                )
                PalettePreviewBar(
                    previewPalette.competitive,
                    Modifier.weight(1f)
                )
                PalettePreviewBar(
                    previewPalette.warning,
                    Modifier.weight(1f)
                )
                PalettePreviewBar(
                    previewPalette.review,
                    Modifier.weight(1f)
                )
            }

            Text(
                text = palette.displayName,
                color =
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurface
                    },
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = palette.description,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PalettePreviewBar(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = color,
                shape = RoundedCornerShape(50)
            )
    )
}

@Composable
internal fun CustomAppColorPaletteDialog(
    initialPalette: CustomAppColorPalette,
    onApply: (CustomAppColorPalette) -> Unit,
    onDismiss: () -> Unit
) {
    var workingPalette by remember(
        initialPalette
    ) {
        mutableStateOf(initialPalette)
    }

    var selectedRole by remember {
        mutableStateOf(
            CustomPaletteRole.PRIMARY
        )
    }

    var hexInput by remember {
        mutableStateOf(
            initialPalette.primaryHex
        )
    }

    val normalizedHex =
        normalizePaletteHex(hexInput)

    fun selectRole(
        role: CustomPaletteRole
    ) {
        selectedRole = role
        hexInput =
            workingPalette.hexFor(role)
    }

    fun selectColor(
        hex: String
    ) {
        val normalized =
            normalizePaletteHex(hex)
                ?: return

        workingPalette =
            workingPalette.withHex(
                role = selectedRole,
                value = normalized
            )

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
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color =
                MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color =
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
                            Icons.Rounded.Palette,
                        contentDescription = null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "CUSTOM COLOUR PALETTE",
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
                                "Choose a colour for each type of app element",
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
                                "Close custom colour editor"
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
                        Arrangement.spacedBy(16.dp)
                ) {
                    CustomPalettePreview(
                        palette = workingPalette
                    )

                    Text(
                        text = "1. Choose what you want to colour",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    CustomPaletteRole.entries
                        .chunked(2)
                        .forEach { roleRow ->
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        8.dp
                                    )
                            ) {
                                roleRow.forEach { role ->
                                    PaletteRoleButton(
                                        role = role,
                                        color =
                                            paletteColorFromHex(
                                                workingPalette
                                                    .hexFor(role),
                                                Color.Transparent
                                            ),
                                        selected =
                                            role == selectedRole,
                                        onClick = {
                                            selectRole(role)
                                        },
                                        modifier =
                                            Modifier.weight(1f)
                                    )
                                }

                                if (roleRow.size == 1) {
                                    Spacer(
                                        modifier =
                                            Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                    Text(
                        text = "2. Choose a colour",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    ColourSwatchMenu(
                        selectedHex =
                            workingPalette.hexFor(
                                selectedRole
                            ),
                        onSelected = ::selectColor
                    )

                    Text(
                        text = "3. Or enter any exact hex colour",
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
                            Text("Hex colour")
                        },
                        placeholder = {
                            Text("#10B981")
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
                                    "Use six characters, for example #10B981"
                                } else {
                                    "${selectedRole.displayName}: $normalizedHex"
                                }
                            )
                        },
                        isError = normalizedHex == null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            normalizedHex?.let(
                                ::selectColor
                            )
                        },
                        enabled = normalizedHex != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector =
                                Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(7.dp))

                        Text(
                            "Use this colour for " +
                                    selectedRole.displayName
                        )
                    }
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onApply(workingPalette)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Palette")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteRoleButton(
    role: CustomPaletteRole,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 68.dp),
        shape = RoundedCornerShape(12.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
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
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.supremeColors.border
                }
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = role.displayName,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                Text(
                    text =
                        if (selected) {
                            "Editing"
                        } else {
                            "Tap to edit"
                        },
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun ColourSwatchMenu(
    selectedHex: String,
    onSelected: (String) -> Unit
) {
    val colours = remember {
        listOf(
            "#EF4444",
            "#F43F5E",
            "#FB7185",
            "#F97316",
            "#F59E0B",
            "#D6A63D",
            "#EAB308",
            "#84CC16",
            "#22C55E",
            "#10B981",
            "#34D399",
            "#14B8A6",
            "#2DD4BF",
            "#06B6D4",
            "#22D3EE",
            "#38BDF8",
            "#3B82F6",
            "#60A5FA",
            "#6366F1",
            "#8B7CF6",
            "#A855F7",
            "#C084FC",
            "#D946EF",
            "#F472B6",
            "#E08A5B",
            "#A16207",
            "#64748B",
            "#334155",
            "#111827",
            "#F8FAFC"
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
                        ) {
                            if (selected) {
                                Box(
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Rounded.Check,
                                        contentDescription =
                                            "Selected colour $hex",
                                        tint =
                                            readableSwatchIconColor(
                                                paletteColorFromHex(
                                                    hex,
                                                    Color.Transparent
                                                )
                                            ),
                                        modifier =
                                            Modifier.size(19.dp)
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
private fun CustomPalettePreview(
    palette: CustomAppColorPalette
) {
    val preview =
        AppCustomization(
            appColorPalette =
                AppColorPalette.CUSTOM,
            customColorPalette = palette
        ).semanticPalette(
            isDarkTheme =
                MaterialTheme
                    .supremeColors
                    .isDark
        )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color =
            MaterialTheme.supremeColors.panelMuted,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.supremeColors.border
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LIVE PALETTE PREVIEW",
                color = preview.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = preview.primary,
                    contentColor = preview.onPrimary
                ),
                contentPadding =
                    PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
            ) {
                Text("Primary action")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                PreviewStatusChip(
                    text = "Highlight",
                    color = preview.secondary,
                    modifier = Modifier.weight(1f)
                )

                PreviewStatusChip(
                    text = "Competitive",
                    color = preview.competitive,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                PreviewStatusChip(
                    text = "Needs check",
                    color = preview.warning,
                    modifier = Modifier.weight(1f)
                )

                PreviewStatusChip(
                    text = "Review",
                    color = preview.review,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PreviewStatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.52f)
        )
    ) {
        Text(
            text = text,
            color = color,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 7.dp
            ),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun readableSwatchIconColor(
    background: Color
): Color {
    val luminance =
        background.red * 0.2126f +
                background.green * 0.7152f +
                background.blue * 0.0722f

    return if (luminance > 0.56f) {
        Color(0xFF111827)
    } else {
        Color.White
    }
}