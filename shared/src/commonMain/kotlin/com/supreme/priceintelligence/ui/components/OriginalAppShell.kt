package com.supreme.priceintelligence.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.app_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun OriginalAppBackground(
    isConnected: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(
                x = size.width * 0.82f,
                y = size.height * 0.08f
            )
            val radius = size.minDimension * 0.58f
            val glowColor = if (isConnected) {
                Color(0x1C10B981)
            } else {
                Color(0x28EF4444)
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
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
    horizontalPadding: Dp = 16.dp
) {
    val navigationShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = 16.dp
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF7C8794)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
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
            modifier = Modifier.size(24.dp)
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