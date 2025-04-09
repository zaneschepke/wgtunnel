package com.zaneschepke.wireguardautotunnel.ui.common.config

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization

@Composable
fun ConfigurationTextBox(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    keyboardActions: KeyboardActions = KeyboardActions(),
    label: String,
    modifier: Modifier,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions =
        KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    OutlinedTextField(
        isError = isError,
        textStyle = MaterialTheme.typography.labelLarge,
        modifier = modifier,
        value = value,
        singleLine = true,
        interactionSource = interactionSource,
        onValueChange = { onValueChange(it) },
        label = {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        maxLines = 1,
        placeholder = {
            Text(
                hint,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailing,
    )
}
