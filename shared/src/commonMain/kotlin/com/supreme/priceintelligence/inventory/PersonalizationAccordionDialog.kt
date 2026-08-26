package com.supreme.priceintelligence.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.supreme.priceintelligence.settings.AdvancedInfoLevel
import com.supreme.priceintelligence.settings.AppAccentColor
import com.supreme.priceintelligence.settings.AppContrastMode
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.settings.AppDisplayDensity
import com.supreme.priceintelligence.settings.AppFontStyle
import com.supreme.priceintelligence.settings.AppMotionPreference
import com.supreme.priceintelligence.settings.AppSurfaceStyle
import com.supreme.priceintelligence.settings.AppTextSize
import com.supreme.priceintelligence.settings.AppThemeMode
import com.supreme.priceintelligence.settings.BreakdownLayout
import com.supreme.priceintelligence.settings.BreakdownValueMode
import com.supreme.priceintelligence.settings.DashboardCardStyle
import com.supreme.priceintelligence.settings.DashboardDefaultSort
import com.supreme.priceintelligence.settings.DashboardPageSize
import com.supreme.priceintelligence.settings.GraphPointMode
import com.supreme.priceintelligence.settings.GraphSize
import com.supreme.priceintelligence.settings.HistoryGraphStyle
import com.supreme.priceintelligence.settings.InsightCustomization
import com.supreme.priceintelligence.settings.MovementDefaultRetailer
import com.supreme.priceintelligence.settings.MovementDirectionFilter
import com.supreme.priceintelligence.settings.MovementLayout
import com.supreme.priceintelligence.settings.MovementProductGraphState
import com.supreme.priceintelligence.settings.MovementProductSort
import com.supreme.priceintelligence.settings.PersonalizationPreset
import com.supreme.priceintelligence.settings.PriceAlertDirection
import com.supreme.priceintelligence.settings.PriceAlertThreshold
import com.supreme.priceintelligence.settings.PriceEmphasis
import com.supreme.priceintelligence.settings.PriceHistoryRange
import com.supreme.priceintelligence.settings.PriceMovementDefaultRange
import com.supreme.priceintelligence.settings.PriorityProductLimit
import com.supreme.priceintelligence.settings.PriorityRowStyle
import com.supreme.priceintelligence.settings.PrioritySortMode
import com.supreme.priceintelligence.settings.SectionStartState
import com.supreme.priceintelligence.settings.matchingPersonalizationPreset
import com.supreme.priceintelligence.settings.personalizationForPreset
import com.supreme.priceintelligence.ui.theme.supremeColors

private enum class PersonalizationSection {
    APPEARANCE,
    DASHBOARD,
    SHOP_OVERVIEW,
    TOP_PRIORITIES,
    PRODUCT_DETAILS,
    PRICE_MOVEMENT,
    ALERTS_BEHAVIOUR
}

@Composable
internal fun PersonalizationAccordionDialog(
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
    var expandedSectionName by rememberSaveable {
        mutableStateOf<String?>(PersonalizationSection.APPEARANCE.name)
    }
    val insight = customization.insightCustomization
    val defaults = AppCustomization()
    val insightDefaults = InsightCustomization()

    fun updateInsight(
        transform: (InsightCustomization) -> InsightCustomization
    ) {
        onCustomizationChanged(
            customization.copy(
                insightCustomization = transform(insight)
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.supremeColors.border),
            shadowElevation = if (MaterialTheme.supremeColors.isDark) 0.dp else 16.dp
        ) {
            Column {
                AccordionHeader(onDismiss)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { PersonalizationPreview(customization) }

                    item {
                        PresetSection(
                            selected = matchingPersonalizationPreset(customization),
                            onSelected = { preset ->
                                onCustomizationChanged(personalizationForPreset(preset))
                                if (preset == PersonalizationPreset.ANALYST) {
                                    onAdvancedModeChanged(true)
                                }
                            }
                        )
                    }

                    item {
                        AccordionSectionCard(
                            title = "Appearance & accessibility",
                            summary = appearanceSummary(themeMode, customization),
                            icon = Icons.Rounded.Palette,
                            expanded = expandedSectionName == PersonalizationSection.APPEARANCE.name,
                            onToggle = {
                                expandedSectionName = toggleSection(
                                    expandedSectionName,
                                    PersonalizationSection.APPEARANCE
                                )
                            }
                        ) {
                            ChoiceGroup(
                                "Theme",
                                AppThemeMode.entries,
                                themeMode,
                                { it.displayLabel() },
                                onThemeModeChanged
                            )
                            ChoiceGroup(
                                "Accent colour",
                                AppAccentColor.entries,
                                customization.accentColor,
                                { it.displayName },
                                { onCustomizationChanged(customization.copy(accentColor = it)) },
                                colourPreview = { it.previewColor() }
                            )
                            ChoiceGroup(
                                "Font style",
                                AppFontStyle.entries,
                                customization.fontStyle,
                                { it.displayName },
                                { onCustomizationChanged(customization.copy(fontStyle = it)) }
                            )
                            ChoiceGroup(
                                "Text size",
                                AppTextSize.entries,
                                customization.textSize,
                                { it.displayName },
                                { onCustomizationChanged(customization.copy(textSize = it)) }
                            )
                            ChoiceGroup(
                                "Screen spacing",
                                AppDisplayDensity.entries,
                                customization.displayDensity,
                                { it.displayName },
                                { onCustomizationChanged(customization.copy(displayDensity = it)) }
                            )
                            ChoiceGroup(
                                "Contrast",
                                AppContrastMode.entries,
                                insight.contrastMode,
                                { it.displayName },
                                { value -> updateInsight { it.copy(contrastMode = value) } }
                            )
                            ChoiceGroup(
                                "Surface style",
                                AppSurfaceStyle.entries,
                                insight.surfaceStyle,
                                { it.displayName },
                                { value -> updateInsight { it.copy(surfaceStyle = value) } }
                            )
                            ChoiceGroup(
                                "Price emphasis",
                                PriceEmphasis.entries,
                                insight.priceEmphasis,
                                { it.displayName },
                                { value -> updateInsight { it.copy(priceEmphasis = value) } }
                            )
                            SettingSwitch(
                                "Reduce transparency",
                                "Uses solid surfaces for clearer text and cards.",
                                insight.reduceTransparency
                            ) { value -> updateInsight { it.copy(reduceTransparency = value) } }
                            SectionResetButton {
                                onThemeModeChanged(AppThemeMode.DARK)
                                onCustomizationChanged(
                                    customization.copy(
                                        accentColor = defaults.accentColor,
                                        fontStyle = defaults.fontStyle,
                                        textSize = defaults.textSize,
                                        displayDensity = defaults.displayDensity,
                                        insightCustomization = insight.copy(
                                            contrastMode = insightDefaults.contrastMode,
                                            surfaceStyle = insightDefaults.surfaceStyle,
                                            reduceTransparency = insightDefaults.reduceTransparency,
                                            priceEmphasis = insightDefaults.priceEmphasis
                                        )
                                    )
                                )
                            }
                        }
                    }

                    item {
                        AccordionSectionCard(
                            "Dashboard",
                            "${customization.dashboardCardStyle.displayName} • ${customization.dashboardDefaultSort.displayName} • ${customization.dashboardPageSize.displayName} per page",
                            Icons.Rounded.Dashboard,
                            expandedSectionName == PersonalizationSection.DASHBOARD.name,
                            {
                                expandedSectionName = toggleSection(
                                    expandedSectionName,
                                    PersonalizationSection.DASHBOARD
                                )
                            }
                        ) {
                            ChoiceGroup(
                                "Product card style",
                                DashboardCardStyle.entries,
                                customization.dashboardCardStyle,
                                { it.displayName },
                                { onCustomizationChanged(customization.copy(dashboardCardStyle = it)) }
                            )
                            ChoiceGroup(
                                "Default sorting",
                                DashboardDefaultSort.entries,
                                customization.dashboardDefaultSort,
                                { it.displayName },
                                { onCustomizationChanged(customization.copy(dashboardDefaultSort = it)) }
                            )
                            ChoiceGroup(
                                "Products per page",
                                DashboardPageSize.entries,
                                customization.dashboardPageSize,
                                { it.displayName },
                                { onCustomizationChanged(customization.copy(dashboardPageSize = it)) }
                            )
                            SectionResetButton {
                                onCustomizationChanged(
                                    customization.copy(
                                        dashboardCardStyle = defaults.dashboardCardStyle,
                                        dashboardDefaultSort = defaults.dashboardDefaultSort,
                                        dashboardPageSize = defaults.dashboardPageSize
                                    )
                                )
                            }
                        }
                    }

                    item {
                        AccordionSectionCard(
                            "Shop Overview & breakdown",
                            "${insight.shopOverviewStartState.displayName} • ${insight.breakdownLayout.displayName} • ${insight.breakdownValueMode.displayName}",
                            Icons.Rounded.Storefront,
                            expandedSectionName == PersonalizationSection.SHOP_OVERVIEW.name,
                            {
                                expandedSectionName = toggleSection(
                                    expandedSectionName,
                                    PersonalizationSection.SHOP_OVERVIEW
                                )
                            }
                        ) {
                            ChoiceGroup(
                                "Shop Overview starts",
                                SectionStartState.entries,
                                insight.shopOverviewStartState,
                                { it.displayName },
                                { value -> updateInsight { it.copy(shopOverviewStartState = value) } }
                            )
                            ChoiceGroup(
                                "Breakdown starts",
                                SectionStartState.entries,
                                insight.breakdownStartState,
                                { it.displayName },
                                { value -> updateInsight { it.copy(breakdownStartState = value) } }
                            )
                            ChoiceGroup(
                                "Breakdown appearance",
                                BreakdownLayout.entries,
                                insight.breakdownLayout,
                                { it.displayName },
                                { value -> updateInsight { it.copy(breakdownLayout = value) } }
                            )
                            ChoiceGroup(
                                "Breakdown values",
                                BreakdownValueMode.entries,
                                insight.breakdownValueMode,
                                { it.displayName },
                                { value -> updateInsight { it.copy(breakdownValueMode = value) } }
                            )
                            SectionResetButton {
                                updateInsight {
                                    it.copy(
                                        shopOverviewStartState = insightDefaults.shopOverviewStartState,
                                        breakdownStartState = insightDefaults.breakdownStartState,
                                        breakdownLayout = insightDefaults.breakdownLayout,
                                        breakdownValueMode = insightDefaults.breakdownValueMode
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AccordionSectionCard(
                            "Top Priorities",
                            "${insight.priorityProductLimit.displayName} • ${insight.prioritySortMode.displayName} • ${insight.priorityRowStyle.displayName}",
                            Icons.Rounded.PriorityHigh,
                            expandedSectionName == PersonalizationSection.TOP_PRIORITIES.name,
                            {
                                expandedSectionName = toggleSection(
                                    expandedSectionName,
                                    PersonalizationSection.TOP_PRIORITIES
                                )
                            }
                        ) {
                            ChoiceGroup(
                                "Section starts",
                                SectionStartState.entries,
                                insight.prioritiesStartState,
                                { it.displayName },
                                { value -> updateInsight { it.copy(prioritiesStartState = value) } }
                            )
                            ChoiceGroup(
                                "Products shown",
                                PriorityProductLimit.entries,
                                insight.priorityProductLimit,
                                { it.displayName },
                                { value -> updateInsight { it.copy(priorityProductLimit = value) } }
                            )
                            ChoiceGroup(
                                "Priority ranking",
                                PrioritySortMode.entries,
                                insight.prioritySortMode,
                                { it.displayName },
                                { value -> updateInsight { it.copy(prioritySortMode = value) } }
                            )
                            ChoiceGroup(
                                "Priority row style",
                                PriorityRowStyle.entries,
                                insight.priorityRowStyle,
                                { it.displayName },
                                { value -> updateInsight { it.copy(priorityRowStyle = value) } }
                            )
                            SectionResetButton {
                                updateInsight {
                                    it.copy(
                                        prioritiesStartState = insightDefaults.prioritiesStartState,
                                        priorityProductLimit = insightDefaults.priorityProductLimit,
                                        prioritySortMode = insightDefaults.prioritySortMode,
                                        priorityRowStyle = insightDefaults.priorityRowStyle
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AccordionSectionCard(
                            "Product Details",
                            "${insight.advancedInfoLevel.displayName} • ${insight.priceHistoryRange.displayName} • ${insight.historyGraphStyle.displayName}",
                            Icons.Rounded.Info,
                            expandedSectionName == PersonalizationSection.PRODUCT_DETAILS.name,
                            {
                                expandedSectionName = toggleSection(
                                    expandedSectionName,
                                    PersonalizationSection.PRODUCT_DETAILS
                                )
                            }
                        ) {
                            SettingSwitch(
                                "Advanced mode",
                                "Shows price history and deeper comparison information.",
                                advancedModeEnabled,
                                onAdvancedModeChanged
                            )
                            ChoiceGroup(
                                "Advanced information starts",
                                SectionStartState.entries,
                                insight.advancedInfoStartState,
                                { it.displayName },
                                { value -> updateInsight { it.copy(advancedInfoStartState = value) } }
                            )
                            ChoiceGroup(
                                "Information level",
                                AdvancedInfoLevel.entries,
                                insight.advancedInfoLevel,
                                { it.displayName },
                                { value -> updateInsight { it.copy(advancedInfoLevel = value) } }
                            )
                            ChoiceGroup(
                                "Price-history period",
                                PriceHistoryRange.entries,
                                insight.priceHistoryRange,
                                { it.displayName },
                                { value -> updateInsight { it.copy(priceHistoryRange = value) } }
                            )
                            ChoiceGroup(
                                "Graph appearance",
                                HistoryGraphStyle.entries,
                                insight.historyGraphStyle,
                                { it.displayName },
                                { value -> updateInsight { it.copy(historyGraphStyle = value) } }
                            )
                            ChoiceGroup(
                                "Graph size",
                                GraphSize.entries,
                                insight.graphSize,
                                { it.displayName },
                                { value -> updateInsight { it.copy(graphSize = value) } }
                            )
                            ChoiceGroup(
                                "Point information",
                                GraphPointMode.entries,
                                insight.graphPointMode,
                                { it.displayName },
                                { value -> updateInsight { it.copy(graphPointMode = value) } }
                            )
                            SectionResetButton {
                                updateInsight {
                                    it.copy(
                                        advancedInfoStartState = insightDefaults.advancedInfoStartState,
                                        advancedInfoLevel = insightDefaults.advancedInfoLevel,
                                        priceHistoryRange = insightDefaults.priceHistoryRange,
                                        historyGraphStyle = insightDefaults.historyGraphStyle,
                                        graphSize = insightDefaults.graphSize,
                                        graphPointMode = insightDefaults.graphPointMode
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AccordionSectionCard(
                            "Price Movement",
                            "${customization.priceMovementDefaultRange.displayName} • ${insight.movementDefaultRetailer.displayName} • ${insight.movementProductSort.displayName}",
                            Icons.Rounded.ShowChart,
                            expandedSectionName == PersonalizationSection.PRICE_MOVEMENT.name,
                            {
                                expandedSectionName = toggleSection(
                                    expandedSectionName,
                                    PersonalizationSection.PRICE_MOVEMENT
                                )
                            }
                        ) {
                            ChoiceGroup(
                                "Default period",
                                PriceMovementDefaultRange.entries,
                                customization.priceMovementDefaultRange,
                                { it.displayName },
                                { onCustomizationChanged(customization.copy(priceMovementDefaultRange = it)) }
                            )
                            ChoiceGroup(
                                "Default retailer",
                                MovementDefaultRetailer.entries,
                                insight.movementDefaultRetailer,
                                { it.displayName },
                                { value -> updateInsight { it.copy(movementDefaultRetailer = value) } }
                            )
                            ChoiceGroup(
                                "Opening layout",
                                MovementLayout.entries,
                                insight.movementLayout,
                                { it.displayName },
                                { value -> updateInsight { it.copy(movementLayout = value) } }
                            )
                            ChoiceGroup(
                                "Changed-product sorting",
                                MovementProductSort.entries,
                                insight.movementProductSort,
                                { it.displayName },
                                { value -> updateInsight { it.copy(movementProductSort = value) } }
                            )
                            ChoiceGroup(
                                "Direction filter",
                                MovementDirectionFilter.entries,
                                insight.movementDirectionFilter,
                                { it.displayName },
                                { value -> updateInsight { it.copy(movementDirectionFilter = value) } }
                            )
                            ChoiceGroup(
                                "Product graph style",
                                HistoryGraphStyle.entries,
                                insight.movementGraphStyle,
                                { it.displayName },
                                { value -> updateInsight { it.copy(movementGraphStyle = value) } }
                            )
                            ChoiceGroup(
                                "Product graphs start",
                                MovementProductGraphState.entries,
                                insight.movementProductGraphState,
                                { it.displayName },
                                { value -> updateInsight { it.copy(movementProductGraphState = value) } }
                            )
                            SectionResetButton {
                                onCustomizationChanged(
                                    customization.copy(
                                        priceMovementDefaultRange = defaults.priceMovementDefaultRange,
                                        insightCustomization = insight.copy(
                                            movementDefaultRetailer = insightDefaults.movementDefaultRetailer,
                                            movementLayout = insightDefaults.movementLayout,
                                            movementProductSort = insightDefaults.movementProductSort,
                                            movementDirectionFilter = insightDefaults.movementDirectionFilter,
                                            movementGraphStyle = insightDefaults.movementGraphStyle,
                                            movementProductGraphState = insightDefaults.movementProductGraphState
                                        )
                                    )
                                )
                            }
                        }
                    }

                    item {
                        AccordionSectionCard(
                            "Alerts & behaviour",
                            "${customization.motionPreference.displayName} • Scan vibration ${if (customization.hapticsEnabled) "on" else "off"} • Alerts ${if (priceChangeNotificationsEnabled) "on" else "off"}",
                            Icons.Rounded.Notifications,
                            expandedSectionName == PersonalizationSection.ALERTS_BEHAVIOUR.name,
                            {
                                expandedSectionName = toggleSection(
                                    expandedSectionName,
                                    PersonalizationSection.ALERTS_BEHAVIOUR
                                )
                            }
                        ) {
                            ChoiceGroup(
                                "Motion",
                                AppMotionPreference.entries,
                                customization.motionPreference,
                                { it.displayName },
                                { onCustomizationChanged(customization.copy(motionPreference = it)) }
                            )
                            SettingSwitch(
                                "Scan vibration",
                                "Vibrate after a barcode is read successfully.",
                                customization.hapticsEnabled
                            ) { onCustomizationChanged(customization.copy(hapticsEnabled = it)) }
                            SettingSwitch(
                                "Price change alerts",
                                "Automatic checks notify only for qualifying changes.",
                                priceChangeNotificationsEnabled,
                                onPriceChangeNotificationsChanged
                            )
                            if (priceChangeNotificationsEnabled) {
                                ChoiceGroup(
                                    "Alert direction",
                                    PriceAlertDirection.entries,
                                    customization.priceAlertDirection,
                                    { it.displayName },
                                    { onCustomizationChanged(customization.copy(priceAlertDirection = it)) }
                                )
                                ChoiceGroup(
                                    "Minimum change",
                                    PriceAlertThreshold.entries,
                                    customization.priceAlertThreshold,
                                    { it.displayName },
                                    { onCustomizationChanged(customization.copy(priceAlertThreshold = it)) }
                                )
                                HelpText("Manual checks stay quiet. Notifications still open the exact product graph.")
                            }
                            SectionResetButton {
                                onPriceChangeNotificationsChanged(false)
                                onCustomizationChanged(
                                    customization.copy(
                                        motionPreference = defaults.motionPreference,
                                        hapticsEnabled = defaults.hapticsEnabled,
                                        priceAlertDirection = defaults.priceAlertDirection,
                                        priceAlertThreshold = defaults.priceAlertThreshold
                                    )
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetPersonalization,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset all")
                    }
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccordionHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 12.dp, end = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("PERSONALIZATION", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
            Text("Choose what you see and how it behaves", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, "Close personalization")
        }
    }
}

@Composable
private fun PersonalizationPreview(customization: AppCustomization) {
    val insight = customization.insightCustomization
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.supremeColors.panelMuted,
        border = BorderStroke(1.dp, MaterialTheme.supremeColors.border)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Sell, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("Live preview product", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Supreme Price: ₹3,499",
                        fontSize = 14.sp,
                        fontWeight = if (insight.priceEmphasis == PriceEmphasis.BOLD) FontWeight.ExtraBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PreviewMetric("12", "Competitive", MaterialTheme.supremeColors.competitive, Modifier.weight(1f))
                PreviewMetric("3", "Need check", MaterialTheme.supremeColors.warning, Modifier.weight(1f))
                PreviewMetric("2", "Review", MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PreviewMetric(value: String, label: String, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(11.dp), color = color.copy(alpha = 0.10f), border = BorderStroke(1.dp, color.copy(alpha = 0.30f))) {
        Column(Modifier.padding(8.dp)) {
            Text(value, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 9.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PresetSection(selected: PersonalizationPreset?, onSelected: (PersonalizationPreset) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(1.dp, MaterialTheme.supremeColors.border)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("QUICK PRESETS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.primary)
            Text("Current: ${selected?.displayName ?: "Custom"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceGrid(PersonalizationPreset.entries, selected, { it.displayName }, onSelected)
            selected?.let { HelpText(it.description) }
        }
    }
}

@Composable
private fun AccordionSectionCard(
    title: String,
    summary: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(180), label = "personalizationChevron")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(1.dp, if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.supremeColors.border)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(summary, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    Icons.Rounded.ExpandMore,
                    if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = rotation }
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(110))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun <T> ChoiceGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    colourPreview: ((T) -> Color)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceGrid(options, selected, label, onSelected, colourPreview)
    }
}

@Composable
private fun <T> ChoiceGrid(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    colourPreview: ((T) -> Color)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowOptions.forEach { option ->
                    ChoiceTile(
                        text = label(option),
                        selected = option == selected,
                        previewColor = colourPreview?.invoke(option),
                        onClick = { onSelected(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChoiceTile(
    text: String,
    selected: Boolean,
    previewColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton; this.selected = selected },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.supremeColors.panelMuted,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.supremeColors.border)
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            previewColor?.let {
                Box(Modifier.size(17.dp).clip(CircleShape).background(it))
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun SectionResetButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.RestartAlt, null, Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text("Reset this section")
    }
}

@Composable
private fun HelpText(text: String) {
    Text(text, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun toggleSection(current: String?, target: PersonalizationSection): String? =
    if (current == target.name) null else target.name

private fun AppThemeMode.displayLabel(): String = when (this) {
    AppThemeMode.SYSTEM -> "System"
    AppThemeMode.LIGHT -> "Light"
    AppThemeMode.DARK -> "Dark"
}

private fun appearanceSummary(themeMode: AppThemeMode, customization: AppCustomization): String =
    "${themeMode.displayLabel()} • ${customization.accentColor.displayName} • ${customization.fontStyle.displayName} • ${customization.textSize.displayName}"

private fun AppAccentColor.previewColor(): Color = when (this) {
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