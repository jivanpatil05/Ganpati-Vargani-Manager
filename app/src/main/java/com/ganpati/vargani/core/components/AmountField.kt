package com.ganpati.vargani.core.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.ganpati.vargani.R
import com.ganpati.vargani.core.theme.TextFieldShape
import com.ganpati.vargani.core.theme.VarganiTheme

@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.amount),
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || it == '.' }
            val dotCount = filtered.count { it == '.' }
            if (dotCount <= 1) {
                val parts = filtered.split('.')
                val sanitized = if (parts.size == 2) {
                    parts[0] + "." + parts[1].take(2)
                } else {
                    filtered
                }
                onValueChange(sanitized)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewWhenFocused(),
        label = { Text(label) },
        prefix = {
            Text(
                text = stringResource(R.string.rupee_symbol),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = {
            if (isError && errorMessage != null) {
                Text(text = errorMessage)
            }
        },
        shape = TextFieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun AmountFieldPreview() {
    VarganiTheme {
        AmountField(
            value = "5100",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AmountFieldErrorPreview() {
    VarganiTheme {
        AmountField(
            value = "",
            onValueChange = {},
            isError = true,
            errorMessage = stringResource(R.string.error_amount_required),
        )
    }
}
