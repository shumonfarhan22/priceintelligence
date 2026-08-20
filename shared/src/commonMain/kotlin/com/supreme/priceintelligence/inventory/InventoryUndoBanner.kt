package com.supreme.priceintelligence.inventory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.data.InventoryItem

@Composable
fun InventoryUndoBanner(
    pendingItems: Set<InventoryItem>,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp
) {
    if (pendingItems.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = horizontalPadding,
                vertical = 4.dp
            )
            .semantics {
                liveRegion = LiveRegionMode.Assertive
            },
        color = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 9.dp,
                bottom = 9.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (pendingItems.size == 1) {
                        "Product deleted"
                    } else {
                        "${pendingItems.size} products deleted"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (pendingItems.size == 1) {
                    Text(
                        text = pendingItems.first().productName,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "You have four seconds to undo",
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextButton(
                onClick = onUndo,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    text = "UNDO",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
