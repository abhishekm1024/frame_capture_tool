package com.sfm.scanner.feature.scan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PackagingScreen(
    onNavigateToUpload: (zipArtifactJson: String) -> Unit,
    onNavigateToUploadWithError: (errorMessage: String) -> Unit,
    viewModel: PackagingViewModel = hiltViewModel(),
) {
    // Per screen_specs §6.5: no user interaction; system back is no-op.
    BackHandler { /* no-op during packaging */ }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PackagingUiEffect.NavigateToUpload -> onNavigateToUpload(effect.zipArtifactJson)
                is PackagingUiEffect.NavigateToUploadWithError ->
                    onNavigateToUploadWithError(effect.message)
            }
        }
    }

    PackagingScreenContent()
}

@Composable
internal fun PackagingScreenContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = ScanStrings.PACKAGING_CONTENT_DESCRIPTION },
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = ScanStrings.PACKAGING_LABEL,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
    }
}
