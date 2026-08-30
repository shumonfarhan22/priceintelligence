package com.supreme.priceintelligence.inventory

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.ui.feedback.rememberPlatformHaptics
import com.supreme.priceintelligence.ui.theme.supremeColors

@Composable
fun OriginalInventoryUndoBanner(
    pendingItems: Set<InventoryItem>,
    onUndo: () -> Unit,
    hapticFeedbackEnabled: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp
) {
    if (pendingItems.isEmpty()) return

    val platformHaptics = rememberPlatformHaptics()
    val progress = remember(pendingItems) {
        Animatable(1f)
    }

    LaunchedEffect(pendingItems) {
        if (hapticFeedbackEnabled) {
            platformHaptics.warning()
        }

        progress.snapTo(1f)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 4000,
                easing = LinearEasing
            )
        )
    }

    val contentColor =
        if (MaterialTheme.supremeColors.isDark) {
            Color.White
        } else {
            Color(0xFF111827)
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .semantics {
                liveRegion = LiveRegionMode.Assertive
            }
            .background(
                color = Color(0xFFEF4444),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(23.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (pendingItems.size == 1) {
                        "Product Deleted"
                    } else {
                        "${pendingItems.size} Products Deleted"
                    },
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (pendingItems.size == 1) {
                        pendingItems.first().productName
                    } else {
                        "Tap undo to restore"
                    },
                    color = contentColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clickable(onClick = onUndo),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = {
                        progress.value
                    },
                    modifier = Modifier.fillMaxSize(),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.28f),
                    strokeWidth = 3.dp
                )

                Text(
                    text = "UNDO",
                    color = contentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
