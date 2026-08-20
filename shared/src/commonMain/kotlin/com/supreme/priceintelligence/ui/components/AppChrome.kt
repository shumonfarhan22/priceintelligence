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
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val firstCenter = Offset(size.width * 0.12f, size.height * 0.10f)
            val secondCenter = Offset(size.width * 0.92f, size.height * 0.70f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2410B981), Color.Transparent),
                    center = firstCenter,
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = firstCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x188B7CF6), Color.Transparent),
                    center = secondCenter,
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = secondCenter
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 10.dp,
                bottom = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = "Price Intelligence logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(15.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SUPREME",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "Price Intelligence",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isConnected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
                .semantics {
                    contentDescription = if (isConnected) {
                        "Internet connection online"
                    } else {
                        "Internet connection offline; saved prices remain available"
                    }
                }
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
            )
            Text(
                text = if (isConnected) "Online" else "Offline",
                color = if (isConnected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
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
                vertical = 10.dp
            )
            .clip(navigationShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(1.dp, MaterialTheme.colorScheme.outline, navigationShape)
            .selectableGroup()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppDestination.entries.forEach { destination ->
            SupremeNavigationItem(
                destination = destination,
                isSelected = selectedDestination == destination,
                onClick = { onDestinationSelected(destination) },
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
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(19.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DestinationIcon(
            destination = destination,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = destination.title,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
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
