package com.sfm.scanner.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sfm.scanner.core.ui.theme.ScanAppTheme

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            isError = error != null,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "LabeledTextField — Default")
@Composable
private fun LabeledTextFieldDefaultPreview() {
    ScanAppTheme {
        LabeledTextField(
            label = "Detail",
            value = "ABC123",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true, name = "LabeledTextField — Error")
@Composable
private fun LabeledTextFieldErrorPreview() {
    ScanAppTheme {
        LabeledTextField(
            label = "Detail",
            value = "bad!input",
            onValueChange = {},
            error = "Only alphanumeric characters allowed",
        )
    }
}
