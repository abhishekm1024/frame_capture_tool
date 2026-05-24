package com.sfm.scanner.feature.upload

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sfm.scanner.core.ui.components.PrimaryButton

@Composable
fun UploadScreen(
    onNavigateToSelection: () -> Unit,
    viewModel: UploadViewModel = hiltViewModel(),
) {
    BackHandler { /* no-op: back disabled on UploadScreen per screen_specs.md §7.8 */ }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                UploadUiEffect.NavigateToSelection -> onNavigateToSelection()
            }
        }
    }

    UploadScreenContent(
        uiState = uiState,
        onStartNewScan = viewModel::onStartNewScan,
    )
}

@Composable
internal fun UploadScreenContent(
    uiState: UploadUiState,
    onStartNewScan: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (uiState) {
                UploadUiState.Authenticating -> AuthenticatingContent()
                is UploadUiState.Uploading -> UploadingContent(uiState)
                UploadUiState.Success -> SuccessContent(onStartNewScan)
                is UploadUiState.Failed -> FailedContent(filename = uiState.filename, onStartNewScan = onStartNewScan)
                UploadUiState.RetryQueued -> RetryQueuedContent(onStartNewScan)
            }
        }
    }
}

@Composable
private fun AuthenticatingContent() {
    CircularProgressIndicator(
        modifier = Modifier
            .size(64.dp)
            .semantics { contentDescription = "Preparing upload, please wait" },
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = UploadStrings.AUTHENTICATING_LABEL,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun UploadingContent(state: UploadUiState.Uploading) {
    Text(
        text = UploadStrings.UPLOADING_LABEL,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Text(
        text = state.filename.truncateMiddle(40),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    LinearProgressIndicator(
        progress = { state.percent / 100f },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(state.percent / 100f, 0f..1f)
            },
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = "${state.percent}%",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SuccessContent(onStartNewScan: () -> Unit) {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = "Upload successful",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(64.dp),
    )
    Text(
        text = UploadStrings.SUCCESS_TITLE,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Text(
        text = UploadStrings.SUCCESS_BODY,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    PrimaryButton(
        label = UploadStrings.BUTTON_NEW_SCAN,
        onClick = onStartNewScan,
        modifier = Modifier.semantics { contentDescription = "Start a new scan session" },
    )
}

@Composable
private fun FailedContent(filename: String, onStartNewScan: () -> Unit) {
    Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = "Upload failed",
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(64.dp),
    )
    Text(
        text = UploadStrings.FAILED_TITLE,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Text(
        text = UploadStrings.FAILED_BODY,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Text(
        text = filename.truncateMiddle(40),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    PrimaryButton(
        label = UploadStrings.BUTTON_NEW_SCAN,
        onClick = onStartNewScan,
        modifier = Modifier.semantics { contentDescription = "Start a new scan session" },
    )
}

@Composable
private fun RetryQueuedContent(onStartNewScan: () -> Unit) {
    Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = "Upload failed",
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(64.dp),
    )
    Text(
        text = UploadStrings.FAILED_TITLE,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Text(
        text = UploadStrings.RETRY_QUEUED_NOTE,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    PrimaryButton(
        label = UploadStrings.BUTTON_NEW_SCAN,
        onClick = onStartNewScan,
        modifier = Modifier.semantics { contentDescription = "Start a new scan session" },
    )
}

private fun String.truncateMiddle(maxLen: Int): String {
    if (length <= maxLen) return this
    val keep = (maxLen - 1) / 2
    return "${take(keep)}…${takeLast(maxLen - keep - 1)}"
}
