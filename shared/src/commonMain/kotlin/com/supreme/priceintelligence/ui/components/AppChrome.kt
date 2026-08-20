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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

enum class AppDestination(val title: String) {
    Dashboard("Dashboard"),
    Inventory("Inventory")
}

@Composable
fun SupremeAmbientBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF0E141B),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val topBrandCenter = Offset(
                x = size.width * 0.08f,
                y = size.height * 0.06f
            )
            val middleAccentCenter = Offset(
                x = size.width * 0.96f,
                y = size.height * 0.48f
            )
            val bottomBrandCenter = Offset(
                x = size.width * 0.18f,
                y = size.height * 0.94f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3210B981),
                        Color.Transparent
                    ),
                    center = topBrandCenter,
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = topBrandCenter
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x208B7CF6),
                        Color.Transparent
                    ),
                    center = middleAccentCenter,
                    radius = size.minDimension * 0.68f
                ),
                radius = size.minDimension * 0.68f,
                center = middleAccentCenter
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x1410B981),
                        Color.Transparent
                    ),
                    center = bottomBrandCenter,
                    radius = size.minDimension * 0.54f
                ),
                radius = size.minDimension * 0.54f,
                center = bottomBrandCenter
            )
        }

        content()
    }
}

@Composable
fun SupremeHeader(
    isConnected: Boolean,
    horizontalPadding: Dp = 20.dp
) {
    val headerShape = RoundedCornerShape(22.dp)
    val statusColor = if (isConnected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val statusBackground = if (isConnected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 8.dp,
                bottom = 11.dp
            )
            .clip(headerShape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.9f),
                shape = headerShape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val logoShape = RoundedCornerShape(15.dp)

        Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = "Supreme Price Intelligence logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(48.dp)
                .clip(logoShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = logoShape
                )
                .padding(5.dp)
        )

        Spacer(modifier = Modifier.width(11.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "SUPREME",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.35.sp
            )

            Text(
                text = "PRICE INTELLIGENCE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(statusBackground)
                .border(
                    width = 1.dp,
                    color = statusColor.copy(alpha = 0.45f),
                    shape = CircleShape
                )
                .semantics {
                    contentDescription = if (isConnected) {
                        "Internet connection online"
                    } else {
                        "Internet connection offline; saved prices remain available"
                    }
                }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Text(
                text = if (isConnected) "ONLINE" else "OFFLINE",
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
fun SupremeBottomNavigation(
    selectedDestination: AppDestination,
    horizontalPadding: Dp = 20.dp,
    onDestinationSelected: (AppDestination) -> Unit
) {
    val navigationShape = RoundedCornerShape(25.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = horizontalPadding,
                vertical = 8.dp
            )
            .clip(navigationShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = navigationShape
            )
            .selectableGroup()
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppDestination.entries.forEach { destination ->
            SupremeNavigationItem(
                destination = destination,
                isSelected = selectedDestination == destination,
                onClick = {
                    onDestinationSelected(destination)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SupremeNavigationItem(
    destination: AppDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(19.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                } else {
                    Color.Transparent
                }
            )
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.Tab
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 28.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    }
                )
                .border(
                    width = if (isSelected) 1.dp else 0.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            DestinationIcon(
                destination = destination,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.size(4.dp))

        Text(
            text = destination.title,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
private fun DestinationIcon(
    destination: AppDestination,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "${destination.title} tab"
        }
    ) {
        val stroke = size.minDimension * 0.095f
        when (destination) {
            AppDestination.Dashboard -> {
                val gap = size.minDimension * 0.12f
                val cell = (size.minDimension - gap) / 2f
                listOf(
                    Offset.Zero,
                    Offset(cell + gap, 0f),
                    Offset(0f, cell + gap),
                    Offset(cell + gap, cell + gap)
                ).forEach { topLeft ->
                    drawRoundRect(
                        color = tint,
                        topLeft = topLeft,
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(cell * 0.22f),
                        style = Stroke(width = stroke)
                    )
                }
            }

            AppDestination.Inventory -> {
                val dotRadius = size.minDimension * 0.075f
                val lineStart = size.width * 0.30f
                listOf(0.22f, 0.50f, 0.78f).forEach { yFraction ->
                    val y = size.height * yFraction
                    drawCircle(tint, dotRadius, Offset(size.width * 0.11f, y))
                    drawLine(
                        color = tint,
                        start = Offset(lineStart, y),
                        end = Offset(size.width, y),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}
