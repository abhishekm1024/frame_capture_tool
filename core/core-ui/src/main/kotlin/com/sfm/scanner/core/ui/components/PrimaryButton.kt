package com.sfm.scanner.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sfm.scanner.core.ui.theme.ScanAppTheme

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true, name = "PrimaryButton — Enabled")
@Composable
private fun PrimaryButtonEnabledPreview() {
    ScanAppTheme {
        PrimaryButton(label = "Continue", onClick = {})
    }
}

@Preview(showBackground = true, name = "PrimaryButton — Disabled")
@Composable
private fun PrimaryButtonDisabledPreview() {
    ScanAppTheme {
        PrimaryButton(label = "Continue", onClick = {}, enabled = false)
    }
}
