package com.supreme.priceintelligence.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlinx.coroutines.launch
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
import com.supreme.priceintelligence.settings.MAX_SAVED_PERSONALIZATION_PRESETS
import com.supreme.priceintelligence.settings.MAX_SAVED_PRESET_NAME_LENGTH
import com.supreme.priceintelligence.settings.BreakdownLayout
import com.supreme.priceintelligence.settings.BreakdownValueMode
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
import com.supreme.priceintelligence.dashboard.DashboardViewModel
import com.supreme.priceintelligence.settings.matchingPersonalizationPreset
import com.supreme.priceintelligence.settings.personalizationForPreset
import com.supreme.priceintelligence.ui.layout.adaptiveLayoutPolicy
import com.supreme.priceintelligence.ui.theme.retailerChartColors
import com.supreme.priceintelligence.ui.theme.supremeColors
import com.supreme.priceintelligence.ui.theme.tintedSurface

internal enum class PersonalizationSection {
    APPEARANCE,
    QUICK_COMPARE,
    SHOP_SUMMARY,
    PRODUCT_DETAILS,
    PRICE_MOVEMENT,
    ALERTS_BEHAVIOUR
}

@Composable
internal fun PersonalizationAccordionDialog(
    dashboardViewModel: DashboardViewModel,
    themeMode: AppThemeMode,
    customization: AppCustomization,
    advancedModeEnabled: Boolean,
    priceChangeNotificationsEnabled: Boolean,
    reduceMotionEnabled: Boolean = false,
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

    var previewTargetName by rememberSaveable {
        mutableStateOf(
            PersonalizationPreviewTarget.LAUNCH_HUB.name
        )
    }

    var customPaletteEditorOpen by rememberSaveable {
        mutableStateOf(false)
    }

    val insight = customization.insightCustomization
    val defaults = AppCustomization()
    val insightDefaults = InsightCustomization()

    val previewTarget =
        PersonalizationPreviewTarget.entries
            .firstOrNull { target ->
                target.name == previewTargetName
            }
            ?: PersonalizationPreviewTarget.LAUNCH_HUB

    val selectedSection =
        PersonalizationSection.entries
            .firstOrNull { section ->
                section.name == expandedSectionName
            }

    val activeSetupName =
        activePersonalizationSetupName(
            presets =
                customization.savedPersonalizationPresets,
            themeMode = themeMode,
            advancedModeEnabled =
                advancedModeEnabled,
            notificationsEnabled =
                priceChangeNotificationsEnabled,
            customization = customization
        )

    fun buildCurrentSetup(
        setupName: String
    ): SavedPersonalizationPreset {
        val cleanCustomization =
            customization.withoutSavedSetupData()

        return SavedPersonalizationPreset(
            name =
                setupName
                    .trim()
                    .take(MAX_SAVED_PRESET_NAME_LENGTH)
                    .ifBlank { "Saved setup" },
            themeMode = themeMode,
            advancedModeEnabled =
                advancedModeEnabled,
            priceChangeNotificationsEnabled =
                priceChangeNotificationsEnabled,
            customizationProfile =
                writeAppCustomization(
                    cleanCustomization
                )
        )
    }

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
                .fillMaxHeight(0.97f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.supremeColors.border),
            shadowElevation = if (MaterialTheme.supremeColors.isDark) 0.dp else 16.dp
        ) {
            Column {
                AccordionHeader(onDismiss)

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val availableHeight = maxHeight
                    val editorOpen =
                        selectedSection != null

                    val previewHeightFraction =
                        if (editorOpen) 0.58f else 1f

                    val previewLayerAlpha = remember {
                        Animatable(1f)
                    }

                    var previewTransitionRunning by remember {
                        mutableStateOf(false)
                    }

                    val previewTransitionScope =
                        rememberCoroutineScope()

                    val changeEditor: (String?) -> Unit =
                        { targetSectionName ->
                            if (
                                !previewTransitionRunning &&
                                expandedSectionName !=
                                    targetSectionName
                            ) {
                                if (reduceMotionEnabled) {
                                    expandedSectionName =
                                        targetSectionName
                                } else {
                                    previewTransitionRunning =
                                        true

                                    previewTransitionScope.launch {
                                        try {
                                            previewLayerAlpha
                                                .animateTo(
                                                    targetValue =
                                                        0f,
                                                    animationSpec =
                                                        tween(
                                                            durationMillis =
                                                                85,
                                                            easing =
                                                                FastOutLinearInEasing
                                                        )
                                                )

                                            expandedSectionName =
                                                targetSectionName

                                            withFrameNanos { }

                                            previewLayerAlpha
                                                .animateTo(
                                                    targetValue =
                                                        1f,
                                                    animationSpec =
                                                        tween(
                                                            durationMillis =
                                                                155,
                                                            easing =
                                                                LinearOutSlowInEasing
                                                        )
                                                )
                                        } finally {
                                            previewLayerAlpha
                                                .snapTo(1f)
                                            previewTransitionRunning =
                                                false
                                        }
                                    }
                                }
                            }
                        }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AdaptivePersonalizationPreview(
                            dashboardViewModel =
                                dashboardViewModel,
                            customization = customization,
                            priceChangeNotificationsEnabled =
                                priceChangeNotificationsEnabled,
                            target = previewTarget,
                            selectedSection =
                                selectedSection,
                            reduceMotionEnabled =
                                reduceMotionEnabled,
                            onSectionSelected = { section ->
                                changeEditor(section.name)
                            },
                            onTargetSelected = { target ->
                                previewTargetName = target.name
                                expandedSectionName = null
                            },
                            modifier = Modifier
                                .height(
                                    availableHeight *
                                        previewHeightFraction
                                )
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha =
                                        previewLayerAlpha.value
                                }
                                .padding(
                                    start = 12.dp,
                                    end = 12.dp
                                )
                        )

                        AnimatedVisibility(
                            visible = editorOpen,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis =
                                        if (
                                            reduceMotionEnabled
                                        ) {
                                            0
                                        } else {
                                            190
                                        },
                                    delayMillis =
                                        if (
                                            reduceMotionEnabled
                                        ) {
                                            0
                                        } else {
                                            70
                                        }
                                )
                            ),
                            exit = fadeOut(
                                animationSpec = tween(
                                    durationMillis =
                                        if (
                                            reduceMotionEnabled
                                        ) {
                                            0
                                        } else {
                                            130
                                        }
                                )
                            )
                        ) {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize()
                            ) {
                                LazyColumn(
                                    modifier =
                                        Modifier.fillMaxSize(),
                                    contentPadding =
                                        PaddingValues(
                                            start = 14.dp,
                                            top = 8.dp,
                                            end = 14.dp,
                                            bottom = 16.dp
                                    ),
                                    verticalArrangement =
                                        Arrangement.Top
                                ) {
                    item {
                        AccordionSectionCard(
                            title = "Appearance",
                            summary = appearanceSummary(themeMode, customization),
                            icon = Icons.Rounded.Palette,
                            expanded = expandedSectionName == PersonalizationSection.APPEARANCE.name,
                            onToggle = {
                                changeEditor(null)
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
                                customEditorExpanded =
                                    customPaletteEditorOpen,
                                onPaletteSelected = {
                                        palette ->

                                    if (
                                        palette !=
                                            AppColorPalette.CUSTOM
                                    ) {
                                        customPaletteEditorOpen =
                                            false
                                    }

                                    onCustomizationChanged(
                                        customization.copy(
                                            appColorPalette =
                                                palette
                                        )
                                    )
                                },
                                onEditCustomPalette = {
                                    customPaletteEditorOpen =
                                        !customPaletteEditorOpen
                                }
                            )

                            if (
                                customization.appColorPalette ==
                                    AppColorPalette.CUSTOM &&
                                customPaletteEditorOpen
                            ) {
                                InlineCustomAppColorPaletteEditor(
                                    palette =
                                        customization
                                            .customColorPalette,
                                    onPaletteChanged = {
                                            updatedPalette ->

                                        onCustomizationChanged(
                                            customization.copy(
                                                appColorPalette =
                                                    AppColorPalette.CUSTOM,
                                                customColorPalette =
                                                    updatedPalette
                                            )
                                        )
                                    }
                                )
                            }

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
                                "Contrast",
                                AppContrastMode.entries,
                                insight.contrastMode,
                                { it.displayName },
                                { value -> updateInsight { it.copy(contrastMode = value) } }
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
                            "Quick Compare",
                            "${customization.dashboardDefaultSort.displayName} • ${customization.dashboardPageSize.displayName} products per page",
                            Icons.Rounded.Search,
                            expandedSectionName == PersonalizationSection.QUICK_COMPARE.name,
                            {
                                changeEditor(null)
                            }
                        ) {
                            HelpText(
                                "These options control the product results shown in Quick Compare."
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
                                        dashboardDefaultSort = defaults.dashboardDefaultSort,
                                        dashboardPageSize = defaults.dashboardPageSize
                                    )
                                )
                            }
                        }
                    }

                    item {
                        AccordionSectionCard(
                            "Shop Summary",
                            "${insight.priorityProductLimit.displayName} • ${insight.prioritySortMode.displayName} • ${insight.priorityRowStyle.displayName}",
                            Icons.Rounded.Storefront,
                            expandedSectionName == PersonalizationSection.SHOP_SUMMARY.name,
                            {
                                changeEditor(null)
                            }
                        ) {
                            HelpText(
                                "Controls the Top Priorities section shown on the Launch Hub."
                            )
                            ChoiceGroup(
                                "Top Priorities starts",
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
                                        shopOverviewStartState = insightDefaults.shopOverviewStartState,
                                        breakdownStartState = insightDefaults.breakdownStartState,
                                        breakdownLayout = insightDefaults.breakdownLayout,
                                        breakdownValueMode = insightDefaults.breakdownValueMode,
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
                            "Product Details & analysis",
                            "${insight.advancedInfoLevel.displayName} • ${insight.priceHistoryRange.displayName} • ${insight.historyGraphStyle.displayName}",
                            Icons.Rounded.Info,
                            expandedSectionName == PersonalizationSection.PRODUCT_DETAILS.name,
                            {
                                changeEditor(null)
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
                                changeEditor(null)
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
                                "Changed-product sorting",
                                MovementProductSort.entries,
                                insight.movementProductSort,
                                { it.displayName },
                                { value -> updateInsight { it.copy(movementProductSort = value) } }
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
                            "Alerts & automatic checks",
                            "Daily checks ${if (customization.automaticPriceChecksEnabled) "on" else "off"} • Alerts ${if (priceChangeNotificationsEnabled) "on" else "off"}",
                            Icons.Rounded.Notifications,
                            expandedSectionName == PersonalizationSection.ALERTS_BEHAVIOUR.name,
                            {
                                changeEditor(null)
                            }
                        ) {
                            SettingSwitch(
                                "Automatic daily price checks",
                                "Checks each linked product at most once per day and keeps the rolling price history useful.",
                                customization.automaticPriceChecksEnabled
                            ) { value ->
                                onCustomizationChanged(
                                    customization.copy(
                                        automaticPriceChecksEnabled = value
                                    )
                                )
                            }
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
                                        automaticPriceChecksEnabled =
                                            defaults.automaticPriceChecksEnabled,
                                        priceAlertDirection = defaults.priceAlertDirection,
                                        priceAlertThreshold = defaults.priceAlertThreshold
                                    )
                                )
                            }
                        }
                    }

                    if (
                        selectedSection ==
                            PersonalizationSection.APPEARANCE
                    ) {
                        item {
                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            NamedPersonalizationSetupsSection(
                            presets =
                                customization
                                    .savedPersonalizationPresets,
                            activePresetName =
                                activeSetupName,
                            onSaveNew = { enteredName ->
                                val safeName =
                                    enteredName
                                        .trim()
                                        .take(
                                            MAX_SAVED_PRESET_NAME_LENGTH
                                        )

                                val nameAlreadyExists =
                                    customization
                                        .savedPersonalizationPresets
                                        .any { preset ->
                                            preset.name.equals(
                                                safeName,
                                                ignoreCase = true
                                            )
                                        }

                                if (
                                    safeName.isNotBlank() &&
                                    !nameAlreadyExists &&
                                    customization
                                        .savedPersonalizationPresets
                                        .size <
                                    MAX_SAVED_PERSONALIZATION_PRESETS
                                ) {
                                    onCustomizationChanged(
                                        customization.copy(
                                            savedPersonalizationPresets =
                                                customization
                                                    .savedPersonalizationPresets +
                                                        buildCurrentSetup(
                                                            safeName
                                                        )
                                        )
                                    )
                                }
                            },
                            onApply = { preset ->
                                val restoredCustomization =
                                    readAppCustomization(
                                        preset.customizationProfile
                                    ).copy(
                                        savedColorPreset =
                                            customization
                                                .savedColorPreset,
                                        savedPersonalizationPreset =
                                            customization
                                                .savedPersonalizationPreset,
                                        savedPersonalizationPresets =
                                            customization
                                                .savedPersonalizationPresets
                                    )

                                onThemeModeChanged(
                                    preset.themeMode
                                )

                                onAdvancedModeChanged(
                                    preset.advancedModeEnabled
                                )

                                onPriceChangeNotificationsChanged(
                                    preset
                                        .priceChangeNotificationsEnabled
                                )

                                onCustomizationChanged(
                                    restoredCustomization
                                )
                            },
                            onUpdate = { preset ->
                                val updatedPreset =
                                    buildCurrentSetup(
                                        preset.name
                                    )

                                onCustomizationChanged(
                                    customization.copy(
                                        savedPersonalizationPresets =
                                            customization
                                                .savedPersonalizationPresets
                                                .map { savedPreset ->
                                                    if (
                                                        savedPreset.name ==
                                                        preset.name
                                                    ) {
                                                        updatedPreset
                                                    } else {
                                                        savedPreset
                                                    }
                                                }
                                    )
                                )
                            },
                            onRename = {
                                    preset,
                                    enteredName ->

                                val safeName =
                                    enteredName
                                        .trim()
                                        .take(
                                            MAX_SAVED_PRESET_NAME_LENGTH
                                        )

                                val nameAlreadyExists =
                                    customization
                                        .savedPersonalizationPresets
                                        .any { savedPreset ->
                                            savedPreset.name !=
                                                preset.name &&
                                                savedPreset.name.equals(
                                                    safeName,
                                                    ignoreCase = true
                                                )
                                        }

                                if (
                                    safeName.isNotBlank() &&
                                    !nameAlreadyExists
                                ) {
                                    onCustomizationChanged(
                                        customization.copy(
                                            savedPersonalizationPresets =
                                                customization
                                                    .savedPersonalizationPresets
                                                    .map { savedPreset ->
                                                        if (
                                                            savedPreset.name ==
                                                            preset.name
                                                        ) {
                                                            savedPreset.copy(
                                                                name =
                                                                    safeName
                                                            )
                                                        } else {
                                                            savedPreset
                                                        }
                                                    }
                                        )
                                    )
                                }
                            },
                            onDelete = { preset ->
                                onCustomizationChanged(
                                    customization.copy(
                                        savedPersonalizationPresets =
                                            customization
                                                .savedPersonalizationPresets
                                                .filterNot {
                                                    it.name ==
                                                        preset.name
                                                }
                                    )
                                )
                            }
                            )
                            }
                        }
                                }

                                Surface(
                                    onClick = {
                                        changeEditor(null)
                                    },
                                    modifier = Modifier
                                        .align(
                                            Alignment.TopEnd
                                        )
                                        .padding(
                                            top = 8.dp,
                                            end = 14.dp
                                        )
                                        .size(42.dp),
                                    shape = CircleShape,
                                    color =
                                        MaterialTheme
                                            .supremeColors
                                            .panelMuted,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color =
                                            MaterialTheme
                                                .supremeColors
                                                .border
                                    ),
                                    shadowElevation = 4.dp
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.fillMaxSize(),
                                        contentAlignment =
                                            Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector =
                                                Icons.Rounded.Close,
                                            contentDescription =
                                                "Close ${selectedSection?.title() ?: "settings"} editor",
                                            tint =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface,
                                            modifier =
                                                Modifier.size(20.dp)
                                        )
                                    }
                                }
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
private fun AccordionHeader(
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .padding(
                start = 16.dp,
                end = 7.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            tint =
                MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(21.dp)
        )

        Spacer(
            modifier = Modifier.width(9.dp)
        )

        Text(
            text = "Personalization",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onDismiss
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription =
                    "Close personalization"
            )
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
private fun PreviewMetric(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier
) {
    val supremeColors = MaterialTheme.supremeColors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = supremeColors.tintedSurface(
            roleColor = color,
            strength = 0.10f,
            lightBase = supremeColors.panelMuted
        ),
        border = BorderStroke(
            width = 1.dp,
            color = supremeColors.tintedSurface(
                roleColor = color,
                strength = 0.30f,
                lightBase = supremeColors.panelMuted
            )
        )
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
private fun PersonalizationSectionTabRow(
    selectedSectionName: String?,
    onSectionSelected: (PersonalizationSection) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = 14.dp,
            vertical = 6.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = PersonalizationSection.entries,
            key = { section -> section.name }
        ) { section ->
            val selected =
                selectedSectionName == section.name

            Surface(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .animateContentSize(
                        animationSpec = tween(180)
                    )
                    .clip(RoundedCornerShape(15.dp))
                    .clickable {
                        onSectionSelected(section)
                    }
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                    },
                shape = RoundedCornerShape(15.dp),
                color =
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                    } else {
                        MaterialTheme
                            .supremeColors
                            .panelMuted
                    },
                border = BorderStroke(
                    width = 1.dp,
                    color =
                        if (selected) {
                            MaterialTheme
                                .colorScheme
                                .primary
                        } else {
                            MaterialTheme
                                .supremeColors
                                .border
                        }
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 13.dp,
                        vertical = 11.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.Center
                ) {
                    Icon(
                        imageVector = section.icon(),
                        contentDescription = section.title(),
                        tint =
                            if (selected) {
                                MaterialTheme
                                    .colorScheme
                                    .onPrimaryContainer
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                            },
                        modifier = Modifier.size(21.dp)
                    )

                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn(tween(140)),
                        exit = fadeOut(tween(90))
                    ) {
                        Row {
                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = section.title(),
                                color = MaterialTheme
                                    .colorScheme
                                    .onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun PersonalizationSection.title(): String =
    when (this) {
        PersonalizationSection.APPEARANCE ->
            "Appearance"

        PersonalizationSection.QUICK_COMPARE ->
            "Quick Compare"

        PersonalizationSection.SHOP_SUMMARY ->
            "Shop Summary"

        PersonalizationSection.PRODUCT_DETAILS ->
            "Product Details"

        PersonalizationSection.PRICE_MOVEMENT ->
            "Price Movement"

        PersonalizationSection.ALERTS_BEHAVIOUR ->
            "Alerts"
    }

private fun PersonalizationSection.icon(): ImageVector =
    when (this) {
        PersonalizationSection.APPEARANCE ->
            Icons.Rounded.Palette

        PersonalizationSection.QUICK_COMPARE ->
            Icons.Rounded.Search

        PersonalizationSection.SHOP_SUMMARY ->
            Icons.Rounded.Storefront

        PersonalizationSection.PRODUCT_DETAILS ->
            Icons.Rounded.Info

        PersonalizationSection.PRICE_MOVEMENT ->
            Icons.Rounded.ShowChart

        PersonalizationSection.ALERTS_BEHAVIOUR ->
            Icons.Rounded.Notifications
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
    if (!expanded) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.supremeColors.panel,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(
                alpha = 0.45f
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(end = 42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(alpha = 0.13f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = summary,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
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
