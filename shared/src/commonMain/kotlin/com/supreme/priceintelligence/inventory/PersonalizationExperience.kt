@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.supreme.priceintelligence.inventory

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.settings.AppThemeMode
import com.supreme.priceintelligence.settings.MAX_SAVED_PERSONALIZATION_PRESETS
import com.supreme.priceintelligence.settings.MAX_SAVED_PRESET_NAME_LENGTH
import com.supreme.priceintelligence.settings.SavedPersonalizationPreset
import com.supreme.priceintelligence.settings.readAppCustomization
import com.supreme.priceintelligence.ui.theme.retailerChartColors
import com.supreme.priceintelligence.ui.theme.semanticPalette
import com.supreme.priceintelligence.ui.theme.supremeColors
import com.supreme.priceintelligence.ui.input.withPlatformTextInput
import com.supreme.priceintelligence.dashboard.DashboardViewModel
import com.supreme.priceintelligence.dashboard.InteractiveAggregateMovementChart
import com.supreme.priceintelligence.dashboard.OriginalProfessionalProductDetailDialog
import com.supreme.priceintelligence.dashboard.ProductCardUiState
import com.supreme.priceintelligence.dashboard.QuickCompareScreen
import com.supreme.priceintelligence.dashboard.ShopPriceMovementDialog
import com.supreme.priceintelligence.dashboard.ShopMovementRange
import com.supreme.priceintelligence.dashboard.buildDecisionSummary
import com.supreme.priceintelligence.dashboard.buildPriceFreshnessSummary
import com.supreme.priceintelligence.dashboard.formatIndianPrice
import com.supreme.priceintelligence.home.LaunchHubScreen
import kotlin.time.Clock

internal enum class PersonalizationPreviewTarget(
    val displayName: String
) {
    LAUNCH_HUB("Launch hub"),
    QUICK_COMPARE("Quick Compare"),
    PRODUCT_DETAILS("Product details"),
    PRICE_MOVEMENT("Price movement"),
    ALERTS("Alerts")
}

@Composable
internal fun AdaptivePersonalizationPreview(
    dashboardViewModel: DashboardViewModel,
    customization: AppCustomization,
    priceChangeNotificationsEnabled: Boolean,
    target: PersonalizationPreviewTarget,
    selectedSection: PersonalizationSection?,
    reduceMotionEnabled: Boolean,
    onSectionSelected: (PersonalizationSection) -> Unit,
    onTargetSelected: (PersonalizationPreviewTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardState by
        dashboardViewModel.uiState.collectAsState()

    val decisionSummary = remember(
        dashboardState.allMatchingItems,
        dashboardState.pageItems
    ) {
        dashboardState
            .allMatchingItems
            .buildDecisionSummary(
                dashboardState.pageItems
            )
    }

    val freshnessSummary = remember(
        dashboardState.allMatchingItems
    ) {
        dashboardState
            .allMatchingItems
            .buildPriceFreshnessSummary(
                nowMillis = Clock.System
                    .now()
                    .toEpochMilliseconds()
            )
    }

    LaunchedEffect(target) {
        if (
            target ==
                PersonalizationPreviewTarget.PRICE_MOVEMENT
        ) {
            dashboardViewModel.loadShopPriceMovement()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val currentDensity =
                    LocalDensity.current

                val previewScale =
                    minOf(
                        maxWidth.value / 390f,
                        maxHeight.value / 820f,
                        0.92f
                    )

                val fittedDensity = remember(
                    currentDensity.density,
                    currentDensity.fontScale,
                    previewScale
                ) {
                    Density(
                        density =
                            currentDensity.density *
                                previewScale,
                        fontScale =
                            currentDensity.fontScale
                    )
                }

                CompositionLocalProvider(
                    LocalDensity provides
                        fittedDensity
                ) {
                    Surface(
                        modifier = Modifier.size(
                            width = 390.dp,
                            height = 820.dp
                        ),
                        shape = RoundedCornerShape(22.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .background,
                        border = BorderStroke(
                            width = 1.5.dp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                                    .copy(alpha = 0.55f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            PersonalizationLivePreviewContent(
                                dashboardViewModel =
                                    dashboardViewModel,
                                dashboardState =
                                    dashboardState,
                                decisionSummary =
                                    decisionSummary,
                                freshnessSummary =
                                    freshnessSummary,
                                customization =
                                    customization,
                                priceChangeNotificationsEnabled =
                                    priceChangeNotificationsEnabled,
                                target = target,
                                reduceMotionEnabled =
                                    reduceMotionEnabled
                            )
                        }
                    }
                }
            }

            if (selectedSection == null) {
                Spacer(modifier = Modifier.height(7.dp))

                PersonalizationEditorZones(
                    target = target,
                    onSectionSelected = onSectionSelected
                )

                Spacer(modifier = Modifier.height(7.dp))

                PersonalizationPreviewTargetSelector(
                    selectedTarget = target,
                    onTargetSelected = onTargetSelected
                )
            }
    }
}

@Composable
private fun PersonalizationEditorZones(
    target: PersonalizationPreviewTarget,
    onSectionSelected: (PersonalizationSection) -> Unit
) {
    val targetSection =
        when (target) {
            PersonalizationPreviewTarget.LAUNCH_HUB ->
                PersonalizationSection.SHOP_SUMMARY

            PersonalizationPreviewTarget.QUICK_COMPARE ->
                PersonalizationSection.QUICK_COMPARE

            PersonalizationPreviewTarget.PRODUCT_DETAILS ->
                PersonalizationSection.PRODUCT_DETAILS

            PersonalizationPreviewTarget.PRICE_MOVEMENT ->
                PersonalizationSection.PRICE_MOVEMENT

            PersonalizationPreviewTarget.ALERTS ->
                PersonalizationSection.ALERTS_BEHAVIOUR
        }

    if (
        target ==
            PersonalizationPreviewTarget.LAUNCH_HUB
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            PersonalizationEditorButton(
                label = "Appearance",
                section =
                    PersonalizationSection.APPEARANCE,
                onSectionSelected = onSectionSelected,
                modifier = Modifier.weight(1f)
            )

            PersonalizationEditorButton(
                label = targetSection.editorLabel(),
                section = targetSection,
                onSectionSelected = onSectionSelected,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        PersonalizationEditorButton(
            label = targetSection.editorLabel(),
            section = targetSection,
            onSectionSelected = onSectionSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PersonalizationEditorButton(
    label: String,
    section: PersonalizationSection,
    onSectionSelected: (PersonalizationSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = {
            onSectionSelected(section)
        },
        modifier = modifier.heightIn(min = 42.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.supremeColors.panelMuted,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.supremeColors.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 9.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector =
                    if (
                        section ==
                        PersonalizationSection.APPEARANCE
                    ) {
                        Icons.Rounded.Palette
                    } else {
                        Icons.Rounded.Edit
                    },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PersonalizationPreviewTargetSelector(
    selectedTarget: PersonalizationPreviewTarget,
    onTargetSelected: (PersonalizationPreviewTarget) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        PersonalizationPreviewTarget.entries.forEach { target ->
            val selected = target == selectedTarget

            Surface(
                onClick = {
                    onTargetSelected(target)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp),
                shape = RoundedCornerShape(14.dp),
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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = target.icon(),
                        contentDescription = target.displayName,
                        tint =
                            if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier
                            .padding(vertical = 9.dp)
                            .size(19.dp)
                    )
                }
            }
        }
    }
}

private fun PersonalizationSection.editorLabel(): String =
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

private fun PersonalizationPreviewTarget.icon(): ImageVector =
    when (this) {
        PersonalizationPreviewTarget.LAUNCH_HUB ->
            Icons.Rounded.Home

        PersonalizationPreviewTarget.QUICK_COMPARE ->
            Icons.Rounded.Search

        PersonalizationPreviewTarget.PRODUCT_DETAILS ->
            Icons.Rounded.Info

        PersonalizationPreviewTarget.PRICE_MOVEMENT ->
            Icons.Rounded.ShowChart

        PersonalizationPreviewTarget.ALERTS ->
            Icons.Rounded.Notifications
    }

@Composable
private fun PersonalizationLivePreviewContent(
    dashboardViewModel: DashboardViewModel,
    dashboardState:
        com.supreme.priceintelligence.dashboard.DashboardUiState,
    decisionSummary:
        com.supreme.priceintelligence.dashboard.DashboardDecisionSummary,
    freshnessSummary:
        com.supreme.priceintelligence.dashboard.PriceFreshnessSummary,
    customization: AppCustomization,
    priceChangeNotificationsEnabled: Boolean,
    target: PersonalizationPreviewTarget,
    reduceMotionEnabled: Boolean
) {
    AnimatedContent(
        targetState = target,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis =
                        if (reduceMotionEnabled) 0 else 180
                )
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis =
                        if (reduceMotionEnabled) 0 else 120
                )
            )
        },
        label = "personalizationLivePreviewContent"
    ) { visibleTarget ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (visibleTarget) {
                PersonalizationPreviewTarget.LAUNCH_HUB ->
                    LaunchHubScreen(
                        isConnected =
                            dashboardState.isConnected,
                        decisionSummary = decisionSummary,
                        freshnessSummary =
                            freshnessSummary,
                        activeFilter =
                            dashboardState.priceFilter,
                        refreshTick =
                            dashboardState
                                .refreshCollapseTick,
                        reduceMotionEnabled = true,
                        insightCustomization =
                            customization
                                .insightCustomization,
                        tileIconPreferences =
                            customization
                                .launchTileIconPreferences,
                        onDashboardClick = {},
                        onInventoryClick = {},
                        onPriceMovementClick = {},
                        onQuickCompareClick = {},
                        onSettingsClick = {},
                        onFilterSelected = {},
                        modifier = Modifier.fillMaxSize()
                    )

                PersonalizationPreviewTarget.QUICK_COMPARE ->
                    QuickCompareScreen(
                        viewModel = dashboardViewModel,
                        advancedModeEnabled = true,
                        reduceMotionEnabled = true,
                        customization = customization,
                        onNavigateHome = {},
                        previewMode = true,
                        focusCatalogPreview = false,
                        modifier = Modifier.fillMaxSize()
                    )

                PersonalizationPreviewTarget.PRODUCT_DETAILS -> {
                    val card =
                        dashboardState.pageItems.firstOrNull()

                    if (card == null) {
                        EmptyAuthenticPreview(
                            message =
                                "Add a product to preview its real comparison window."
                        )
                    } else {
                        QuickCompareScreen(
                            viewModel = dashboardViewModel,
                            advancedModeEnabled = true,
                            reduceMotionEnabled = true,
                            customization = customization,
                            onNavigateHome = {},
                            previewMode = true,
                            focusCatalogPreview = false,
                            modifier = Modifier.fillMaxSize()
                        )

                        OriginalProfessionalProductDetailDialog(
                            card = card,
                            networkState =
                                dashboardState.bloomState,
                            advancedModeEnabled = true,
                            reduceMotionEnabled = true,
                            insightCustomization =
                                customization
                                    .insightCustomization,
                            isHistoryLoading =
                                card.item.id in
                                    dashboardState
                                        .historyLoadingProductIds,
                            priceHistory =
                                dashboardState
                                    .priceHistoryByProduct[
                                        card.item.id
                                    ].orEmpty(),
                            onRefresh = {},
                            onDismiss = {},
                            embeddedPreview = true
                        )
                    }
                }

                PersonalizationPreviewTarget.PRICE_MOVEMENT ->
                    ShopPriceMovementDialog(
                        snapshot =
                            dashboardState.shopPriceMovement,
                        isLoading =
                            dashboardState
                                .isShopPriceMovementLoading,
                        errorMessage =
                            dashboardState
                                .shopPriceMovementError,
                        reduceMotionEnabled = true,
                        customization = customization,
                        useInternalTransition = false,
                        applySystemInsets = false,
                        focusOverviewPreview = true,
                        onRefresh = {},
                        onDismiss = {}
                    )

                PersonalizationPreviewTarget.ALERTS ->
                    AlertsPreview(
                        automaticChecksEnabled =
                            customization
                                .automaticPriceChecksEnabled,
                        alertsEnabled =
                            priceChangeNotificationsEnabled
                    )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember {
                            MutableInteractionSource()
                        },
                        indication = null,
                        onClick = {}
                    )
            )
        }
    }
}

@Composable
private fun EmptyAuthenticPreview(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PersonalizationPreviewContent(
    customization: AppCustomization,
    target: PersonalizationPreviewTarget,
    reduceMotionEnabled: Boolean
) {
    AnimatedContent(
        targetState = target,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis =
                        if (reduceMotionEnabled) {
                            0
                        } else {
                            180
                        }
                )
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis =
                        if (reduceMotionEnabled) {
                            0
                        } else {
                            120
                        }
                )
            )
        },
        label = "personalizationPreviewContent"
    ) { visibleTarget ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (visibleTarget) {
                PersonalizationPreviewTarget
                    .LAUNCH_HUB ->
                    LaunchHubPreview()

                PersonalizationPreviewTarget
                    .QUICK_COMPARE ->
                    DashboardPreview(
                        customization
                    )

                PersonalizationPreviewTarget
                    .PRODUCT_DETAILS ->
                    ProductDetailsPreview()

                PersonalizationPreviewTarget
                    .PRICE_MOVEMENT ->
                    PriceMovementPreview(
                        customization
                    )

                PersonalizationPreviewTarget
                    .ALERTS ->
                    AlertsPreview()
            }
        }
    }
}

@Composable
private fun ExpandedPersonalizationPreview(
    customization: AppCustomization,
    target: PersonalizationPreviewTarget,
    reduceMotionEnabled: Boolean,
    onDismiss: () -> Unit
) {
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
                    MaterialTheme
                        .supremeColors
                        .scrim
                        .copy(alpha = 0.92f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text =
                    "EXPANDED ${target.displayName.uppercase()}",
                color =
                    MaterialTheme
                        .colorScheme
                        .primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = 10.dp,
                        end = 10.dp
                    )
            ) {
                Icon(
                    imageVector =
                        Icons.Rounded.Close,
                    contentDescription =
                        "Close expanded preview",
                    tint =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxHeight(0.78f)
                    .aspectRatio(
                        ratio = 9f / 16f,
                        matchHeightConstraintsFirst = true
                    ),
                shape = RoundedCornerShape(28.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .background,
                border = BorderStroke(
                    width = 1.5.dp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(alpha = 0.60f)
                ),
                shadowElevation =
                    if (
                        MaterialTheme
                            .supremeColors
                            .isDark
                    ) {
                        8.dp
                    } else {
                        16.dp
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    PersonalizationPreviewContent(
                        customization =
                            customization,
                        target = target,
                        reduceMotionEnabled =
                            reduceMotionEnabled
                    )
                }
            }

            Text(
                text =
                    "Preview only • your inventory is not changed",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun LaunchHubPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(alpha = 0.18f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "₹",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "SUPREME",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "PRICE INTELLIGENCE",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onBackground,
                    fontSize = 7.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "⚙",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 17.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.supremeColors.panelMuted,
            border = BorderStroke(
                1.dp,
                MaterialTheme.supremeColors.border
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = "SHOP POSITION",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "34 products compared",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(29f)
                            .height(8.dp)
                            .background(
                                MaterialTheme
                                    .supremeColors
                                    .competitive,
                                RoundedCornerShape(6.dp)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .weight(5f)
                            .height(8.dp)
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .error,
                                RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            PreviewTile(
                text = "Dashboard",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            PreviewTile(
                text = "Inventory",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            PreviewTile(
                text = "Movement",
                color =
                    MaterialTheme
                        .supremeColors
                        .competitive,
                modifier = Modifier.weight(1f)
            )

            PreviewTile(
                text = "Quick Compare",
                color = MaterialTheme.supremeColors.warning,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PreviewTile(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(11.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.34f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun DashboardPreview(
    customization: AppCustomization
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text = "QUICK COMPARE",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text =
                    customization
                        .dashboardDefaultSort
                        .displayName,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 10.sp
            )
        }

        Text(
            text =
                "${customization.dashboardPageSize.displayName} products per page",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickComparePreviewCard(
                name = "Prestige Triply Pressure Cooker",
                price = "₹3,841",
                positive = true,
                modifier = Modifier.weight(1f)
            )

            QuickComparePreviewCard(
                name = "Hawkins Stainless Cookware Set",
                price = "₹2,799",
                positive = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickComparePreviewCard(
    name: String,
    price: String,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor =
        if (positive) {
            MaterialTheme.supremeColors.competitive
        } else {
            MaterialTheme.colorScheme.error
        }

    Surface(
        modifier = modifier.heightIn(min = 220.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.supremeColors.panelMuted,
        border = BorderStroke(
            1.dp,
            MaterialTheme.supremeColors.border
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(98.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PRODUCT",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier.padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = price,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text =
                        if (positive) {
                            "Competitive"
                        } else {
                            "Review"
                        },
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PreviewProductCard(
    name: String,
    price: String,
    positive: Boolean
) {
    val stateColor =
        if (positive) {
            MaterialTheme.supremeColors.competitive
        } else {
            MaterialTheme.colorScheme.error
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.supremeColors.panelMuted,
        border = BorderStroke(
            1.dp,
            MaterialTheme.supremeColors.border
        )
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(9.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◫",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Supreme Price: $price",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        stateColor,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ProductDetailsPreview(
    card: ProductCardUiState? = null
) {
    val item = card?.item
    val productName =
        item?.productName ?: "No inventory product yet"
    val shopPrice =
        item?.shopPrice?.let(::formatIndianPrice) ?: "—"
    val amazonValue =
        card?.amazonResult?.price ?: item?.amazonLastPrice
    val flipkartValue =
        card?.flipkartResult?.price ?: item?.flipkartLastPrice
    val amazonPrice =
        amazonValue?.let(::formatIndianPrice) ?: "Unavailable"
    val flipkartPrice =
        flipkartValue?.let(::formatIndianPrice) ?: "Unavailable"

    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            text = "PRODUCT DETAILS",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.supremeColors.panelMuted,
            border = BorderStroke(
                1.dp,
                MaterialTheme.supremeColors.border
            )
        ) {
            Row(
                modifier = Modifier.padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PRODUCT",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize = 7.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = productName,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = "Shop price",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize = 8.sp
                    )

                    Text(
                        text = shopPrice,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        PreviewRetailerPrice(
            retailer = "Amazon",
            price = amazonPrice,
            message =
                if (
                    item != null &&
                    amazonValue?.let { it < item.shopPrice } == true
                ) {
                    "Lower • review"
                } else {
                    "Higher or matched • good for shop"
                },
            color =
                if (
                    item != null &&
                    amazonValue?.let { it < item.shopPrice } == true
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.supremeColors.competitive
                }
        )

        PreviewRetailerPrice(
            retailer = "Flipkart",
            price = flipkartPrice,
            message =
                if (
                    item != null &&
                    flipkartValue?.let { it < item.shopPrice } == true
                ) {
                    "Lower • review"
                } else {
                    "Higher or matched • good for shop"
                },
            color =
                if (
                    item != null &&
                    flipkartValue?.let { it < item.shopPrice } == true
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.supremeColors.competitive
                }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color =
                MaterialTheme
                    .colorScheme
                    .primary
                    .copy(alpha = 0.14f)
        ) {
            Text(
                text = "Refresh live prices",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun PreviewRetailerPrice(
    retailer: String,
    price: String,
    message: String,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        color = color.copy(alpha = 0.09f),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.28f)
        )
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = retailer,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = message,
                    color = color,
                    fontSize = 8.sp
                )
            }

            Text(
                text = price,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun LivePriceMovementPreview(
    customization: AppCustomization,
    dashboardState:
        com.supreme.priceintelligence.dashboard.DashboardUiState
) {
    val liveChanges = remember(
        dashboardState.shopPriceMovement
    ) {
        dashboardState
            .shopPriceMovement
            .products
            .flatMap { product -> product.changes }
    }

    val generatedAt =
        dashboardState
            .shopPriceMovement
            .generatedAt
            .takeIf { it > 0L }
            ?: Clock.System
                .now()
                .toEpochMilliseconds()

    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            text = "PRICE MOVEMENT",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text =
                "${liveChanges.size} recorded changes • " +
                    customization
                        .priceMovementDefaultRange
                        .displayName,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 9.sp
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.supremeColors.panelMuted,
            border = BorderStroke(
                1.dp,
                MaterialTheme.supremeColors.border
            )
        ) {
            Box(
                modifier = Modifier.padding(10.dp)
            ) {
                InteractiveAggregateMovementChart(
                    changes = liveChanges,
                    range = ShopMovementRange.THIRTY_DAYS,
                    generatedAt = generatedAt,
                    lowerColor =
                        MaterialTheme.colorScheme.error,
                    higherColor =
                        MaterialTheme
                            .supremeColors
                            .competitive
                )
            }
        }
    }
}

@Composable
private fun PriceMovementPreview(
    customization: AppCustomization
) {
    val insight =
        customization.insightCustomization

    val retailerColors =
        insight
            .retailerChartPalette
            .retailerChartColors(
                customColors =
                    insight.customRetailerChartColors,
                isDarkTheme =
                    MaterialTheme
                        .supremeColors
                        .isDark
            )

    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            text = "PRICE MOVEMENT",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "Retailer price history • 30 days",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 9.sp
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            GraphLegend(
                label = "Amazon",
                color = retailerColors.amazon
            )

            GraphLegend(
                label = "Flipkart",
                color = retailerColors.flipkart
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.supremeColors.panelMuted,
            border = BorderStroke(
                1.dp,
                MaterialTheme.supremeColors.border
            )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                val gridColor =
                    Color.Gray.copy(alpha = 0.18f)

                repeat(4) { index ->
                    val y =
                        size.height *
                                index.toFloat() /
                                3f

                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val amazonPath = Path().apply {
                    moveTo(0f, size.height * 0.68f)
                    lineTo(size.width * 0.22f, size.height * 0.52f)
                    lineTo(size.width * 0.43f, size.height * 0.59f)
                    lineTo(size.width * 0.67f, size.height * 0.31f)
                    lineTo(size.width, size.height * 0.38f)
                }

                val flipkartPath = Path().apply {
                    moveTo(0f, size.height * 0.44f)
                    lineTo(size.width * 0.25f, size.height * 0.60f)
                    lineTo(size.width * 0.51f, size.height * 0.46f)
                    lineTo(size.width * 0.76f, size.height * 0.69f)
                    lineTo(size.width, size.height * 0.56f)
                }

                drawPath(
                    path = amazonPath,
                    color = retailerColors.amazon,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawPath(
                    path = flipkartPath,
                    color = retailerColors.flipkart,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawCircle(
                    color = retailerColors.amazon,
                    radius = 4.dp.toPx(),
                    center = Offset(
                        size.width * 0.67f,
                        size.height * 0.31f
                    )
                )

                drawCircle(
                    color = retailerColors.flipkart,
                    radius = 4.dp.toPx(),
                    center = Offset(
                        size.width * 0.76f,
                        size.height * 0.69f
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text = "30 days ago",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 8.sp
            )

            Text(
                text = "Today",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun GraphLegend(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = label,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 8.sp
        )
    }
}

@Composable
private fun AlertsPreview(
    automaticChecksEnabled: Boolean = true,
    alertsEnabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.14f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column {
                Text(
                    text = "PRICE ALERTS",
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )

                Text(
                    text = "Retailer changes worth checking",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.supremeColors.panel,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.supremeColors.border
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                AlertsPreviewStatus(
                    label = "Daily checks",
                    enabled = automaticChecksEnabled,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                AlertsPreviewStatus(
                    label = "Notifications",
                    enabled = alertsEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = "RECENT CHANGES",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.7.sp
        )

        PreviewAlert(
            productName =
                "Prestige 5 Litre Triply Pressure Cooker",
            retailer = "Amazon",
            change = "Higher by ₹200",
            color = MaterialTheme.supremeColors.competitive
        )

        PreviewAlert(
            productName =
                "Hawkins Stainless Cookware Set",
            retailer = "Flipkart",
            change = "Lower by ₹150",
            color = MaterialTheme.colorScheme.error
        )

        PreviewAlert(
            productName =
                "Prestige Omega Deluxe Fry Pan",
            retailer = "Amazon",
            change = "Matched shop price",
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.supremeColors.panelMuted
        ) {
            Text(
                text =
                    "Open an alert to see its exact product graph and saved price history.",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 13.sp,
                modifier = Modifier.padding(11.dp)
            )
        }
    }
}

@Composable
private fun AlertsPreviewStatus(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if (enabled) "ON" else "OFF",
            color =
                if (enabled) {
                    MaterialTheme
                        .colorScheme
                        .primary
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                },
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun PreviewAlert(
    productName: String,
    retailer: String,
    change: String,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.09f),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = productName,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "$retailer • $change",
                color = color,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun NamedPersonalizationSetupsSection(
    presets: List<SavedPersonalizationPreset>,
    activePresetName: String?,
    onSaveNew: (String) -> Unit,
    onApply: (SavedPersonalizationPreset) -> Unit,
    onUpdate: (SavedPersonalizationPreset) -> Unit,
    onRename: (
        preset: SavedPersonalizationPreset,
        newName: String
    ) -> Unit,
    onDelete: (SavedPersonalizationPreset) -> Unit
) {
    var nameDialogOpen by rememberSaveable {
        mutableStateOf(false)
    }

    var renameSourceName by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var nameDraft by rememberSaveable {
        mutableStateOf("")
    }

    var deleteCandidateName by rememberSaveable {
        mutableStateOf<String?>(null)
    }

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
                text = "SAVED SETUPS",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )

            Text(
                text =
                    "Save your complete personalization. Saved setups remain available after Reset All.",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            if (presets.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.supremeColors.panelMuted
                ) {
                    Text(
                        text =
                            "No saved setups yet. Adjust the app, then save the complete setup here.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                presets.forEach { preset ->
                    SavedSetupRow(
                        preset = preset,
                        active =
                            preset.name == activePresetName,
                        onApply = {
                            onApply(preset)
                        },
                        onUpdate = {
                            onUpdate(preset)
                        },
                        onRename = {
                            renameSourceName =
                                preset.name
                            nameDraft = preset.name
                            nameDialogOpen = true
                        },
                        onDelete = {
                            deleteCandidateName =
                                preset.name
                        }
                    )
                }
            }

            Button(
                onClick = {
                    renameSourceName = null
                    nameDraft = ""
                    nameDialogOpen = true
                },
                enabled =
                    presets.size <
                            MAX_SAVED_PERSONALIZATION_PRESETS,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = "Save current setup"
                )
            }

            if (
                presets.size >=
                MAX_SAVED_PERSONALIZATION_PRESETS
            ) {
                Text(
                    text =
                        "The maximum of $MAX_SAVED_PERSONALIZATION_PRESETS saved setups has been reached.",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }
    }

    if (nameDialogOpen) {
        SavedSetupNameDialog(
            initialName = nameDraft,
            originalName = renameSourceName,
            existingNames =
                presets.map { it.name },
            onConfirm = { enteredName ->
                val sourceName = renameSourceName

                if (sourceName == null) {
                    onSaveNew(enteredName)
                } else {
                    presets
                        .firstOrNull {
                            it.name == sourceName
                        }
                        ?.let { preset ->
                            onRename(
                                preset,
                                enteredName
                            )
                        }
                }

                nameDialogOpen = false
                renameSourceName = null
                nameDraft = ""
            },
            onDismiss = {
                nameDialogOpen = false
                renameSourceName = null
                nameDraft = ""
            }
        )
    }

    deleteCandidateName?.let { candidateName ->
        val candidate =
            presets.firstOrNull {
                it.name == candidateName
            }

        if (candidate != null) {
            AlertDialog(
                onDismissRequest = {
                    deleteCandidateName = null
                },
                title = {
                    Text("Delete saved setup?")
                },
                text = {
                    Text(
                        "Delete “${candidate.name}”? " +
                                "Your current app appearance will not change."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete(candidate)
                            deleteCandidateName = null
                        }
                    ) {
                        Text(
                            text = "Delete",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            deleteCandidateName = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SavedSetupRow(
    preset: SavedPersonalizationPreset,
    active: Boolean,
    onApply: () -> Unit,
    onUpdate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember {
        mutableStateOf(false)
    }

    val savedCustomization =
        remember(preset.customizationProfile) {
            readAppCustomization(
                preset.customizationProfile
            )
        }

    val savedPalette =
        savedCustomization.semanticPalette(
            isDarkTheme =
                preset.themeMode !=
                        AppThemeMode.LIGHT
        )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color =
            if (active) {
                MaterialTheme
                    .colorScheme
                    .primary
                    .copy(alpha = 0.10f)
            } else {
                MaterialTheme.supremeColors.panelMuted
            },
        border = BorderStroke(
            width =
                if (active) {
                    1.5.dp
                } else {
                    1.dp
                },
            color =
                if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.supremeColors.border
                }
        )
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = preset.name,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    listOf(
                        savedPalette.primary,
                        savedPalette.secondary,
                        savedPalette.competitive,
                        savedPalette.warning,
                        savedPalette.review
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text =
                        if (active) {
                            "ACTIVE"
                        } else {
                            "${preset.themeMode.name.lowercase()} • complete setup"
                        },
                    color =
                        if (active) {
                            MaterialTheme
                                .colorScheme
                                .primary
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        },
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            TextButton(
                onClick = onApply,
                enabled = !active
            ) {
                Text(
                    if (active) {
                        "Applied"
                    } else {
                        "Apply"
                    }
                )
            }

            Box {
                IconButton(
                    onClick = {
                        menuOpen = true
                    }
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.MoreVert,
                        contentDescription =
                            "Manage ${preset.name}"
                    )
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = {
                        menuOpen = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Update with current setup")
                        },
                        onClick = {
                            menuOpen = false
                            onUpdate()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Rename")
                        },
                        onClick = {
                            menuOpen = false
                            onRename()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedSetupNameDialog(
    initialName: String,
    originalName: String?,
    existingNames: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val valueState = rememberTextFieldState(initialName)
    val value = valueState.text.toString()

    val normalizedValue = value.trim()

    val duplicate =
        existingNames.any { existingName ->
            existingName.equals(
                normalizedValue,
                ignoreCase = true
            ) &&
                    !existingName.equals(
                        originalName.orEmpty(),
                        ignoreCase = true
                    )
        }

    val valid =
        normalizedValue.isNotBlank() &&
                normalizedValue.length <=
                MAX_SAVED_PRESET_NAME_LENGTH &&
                !duplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (originalName == null) {
                    "Save current setup"
                } else {
                    "Rename saved setup"
                }
            )
        },
        text = {
            OutlinedTextField(
                state = valueState,
                label = {
                    Text("Setup name")
                },
                inputTransformation =
                    InputTransformation.maxLength(
                        MAX_SAVED_PRESET_NAME_LENGTH
                    ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ).withPlatformTextInput(),
                onKeyboardAction = {
                    focusManager.clearFocus()
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                isError = duplicate,
                supportingText = {
                    Text(
                        when {
                            duplicate ->
                                "That name is already used."

                            else ->
                                "${value.length}/" +
                                        MAX_SAVED_PRESET_NAME_LENGTH
                        }
                    )
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(normalizedValue)
                },
                enabled = valid
            ) {
                Text(
                    if (originalName == null) {
                        "Save"
                    } else {
                        "Rename"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

internal fun activePersonalizationSetupName(
    presets: List<SavedPersonalizationPreset>,
    themeMode: AppThemeMode,
    advancedModeEnabled: Boolean,
    notificationsEnabled: Boolean,
    customization: AppCustomization
): String? {
    val comparableCurrent =
        customization.withoutSavedSetupData()

    return presets.firstOrNull { preset ->
        preset.themeMode == themeMode &&
                preset.advancedModeEnabled ==
                advancedModeEnabled &&
                preset.priceChangeNotificationsEnabled ==
                notificationsEnabled &&
                readAppCustomization(
                    preset.customizationProfile
                ).withoutSavedSetupData() ==
                comparableCurrent
    }?.name
}

internal fun AppCustomization.withoutSavedSetupData():
        AppCustomization =
    copy(
        savedColorPreset = null,
        savedPersonalizationPreset = null,
        savedPersonalizationPresets =
            emptyList()
    )
