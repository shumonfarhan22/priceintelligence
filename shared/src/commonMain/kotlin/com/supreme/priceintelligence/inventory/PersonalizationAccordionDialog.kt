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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.platform.LocalDensity
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
import com.supreme.priceintelligence.settings.AppColorPalette
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
import com.supreme.priceintelligence.settings.RetailerChartPalette
import com.supreme.priceintelligence.settings.PriorityProductLimit
import com.supreme.priceintelligence.settings.PriorityRowStyle
import com.supreme.priceintelligence.settings.PrioritySortMode
import com.supreme.priceintelligence.settings.SavedColorPreset
import com.supreme.priceintelligence.settings.SavedPersonalizationPreset
import com.supreme.priceintelligence.settings.SectionStartState
import com.supreme.priceintelligence.settings.readAppCustomization
import com.supreme.priceintelligence.settings.writeAppCustomization
import com.supreme.priceintelligence.settings.matchingPersonalizationPreset
import com.supreme.priceintelligence.settings.personalizationForPreset
import com.supreme.priceintelligence.ui.layout.adaptiveLayoutPolicy
import com.supreme.priceintelligence.ui.theme.retailerChartColors
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
        mutableStateOf<String?>(null)
    }

    var customPaletteEditorOpen by rememberSaveable {
        mutableStateOf(false)
    }

    var customRetailerColorsEditorOpen by
        rememberSaveable {
            mutableStateOf(false)
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
                                val builtInPreset =
                                    personalizationForPreset(
                                        preset
                                    )

                                onCustomizationChanged(
                                    builtInPreset.copy(
                                        savedColorPreset =
                                            customization
                                                .savedColorPreset,
                                        savedPersonalizationPreset =
                                            customization
                                                .savedPersonalizationPreset
                                    )
                                )

                                if (preset == PersonalizationPreset.ANALYST) {
                                    onAdvancedModeChanged(true)
                                }
                            }
                        )
                    }

                    item {
                        SavedPresetSection(
                            hasSavedColors =
                                customization
                                    .savedColorPreset != null,
                            hasSavedFullSetup =
                                customization
                                    .savedPersonalizationPreset !=
                                    null,
                            onSaveColors = {
                                onCustomizationChanged(
                                    customization.copy(
                                        savedColorPreset =
                                            SavedColorPreset(
                                                appColorPalette =
                                                    customization
                                                        .appColorPalette,
                                                customColorPalette =
                                                    customization
                                                        .customColorPalette,
                                                retailerChartPalette =
                                                    insight
                                                        .retailerChartPalette,
                                                customRetailerChartColors =
                                                    insight
                                                        .customRetailerChartColors
                                            )
                                    )
                                )
                            },
                            onRestoreColors = {
                                customization
                                    .savedColorPreset
                                    ?.let { preset ->
                                        onCustomizationChanged(
                                            customization.copy(
                                                appColorPalette =
                                                    preset
                                                        .appColorPalette,
                                                customColorPalette =
                                                    preset
                                                        .customColorPalette,
                                                insightCustomization =
                                                    insight.copy(
                                                        retailerChartPalette =
                                                            preset
                                                                .retailerChartPalette,
                                                        customRetailerChartColors =
                                                            preset
                                                                .customRetailerChartColors
                                                    )
                                            )
                                        )
                                    }
                            },
                            onSaveFullSetup = {
                                val cleanSnapshot =
                                    customization.copy(
                                        savedColorPreset = null,
                                        savedPersonalizationPreset =
                                            null
                                    )

                                onCustomizationChanged(
                                    customization.copy(
                                        savedPersonalizationPreset =
                                            SavedPersonalizationPreset(
                                                themeMode =
                                                    themeMode,
                                                advancedModeEnabled =
                                                    advancedModeEnabled,
                                                priceChangeNotificationsEnabled =
                                                    priceChangeNotificationsEnabled,
                                                customizationProfile =
                                                    writeAppCustomization(
                                                        cleanSnapshot
                                                    )
                                            )
                                    )
                                )
                            },
                            onRestoreFullSetup = {
                                customization
                                    .savedPersonalizationPreset
                                    ?.let { preset ->
                                        val restored =
                                            readAppCustomization(
                                                preset
                                                    .customizationProfile
                                            ).copy(
                                                savedColorPreset =
                                                    customization
                                                        .savedColorPreset,
                                                savedPersonalizationPreset =
                                                    customization
                                                        .savedPersonalizationPreset
                                            )

                                        onThemeModeChanged(
                                            preset.themeMode
                                        )

                                        onAdvancedModeChanged(
                                            preset
                                                .advancedModeEnabled
                                        )

                                        onPriceChangeNotificationsChanged(
                                            preset
                                                .priceChangeNotificationsEnabled
                                        )

                                        onCustomizationChanged(
                                            restored
                                        )
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
                            AppColorPaletteControl(
                                customization =
                                    customization,
                                onPaletteSelected = {
                                        palette ->

                                    onCustomizationChanged(
                                        customization.copy(
                                            appColorPalette =
                                                palette
                                        )
                                    )
                                },
                                onEditCustomPalette = {
                                    customPaletteEditorOpen =
                                        true
                                }
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
                                        appColorPalette =
                                            defaults.appColorPalette,
                                        customColorPalette =
                                            defaults.customColorPalette,
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
                                title = "Retailer colours · Amazon / Flipkart",
                                options = RetailerChartPalette.entries,
                                selected = insight.retailerChartPalette,
                                label = { it.displayName },
                                onSelected = { value ->
                                    updateInsight {
                                        it.copy(
                                            retailerChartPalette =
                                                value
                                        )
                                    }

                                    if (
                                        value ==
                                        RetailerChartPalette.CUSTOM
                                    ) {
                                        customRetailerColorsEditorOpen =
                                            true
                                    }
                                },
                                colourPreview = {
                                    it.retailerChartColors(
                                        insight.customRetailerChartColors
                                    ).amazon
                                },
                                secondaryColourPreview = {
                                    it.retailerChartColors(
                                        insight.customRetailerChartColors
                                    ).flipkart
                                }
                            )

                            if (
                                insight.retailerChartPalette ==
                                RetailerChartPalette.CUSTOM
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        customRetailerColorsEditorOpen =
                                            true
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Edit Amazon / Flipkart hex colours"
                                    )
                                }
                            }

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
                                        graphPointMode = insightDefaults.graphPointMode,
                                        retailerChartPalette =
                                            insightDefaults.retailerChartPalette,
                                        customRetailerChartColors =
                                            insightDefaults.customRetailerChartColors
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

                PersonalizationFooter(
                    onReset = onResetPersonalization,
                    onDone = onDismiss
                )
            }
        }
    }

    if (customRetailerColorsEditorOpen) {
        CustomRetailerGraphColorsDialog(
            initialColors =
                insight.customRetailerChartColors,
            onApply = { updatedColors ->
                updateInsight {
                    it.copy(
                        retailerChartPalette =
                            RetailerChartPalette.CUSTOM,
                        customRetailerChartColors =
                            updatedColors
                    )
                }

                customRetailerColorsEditorOpen =
                    false
            },
            onDismiss = {
                customRetailerColorsEditorOpen =
                    false
            }
        )
    }

    if (customPaletteEditorOpen) {
        CustomAppColorPaletteDialog(
            initialPalette =
                customization.customColorPalette,
            onApply = { updatedPalette ->
                onCustomizationChanged(
                    customization.copy(
                        appColorPalette =
                            AppColorPalette.CUSTOM,
                        customColorPalette =
                            updatedPalette
                    )
                )

                customPaletteEditorOpen = false
            },
            onDismiss = {
                customPaletteEditorOpen = false
            }
        )
    }
}

@Composable
private fun PersonalizationFooter(
    onReset: () -> Unit,
    onDone: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface
            )
            .padding(
                horizontal = 14.dp,
                vertical = 12.dp
            )
    ) {
        val policy = adaptiveLayoutPolicy(
            availableWidthDp = maxWidth.value,
            fontScale =
                LocalDensity.current.fontScale
        )

        val stackButtons =
            policy.shouldStack(
                minimumWidthForRowDp = 330f
            )

        if (stackButtons) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        null,
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Reset all")
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        null,
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Reset all")
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Done")
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                PreviewMetric(
                    "12",
                    "Competitive",
                    MaterialTheme
                        .supremeColors
                        .competitive,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                PreviewMetric(
                    "3",
                    "Need check",
                    MaterialTheme
                        .supremeColors
                        .warning,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                PreviewMetric(
                    "2",
                    "Review",
                    MaterialTheme
                        .colorScheme
                        .error,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun PreviewMetric(value: String, label: String, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(11.dp), color = color.copy(alpha = 0.10f), border = BorderStroke(1.dp, color.copy(alpha = 0.30f))) {
        Column(Modifier.padding(8.dp)) {
            Text(value, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = label,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SavedPresetSection(
    hasSavedColors: Boolean,
    hasSavedFullSetup: Boolean,
    onSaveColors: () -> Unit,
    onRestoreColors: () -> Unit,
    onSaveFullSetup: () -> Unit,
    onRestoreFullSetup: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            1.dp,
            MaterialTheme.supremeColors.border
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "MY SAVED PRESETS",
                color =
                    MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )

            Text(
                text =
                    "Saved presets remain available after Reset All.",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )

            SavedPresetRow(
                title = "Colour preset",
                description =
                    "App palette and retailer graph colours",
                hasSavedPreset = hasSavedColors,
                onSave = onSaveColors,
                onRestore = onRestoreColors
            )

            SavedPresetRow(
                title = "Full setup",
                description =
                    "Theme, fonts, layouts, graphs and alerts",
                hasSavedPreset = hasSavedFullSetup,
                onSave = onSaveFullSetup,
                onRestore = onRestoreFullSetup
            )
        }
    }
}

@Composable
private fun SavedPresetRow(
    title: String,
    description: String,
    hasSavedPreset: Boolean,
    onSave: () -> Unit,
    onRestore: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color =
            MaterialTheme.supremeColors.panelMuted,
        border = BorderStroke(
            1.dp,
            MaterialTheme.supremeColors.border
        )
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
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
                        text = title,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = description,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }

                Text(
                    text =
                        if (hasSavedPreset) {
                            "SAVED"
                        } else {
                            "EMPTY"
                        },
                    color =
                        if (hasSavedPreset) {
                            MaterialTheme
                                .supremeColors
                                .competitive
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (hasSavedPreset) {
                            "Replace"
                        } else {
                            "Save"
                        }
                    )
                }

                Button(
                    onClick = onRestore,
                    enabled = hasSavedPreset,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore")
                }
            }
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
    colourPreview: ((T) -> Color)? = null,
    secondaryColourPreview: ((T) -> Color)? = null
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        ChoiceGrid(
            options = options,
            selected = selected,
            label = label,
            onSelected = onSelected,
            colourPreview = colourPreview,
            secondaryColourPreview =
                secondaryColourPreview
        )
    }
}

@Composable
private fun <T> ChoiceGrid(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    colourPreview: ((T) -> Color)? = null,
    secondaryColourPreview: ((T) -> Color)? = null
) {
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
                Arrangement.spacedBy(7.dp)
        ) {
            options
                .chunked(columnCount)
                .forEach { rowOptions ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {
                        rowOptions.forEach { option ->
                            ChoiceTile(
                                text = label(option),
                                selected =
                                    option == selected,
                                previewColor =
                                    colourPreview
                                        ?.invoke(option),
                                secondaryPreviewColor =
                                    secondaryColourPreview
                                        ?.invoke(option),
                                onClick = {
                                    onSelected(option)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        if (
                            columnCount > 1 &&
                            rowOptions.size == 1
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
}

@Composable
private fun ChoiceTile(
    text: String,
    selected: Boolean,
    previewColor: Color?,
    secondaryPreviewColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val hasColourPreview =
        previewColor != null ||
                secondaryPreviewColor != null

    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
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
                MaterialTheme.supremeColors.panelMuted
            },
        border = BorderStroke(
            width = 1.dp,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.supremeColors.border
                }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                if (hasColourPreview) {
                    Arrangement.Start
                } else {
                    Arrangement.Center
                }
        ) {
            if (hasColourPreview) {
                Box(
                    modifier = Modifier
                        .width(
                            if (
                                secondaryPreviewColor !=
                                null
                            ) {
                                34.dp
                            } else {
                                20.dp
                            }
                        )
                        .height(20.dp)
                ) {
                    previewColor?.let { colour ->
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .align(
                                    Alignment.CenterStart
                                )
                                .clip(CircleShape)
                                .background(colour)
                        )
                    }

                    secondaryPreviewColor
                        ?.let { colour ->
                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .align(
                                        Alignment.CenterEnd
                                    )
                                    .clip(CircleShape)
                                    .background(colour)
                            )
                        }
                }

                Spacer(modifier = Modifier.width(7.dp))
            }

            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color =
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    if (hasColourPreview) {
                        Modifier.weight(1f)
                    } else {
                        Modifier
                    }
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

private fun appearanceSummary(
    themeMode: AppThemeMode,
    customization: AppCustomization
): String =
    "${themeMode.displayLabel()} • " +
        "${customization.appColorPalette.displayName} • " +
        "${customization.fontStyle.displayName} • " +
        customization.textSize.displayName

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