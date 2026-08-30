package com.supreme.priceintelligence.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.dashboard.DashboardDecisionSummary
import com.supreme.priceintelligence.dashboard.DashboardDecisionSummaryCard
import com.supreme.priceintelligence.dashboard.PriceFreshnessSummary
import com.supreme.priceintelligence.dashboard.PricePositionFilter
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.app_logo
import com.supreme.priceintelligence.settings.InsightCustomization
import com.supreme.priceintelligence.ui.theme.Accent
import com.supreme.priceintelligence.ui.theme.supremeColors
import com.supreme.priceintelligence.ui.theme.tintedSurface
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun LaunchHubScreen(
    isConnected: Boolean,
    decisionSummary: DashboardDecisionSummary,
    freshnessSummary: PriceFreshnessSummary,
    activeFilter: PricePositionFilter?,
    refreshTick: Int,
    reduceMotionEnabled: Boolean,
    insightCustomization: InsightCustomization,
    onDashboardClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onPriceMovementClick: () -> Unit,
    onQuickCompareClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFilterSelected: (PricePositionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "launch-header") {
            LaunchHubHeader(
                isConnected = isConnected,
                onSettingsClick = onSettingsClick
            )
        }

        item(key = "launch-shop-position") {
            DashboardDecisionSummaryCard(
                summary = decisionSummary,
                freshnessSummary = freshnessSummary,
                activeFilter = activeFilter,
                refreshTick = refreshTick,
                reduceMotionEnabled = reduceMotionEnabled,
                insightCustomization = insightCustomization,
                showOverviewSummary = true,
                showBreakdown = false,
                showTopPriorities = false,
                overviewCollapsible = false,
                showPriceMovementAction = false,
                showLivePricePill = false,
                showMeterLegend = true,
                outerVerticalPadding = 0.dp,
                onPriceMovementClick =
                    onPriceMovementClick,
                onFilterToggle = onFilterSelected
            )
        }

        if (
            decisionSummary.priorityProducts
                .isNotEmpty()
        ) {
            item(key = "launch-top-priorities") {
                DashboardDecisionSummaryCard(
                    summary = decisionSummary,
                    freshnessSummary =
                        freshnessSummary,
                    activeFilter = activeFilter,
                    refreshTick = refreshTick,
                    reduceMotionEnabled =
                        reduceMotionEnabled,
                    insightCustomization =
                        insightCustomization,
                    showOverviewSummary = false,
                    showBreakdown = false,
                    showTopPriorities = true,
                    overviewCollapsible = false,
                    showPriceMovementAction = false,
                    showLivePricePill = false,
                    showMeterLegend = false,
                    outerVerticalPadding = 0.dp,
                    onPriceMovementClick =
                        onPriceMovementClick,
                    onFilterToggle =
                        onFilterSelected
                )
            }
        }

        item(key = "launch-first-row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                LaunchDestinationTile(
                    title = "Insights",
                    icon = Icons.Rounded.Dashboard,
                    accent =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    onClick = onDashboardClick,
                    reduceMotionEnabled =
                        reduceMotionEnabled,
                    modifier = Modifier.weight(1f)
                )

                LaunchDestinationTile(
                    title = "Inventory",
                    icon = Icons.Rounded.Inventory2,
                    accent =
                        MaterialTheme
                            .colorScheme
                            .secondary,
                    onClick = onInventoryClick,
                    reduceMotionEnabled =
                        reduceMotionEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "launch-second-row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                LaunchDestinationTile(
                    title = "Price Movement",
                    icon = Icons.Rounded.ShowChart,
                    accent = Accent,
                    onClick = onPriceMovementClick,
                    reduceMotionEnabled =
                        reduceMotionEnabled,
                    modifier = Modifier.weight(1f)
                )

                LaunchDestinationTile(
                    title = "Quick Compare",
                    icon = Icons.Rounded.Search,
                    accent =
                        MaterialTheme
                            .supremeColors
                            .warning,
                    onClick = onQuickCompareClick,
                    reduceMotionEnabled =
                        reduceMotionEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LaunchHubHeader(
    isConnected: Boolean,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 4.dp,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                Res.drawable.app_logo
            ),
            contentDescription =
                "Supreme Price Intelligence logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(50.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "SUPREME",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                maxLines = 1
            )

            Text(
                text = "PRICE INTELLIGENCE",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.size(5.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (isConnected) {
                            Icons.Rounded.Wifi
                        } else {
                            Icons.Rounded.WifiOff
                        },
                    contentDescription = null,
                    tint =
                        if (isConnected) {
                            MaterialTheme
                                .supremeColors
                                .competitive
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    modifier = Modifier.size(14.dp)
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text =
                        if (isConnected) {
                            "Online"
                        } else {
                            "Offline • saved prices available"
                        },
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(46.dp)
                .semantics {
                    role = Role.Button
                },
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.supremeColors.panel,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.supremeColors.border
            )
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription =
                        "Open personalization settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LaunchDestinationTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    reduceMotionEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val isPressed by
        interactionSource.collectIsPressedAsState()

    val pressProgress by animateFloatAsState(
        targetValue =
            if (
                reduceMotionEnabled ||
                !isPressed
            ) {
                0f
            } else {
                1f
            },
        animationSpec =
            when {
                reduceMotionEnabled ->
                    snap()

                isPressed ->
                    tween(
                        durationMillis = 65,
                        easing =
                            FastOutSlowInEasing
                    )

                else ->
                    spring(
                        dampingRatio = 0.88f,
                        stiffness = 650f
                    )
            },
        label = "launchTilePress-$title"
    )

    val tileScale =
        1f - (pressProgress * 0.035f)

    val supremeColors =
        MaterialTheme.supremeColors

    val tileGradientStart =
        supremeColors.tintedSurface(
            roleColor = accent,
            strength =
                0.16f +
                    (pressProgress * 0.08f)
        )

    val tileGradientMiddle =
        supremeColors.tintedSurface(
            roleColor = accent,
            strength =
                0.055f +
                    (pressProgress * 0.035f)
        )

    val tileGradientEnd =
        if (supremeColors.isDark) {
            Color.Transparent
        } else {
            supremeColors.panel.copy(alpha = 1f)
        }

    val haloColor =
        supremeColors.tintedSurface(
            roleColor = accent,
            strength =
                0.07f +
                    (pressProgress * 0.055f)
        )

    val iconContainerColor =
        supremeColors.tintedSurface(
            roleColor = accent,
            strength =
                0.13f +
                    (pressProgress * 0.075f)
        )

    val accentBorderColor =
        supremeColors.tintedSurface(
            roleColor = accent,
            strength =
                0.30f +
                    (pressProgress * 0.20f)
        )

    val iconBorderColor =
        supremeColors.tintedSurface(
            roleColor = accent,
            strength =
                0.28f +
                    (pressProgress * 0.18f)
        )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 174.dp)
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
                alpha =
                    1f -
                        (pressProgress * 0.025f)
            }
            .semantics {
                role = Role.Button
            },
        shape = RoundedCornerShape(26.dp),
        color = supremeColors.panel,
        border = BorderStroke(
            width = 1.dp,
            color = accentBorderColor
        ),
        shadowElevation =
            if (supremeColors.isDark) {
                2.dp
            } else {
                3.dp
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 174.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            tileGradientStart,
                            tileGradientMiddle,
                            tileGradientEnd
                        )
                    )
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(
                            color = haloColor,
                            shape =
                                RoundedCornerShape(26.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = iconContainerColor,
                        border = BorderStroke(
                            width = 1.dp,
                            color = iconBorderColor
                        ),
                        shadowElevation =
                            if (
                                supremeColors.isDark
                            ) {
                                3.dp
                            } else {
                                1.dp
                            }
                    ) {
                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent,
                                modifier =
                                    Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.heightIn(
                        min = 16.dp
                    )
                )

                Text(
                    text = title,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
