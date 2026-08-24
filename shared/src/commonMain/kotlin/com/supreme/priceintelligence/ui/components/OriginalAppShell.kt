package com.supreme.priceintelligence.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.dashboard.PricePositionFilter
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.app_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OriginalAppBackground(
    isConnected: Boolean,
    filterBloom: PricePositionFilter?,
    content: @Composable () -> Unit
) {


    var networkPulseVisible by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(isConnected) {
        networkPulseVisible = true
        delay(2400.milliseconds)
        networkPulseVisible = false
    }

    val networkPulseColor by animateColorAsState(
        targetValue = if (isConnected) {
            Color(0xFF10B981)
        } else {
            Color(0xFFEF4444)
        },
        animationSpec = tween(
            durationMillis = 450,
            easing = FastOutSlowInEasing
        ),
        label = "networkPulseColor"
    )

    val networkPulseAlpha by animateFloatAsState(
        targetValue = if (networkPulseVisible) {
            if (isConnected) {
                0.065f
            } else {
                0.085f
            }
        } else {
            0f
        },
        animationSpec = tween(
            durationMillis = if (networkPulseVisible) {
                500
            } else {
                1400
            },
            easing = FastOutSlowInEasing
        ),
        label = "networkPulseAlpha"
    )

    // A separate, whole-screen pulse for the dashboard's Competitive/Review
    // KPI taps — distinct from the small corner glow above, which is only
    // about network status.
    var filterPulseVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(filterBloom) {
        if (filterBloom != null) {
            filterPulseVisible = true
            delay(900.milliseconds)
            filterPulseVisible = false
        } else {
            filterPulseVisible = false
        }
    }

    val filterPulseColor by animateColorAsState(
        targetValue = when (filterBloom) {
            PricePositionFilter.COMPETITIVE -> MaterialTheme.colorScheme.primary
            PricePositionFilter.REVIEW -> MaterialTheme.colorScheme.error
            null -> Color.Transparent
        },
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "filterPulseColor"
    )

    val filterPulseAlpha by animateFloatAsState(
        targetValue = if (filterPulseVisible) 0.12f else 0f,
        animationSpec = tween(
            durationMillis = if (filterPulseVisible) 250 else 800,
            easing = FastOutSlowInEasing
        ),
        label = "filterPulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val logoGlowCenter = Offset(
                x = size.width * 0.14f,
                y = size.height * 0.09f
            )



            if (networkPulseAlpha > 0f) {
                val networkGlowRadius =
                    size.minDimension * 0.58f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            networkPulseColor.copy(
                                alpha = networkPulseAlpha
                            ),
                            networkPulseColor.copy(
                                alpha = networkPulseAlpha * 0.18f
                            ),
                            Color.Transparent
                        ),
                        center = logoGlowCenter,
                        radius = networkGlowRadius
                    ),
                    radius = networkGlowRadius,
                    center = logoGlowCenter
                )
            }

            if (filterPulseAlpha > 0f) {
                val screenCenter = Offset(
                    x = size.width * 0.5f,
                    y = size.height * 0.5f
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            filterPulseColor.copy(alpha = filterPulseAlpha),
                            filterPulseColor.copy(alpha = filterPulseAlpha * 0.6f)
                        ),
                        center = screenCenter,
                        radius = size.maxDimension
                    ),
                    size = size
                )
            }
        }

        content()
    }
}

@Composable
fun OriginalDashboardHeader(
    isConnected: Boolean,
    horizontalPadding: Dp = 16.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 8.dp,
                bottom = 8.dp
            )
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = "Supreme Price Intelligence",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SUPREME",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Text(
                text = "PRICE INTELLIGENCE",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isConnected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
                .semantics {
                    contentDescription = if (isConnected) {
                        "Internet connected"
                    } else {
                        "No internet connection"
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConnected) {
                    Icons.Rounded.Wifi
                } else {
                    Icons.Rounded.WifiOff
                },
                contentDescription = null,
                tint = if (isConnected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun OriginalBottomNavigation(
    selectedDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    enabled: Boolean = true,
    reduceMotionEnabled: Boolean = false
) {
    val navigationShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = 8.dp
            )
            .height(72.dp)
            .clip(navigationShape)
            .background(Color(0xFF0F1216))
            .border(
                width = 1.dp,
                color = Color(0xFF1F252B),
                shape = navigationShape
            )
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppDestination.entries.forEachIndexed { index, destination ->
            OriginalNavigationItem(
                destination = destination,
                selected = destination == selectedDestination,
                enabled = enabled,
                reduceMotionEnabled = reduceMotionEnabled,
                onClick = {
                    onDestinationSelected(destination)
                },
                modifier = Modifier.weight(1f)
            )

            if (index < AppDestination.entries.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFF2A313C))
                )
            }
        }
    }
}

@Composable
private fun OriginalNavigationItem(
    destination: AppDestination,
    selected: Boolean,
    enabled: Boolean,
    reduceMotionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(0xFF7C8794)
        },
        animationSpec =
            if (reduceMotionEnabled) {
                snap()
            } else {
                tween(durationMillis = 180)
            },
        label = "navigationItemColor"
    )

    val iconScale by animateFloatAsState(
        targetValue =
            if (selected && !reduceMotionEnabled) {
                1.08f
            } else {
                1f
            },
        animationSpec =
            if (reduceMotionEnabled) {
                snap()
            } else {
                tween(durationMillis = 180)
            },
        label = "navigationIconScale"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.Tab
            )
            .semantics {
                contentDescription = "${destination.title} tab"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when (destination) {
                AppDestination.Dashboard -> Icons.Rounded.Home
                AppDestination.Inventory -> Icons.AutoMirrored.Rounded.List
            },
            contentDescription = null,
            tint = itemColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )

        Text(
            text = destination.title,
            color = itemColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}