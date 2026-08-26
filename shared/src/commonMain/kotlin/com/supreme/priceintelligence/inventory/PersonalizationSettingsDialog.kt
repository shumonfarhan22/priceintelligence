package com.supreme.priceintelligence.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.supreme.priceintelligence.settings.AppAccentColor
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.settings.AppDisplayDensity
import com.supreme.priceintelligence.settings.AppFontStyle
import com.supreme.priceintelligence.settings.AppMotionPreference
import com.supreme.priceintelligence.settings.AppTextSize
import com.supreme.priceintelligence.settings.AppThemeMode
import com.supreme.priceintelligence.settings.DashboardCardStyle
import com.supreme.priceintelligence.settings.DashboardDefaultSort
import com.supreme.priceintelligence.settings.DashboardPageSize
import com.supreme.priceintelligence.settings.PriceAlertDirection
import com.supreme.priceintelligence.settings.PriceAlertThreshold
import com.supreme.priceintelligence.settings.PriceMovementDefaultRange
import com.supreme.priceintelligence.ui.theme.supremeColors

@Composable
internal fun PersonalizationSettingsDialog(
    themeMode: AppThemeMode,
    customization: AppCustomization,
    advancedModeEnabled: Boolean,
    priceChangeNotificationsEnabled: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onCustomizationChanged: (AppCustomization) -> Unit,
    onAdvancedModeChanged: (Boolean) -> Unit,
    onPriceChangeNotificationsChanged: (Boolean) -> Unit,
    onResetPersonalization: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            ),
            shadowElevation =
                if (MaterialTheme.supremeColors.isDark) {
                    0.dp
                } else {
                    16.dp
                }
        ) {
            Column {
                SettingsHeader(onDismiss = onDismiss)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(
                                start = 18.dp,
                                end = 18.dp,
                                bottom = 18.dp
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        LiveCustomizationPreview()
                    }

                    item {
                        ChoiceSection(
                            title = "Theme",
                            description =
                                "System follows your phone automatically.",
                            options = AppThemeMode.entries,
                            selected = themeMode,
                            label = { option ->
                                when (option) {
                                    AppThemeMode.SYSTEM -> "System"
                                    AppThemeMode.LIGHT -> "Light"
                                    AppThemeMode.DARK -> "Dark"
                                }
                            },
                            onSelected =
                                onThemeModeChanged
                        )
                    }

                    item {
                        AccentSection(
                            selected =
                                customization.accentColor,
                            onSelected = { accent ->
                                onCustomizationChanged(
                                    customization.copy(
                                        accentColor = accent
                                    )
                                )
                            }
                        )
                    }

                    item {
                        ChoiceSection(
                            title = "Font style",
                            description =
                                "Eight offline styles. Native gives the most platform-like appearance.",
                            options = AppFontStyle.entries,
                            selected =
                                customization.fontStyle,
                            label = { it.displayName },
                            onSelected = { fontStyle ->
                                onCustomizationChanged(
                                    customization.copy(
                                        fontStyle = fontStyle
                                    )
                                )
                            }
                        )
                    }

                    item {
                        ChoiceSection(
                            title = "Text size",
                            description =
                                "This is added safely on top of your phone's accessibility text size.",
                            options = AppTextSize.entries,
                            selected = customization.textSize,
                            label = { it.displayName },
                            onSelected = { textSize ->
                                onCustomizationChanged(
                                    customization.copy(
                                        textSize = textSize
                                    )
                                )
                            }
                        )
                    }

                    item {
                        ChoiceSection(
                            title = "Display density",
                            description =
                                "Compact reduces outer screen spacing while keeping touch targets usable.",
                            options =
                                AppDisplayDensity.entries,
                            selected =
                                customization.displayDensity,
                            label = { it.displayName },
                            onSelected = { density ->
                                onCustomizationChanged(
                                    customization.copy(
                                        displayDensity = density
                                    )
                                )
                            }
                        )
                    }

                    item {
                        ChoiceSection(
                            title = "Motion",
                            description =
                                "The phone's Reduce Motion setting always remains respected.",
                            options =
                                AppMotionPreference.entries,
                            selected =
                                customization.motionPreference,
                            label = { it.displayName },
                            onSelected = { motion ->
                                onCustomizationChanged(
                                    customization.copy(
                                        motionPreference = motion
                                    )
                                )
                            }
                        )
                    }

                    item {
                        SettingsSwitchRow(
                            title = "Scan vibration",
                            description =
                                "Vibrate after a barcode is read successfully.",
                            checked =
                                customization.hapticsEnabled,
                            onCheckedChange = { enabled ->
                                onCustomizationChanged(
                                    customization.copy(
                                        hapticsEnabled = enabled
                                    )
                                )
                            }
                        )
                    }

                    item {
                        SettingsSectionTitle(
                            title = "Dashboard",
                            description =
                                "Choose the default layout and starting view."
                        )

                        Spacer(Modifier.height(10.dp))

                        ChoiceGrid(
                            options =
                                DashboardCardStyle.entries,
                            selected =
                                customization.dashboardCardStyle,
                            label = { it.displayName },
                            onSelected = { cardStyle ->
                                onCustomizationChanged(
                                    customization.copy(
                                        dashboardCardStyle =
                                            cardStyle
                                    )
                                )
                            }
                        )

                        Spacer(Modifier.height(14.dp))

                        SettingsSubheading("Default sorting")

                        Spacer(Modifier.height(8.dp))

                        ChoiceGrid(
                            options =
                                DashboardDefaultSort.entries,
                            selected =
                                customization.dashboardDefaultSort,
                            label = { it.displayName },
                            onSelected = { sort ->
                                onCustomizationChanged(
                                    customization.copy(
                                        dashboardDefaultSort = sort
                                    )
                                )
                            }
                        )

                        Spacer(Modifier.height(14.dp))

                        SettingsSubheading("Products per page")

                        Spacer(Modifier.height(8.dp))

                        ChoiceGrid(
                            options =
                                DashboardPageSize.entries,
                            selected =
                                customization.dashboardPageSize,
                            label = { it.displayName },
                            onSelected = { pageSize ->
                                onCustomizationChanged(
                                    customization.copy(
                                        dashboardPageSize = pageSize
                                    )
                                )
                            }
                        )
                    }

                    item {
                        ChoiceSection(
                            title = "Default graph range",
                            description =
                                "A notification still opens its exact change inside the 30-day view.",
                            options =
                                PriceMovementDefaultRange.entries,
                            selected =
                                customization
                                    .priceMovementDefaultRange,
                            label = { it.displayName },
                            onSelected = { range ->
                                onCustomizationChanged(
                                    customization.copy(
                                        priceMovementDefaultRange =
                                            range
                                    )
                                )
                            }
                        )
                    }

                    item {
                        SettingsSwitchRow(
                            title = "Advanced mode",
                            description =
                                "Show price history and extra comparison tools.",
                            checked = advancedModeEnabled,
                            onCheckedChange =
                                onAdvancedModeChanged
                        )
                    }

                    item {
                        SettingsSwitchRow(
                            title = "Price change alerts",
                            description =
                                "Notify when an automatic Amazon or Flipkart check finds a qualifying change.",
                            checked =
                                priceChangeNotificationsEnabled,
                            onCheckedChange =
                                onPriceChangeNotificationsChanged
                        )

                        if (priceChangeNotificationsEnabled) {
                            Spacer(Modifier.height(14.dp))

                            SettingsSubheading("Alert direction")

                            Spacer(Modifier.height(8.dp))

                            ChoiceGrid(
                                options =
                                    PriceAlertDirection.entries,
                                selected =
                                    customization
                                        .priceAlertDirection,
                                label = { it.displayName },
                                onSelected = { direction ->
                                    onCustomizationChanged(
                                        customization.copy(
                                            priceAlertDirection =
                                                direction
                                        )
                                    )
                                }
                            )

                            Spacer(Modifier.height(14.dp))

                            SettingsSubheading("Minimum change")

                            Spacer(Modifier.height(8.dp))

                            ChoiceGrid(
                                options =
                                    PriceAlertThreshold.entries,
                                selected =
                                    customization
                                        .priceAlertThreshold,
                                label = { it.displayName },
                                onSelected = { threshold ->
                                    onCustomizationChanged(
                                        customization.copy(
                                            priceAlertThreshold =
                                                threshold
                                        )
                                    )
                                }
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text =
                                    "Manual checks stay quiet. Every alert still opens the exact product graph.",
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = onResetPersonalization,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Rounded.RestartAlt,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text("Reset to Supreme defaults")
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 18.dp,
                top = 14.dp,
                end = 8.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Personalization",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Make Price Intelligence feel like yours",
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close personalization"
            )
        }
    }
}

@Composable
private fun LiveCustomizationPreview() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.supremeColors.panelMuted,
        border = BorderStroke(
            1.dp,
            MaterialTheme.supremeColors.border
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme
                            .primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sell,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Live preview product",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Supreme Price: ₹3,499",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AccentSection(
    selected: AppAccentColor,
    onSelected: (AppAccentColor) -> Unit
) {
    SettingsSectionTitle(
        title = "Accent colour",
        description =
            "Business red and green remain locked for clear price meaning."
    )

    Spacer(Modifier.height(10.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppAccentColor.entries
            .chunked(2)
            .forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    rowOptions.forEach { option ->
                        val isSelected = option == selected

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelected(option)
                                }
                                .semantics {
                                    role = Role.RadioButton
                                    this.selected = isSelected
                                },
                            shape = RoundedCornerShape(12.dp),
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme
                                        .primaryContainer
                                } else {
                                    MaterialTheme.colorScheme
                                        .surfaceVariant
                                },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
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
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            option.previewColor()
                                        )
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = option.displayName,
                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (rowOptions.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
    }
}

@Composable
private fun <T> ChoiceSection(
    title: String,
    description: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    SettingsSectionTitle(
        title = title,
        description = description
    )

    Spacer(Modifier.height(10.dp))

    ChoiceGrid(
        options = options,
        selected = selected,
        label = label,
        onSelected = onSelected
    )
}

@Composable
private fun <T> ChoiceGrid(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { option ->
                    ChoiceButton(
                        text = label(option),
                        selected = option == selected,
                        onClick = {
                            onSelected(option)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowOptions.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
        shape = RoundedCornerShape(12.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        border = BorderStroke(
            width = 1.dp,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 11.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme
                            .onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                    },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSectionTitle(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = description,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SettingsSubheading(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

private fun AppAccentColor.previewColor(): Color =
    when (this) {
        AppAccentColor.SUPREME -> Color(0xFF10B981)
        AppAccentColor.EMERALD -> Color(0xFF10B981)
        AppAccentColor.GOLD -> Color(0xFFD6A63D)
        AppAccentColor.INDIGO -> Color(0xFF8B7CF6)
        AppAccentColor.OCEAN -> Color(0xFF38BDF8)
        AppAccentColor.TEAL -> Color(0xFF2DD4BF)
        AppAccentColor.SAPPHIRE -> Color(0xFF60A5FA)
        AppAccentColor.AMETHYST -> Color(0xFFC084FC)
        AppAccentColor.COPPER -> Color(0xFFE08A5B)
    }
