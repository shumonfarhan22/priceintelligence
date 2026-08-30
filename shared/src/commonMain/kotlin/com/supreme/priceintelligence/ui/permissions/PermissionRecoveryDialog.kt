package com.supreme.priceintelligence.ui.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics

@Composable
internal fun PermissionRecoveryDialog(
    title: String,
    explanation: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = androidx.compose.ui.Modifier
            .semantics {
                paneTitle = title
            },
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = explanation,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenSettings()
                }
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        }
    )
}
