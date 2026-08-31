@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.supreme.priceintelligence.ui.input

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.supreme.priceintelligence.ui.theme.supremeColors

@Composable
internal actual fun PlatformProductNameTextField(
    state: TextFieldState,
    placeholder: String,
    onNext: () -> Unit,
    modifier: Modifier
) {
    OutlinedTextField(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        onKeyboardAction = { onNext() },
        lineLimits = TextFieldLineLimits.SingleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor =
                MaterialTheme.supremeColors.panelMuted,
            unfocusedContainerColor =
                MaterialTheme.supremeColors.panel,
            focusedBorderColor =
                MaterialTheme.colorScheme.primary,
            unfocusedBorderColor =
                MaterialTheme.supremeColors.border,
            focusedTextColor =
                MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor =
                MaterialTheme.colorScheme.onSurface,
            cursorColor =
                MaterialTheme.colorScheme.primary
        )
    )
}
