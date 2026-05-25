package com.sfm.scanner.feature.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sfm.scanner.core.ui.components.PrimaryButton

@Composable
fun ScanScreen(
    onNavigateToPackaging: () -> Unit,
    onRequestArInstall: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    // Back is disabled during scan per TD-15.
    BackHandler { /* no-op */ }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val displayWidthPx = with(density) { configuration.screenWidthDp.dp.toPx().toInt() }
    val displayHeightPx = with(density) { configuration.screenHeightDp.dp.toPx().toInt() }

    LaunchedEffect(Unit) {
        viewModel.onScreenEntered(lifecycleOwner, displayWidthPx, displayHeightPx)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onCameraPermissionResult(granted) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onCameraPermissionResult(true)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ScanUiEffect.NavigateToPackaging -> onNavigateToPackaging()
                ScanUiEffect.RequestArInstall -> onRequestArInstall()
            }
        }
    }

    ScanScreenContent(
        uiState = uiState,
        onSurfaceProvider = viewModel::onSurfaceProviderAvailable,
        onToggleTorch = viewModel::onToggleTorch,
        onStartScan = viewModel::onStartScan,
        onStopScan = viewModel::onStopScan,
        onOpenSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        },
    )
}

@Composable
internal fun ScanScreenContent(
    uiState: ScanUiState,
    onSurfaceProvider: (androidx.camera.core.Preview.SurfaceProvider) -> Unit,
    onToggleTorch: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (uiState is ScanUiState.Error) {
        ErrorOverlay(message = uiState.message, onOpenSettings = onOpenSettings)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CameraPreview(onSurfaceProvider = onSurfaceProvider)

        // AR status chip (always visible during normal states)
        ArStatusChip(
            arDisplayState = arDisplayStateFor(uiState),
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(16.dp),
        )

        // Instruction banner (Ready or Scanning only)
        val instruction = instructionFor(uiState)
        if (instruction != null) {
            InstructionBanner(
                text = instruction,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
                    .padding(horizontal = 24.dp),
            )
        }

        // Bottom control bar
        BottomControlBar(
            uiState = uiState,
            onToggleTorch = onToggleTorch,
            onStartScan = onStartScan,
            onStopScan = onStopScan,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding(),
        )
    }
}

@Composable
private fun CameraPreview(onSurfaceProvider: (androidx.camera.core.Preview.SurfaceProvider) -> Unit) {
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                contentDescription = ScanStrings.CAMERA_VIEWFINDER_CONTENT_DESCRIPTION
                onSurfaceProvider(surfaceProvider)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ArStatusChip(arDisplayState: ArDisplayState, modifier: Modifier = Modifier) {
    val (dotColor, text) = arStatusColors(arDisplayState)
    Surface(
        modifier = modifier.semantics { contentDescription = text },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun arStatusColors(state: ArDisplayState): Pair<Color, String> {
    val cs = MaterialTheme.colorScheme
    return when (state) {
        ArDisplayState.INITIALIZING -> cs.outline to ScanStrings.AR_STATE_INITIALIZING
        ArDisplayState.TRACKING -> Color(0xFF4CAF50) to ScanStrings.AR_STATE_TRACKING
        ArDisplayState.POINT_A_CAPTURED -> cs.primary to ScanStrings.AR_STATE_POINT_A
        ArDisplayState.MEASUREMENT_COMPLETE -> Color(0xFF4CAF50) to ScanStrings.AR_STATE_COMPLETE
        ArDisplayState.TRACKING_LOST -> cs.error to ScanStrings.AR_STATE_LOST
        ArDisplayState.UNSUPPORTED -> cs.error to ScanStrings.AR_STATE_UNSUPPORTED
    }
}

@Composable
private fun InstructionBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BottomControlBar(
    uiState: ScanUiState,
    onToggleTorch: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(64.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: frame counter (only during Scanning/Stopping/Complete)
            FrameCounterText(uiState = uiState)

            // Right: torch + start/stop
            Row(verticalAlignment = Alignment.CenterVertically) {
                TorchToggle(
                    torchOn = torchOnFor(uiState),
                    enabled = torchEnabled(uiState),
                    onToggle = onToggleTorch,
                )
                Spacer(modifier = Modifier.width(16.dp))
                ActionButton(
                    uiState = uiState,
                    onStartScan = onStartScan,
                    onStopScan = onStopScan,
                )
            }
        }
    }
}

@Composable
private fun FrameCounterText(uiState: ScanUiState) {
    val count: Int? = when (uiState) {
        is ScanUiState.Scanning -> uiState.frameCount
        else -> null
    }
    if (count != null) {
        Text(
            text = ScanStrings.LABEL_FRAMES.format(count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    } else {
        Spacer(modifier = Modifier.width(1.dp))
    }
}

@Composable
private fun TorchToggle(torchOn: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val description = if (torchOn) ScanStrings.TORCH_ON_DESCRIPTION else ScanStrings.TORCH_OFF_DESCRIPTION
    IconButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        // Lightning-bolt unicode glyph used as a torch indicator (no extended icons dep needed).
        Text(
            text = if (torchOn) "⚡" else "⚡",
            color = if (torchOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ActionButton(
    uiState: ScanUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
) {
    when (uiState) {
        is ScanUiState.Ready -> {
            PrimaryButton(
                label = ScanStrings.BUTTON_START,
                onClick = onStartScan,
                modifier = Modifier.width(160.dp),
            )
        }
        is ScanUiState.Scanning -> {
            OutlinedButton(
                onClick = onStopScan,
                modifier = Modifier.width(120.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(text = ScanStrings.BUTTON_STOP)
            }
        }
        is ScanUiState.Stopping -> {
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.width(120.dp),
            ) {
                Text(text = ScanStrings.BUTTON_STOP)
            }
        }
        else -> {
            // Initializing / Complete / Error → no button
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            // "Open Settings" only shown for the permission error
            if (message == ScanStrings.ERROR_PERMISSION_CAMERA) {
                PrimaryButton(
                    label = ScanStrings.BUTTON_OPEN_SETTINGS,
                    onClick = onOpenSettings,
                    modifier = Modifier.width(220.dp),
                )
            }
        }
    }
}

// ── Pure helpers ─────────────────────────────────────────────────────────────

private fun arDisplayStateFor(state: ScanUiState): ArDisplayState = when (state) {
    is ScanUiState.Initializing -> ArDisplayState.INITIALIZING
    is ScanUiState.Ready -> state.arDisplayState
    is ScanUiState.Scanning -> state.arDisplayState
    is ScanUiState.Stopping -> ArDisplayState.MEASUREMENT_COMPLETE
    is ScanUiState.Complete -> ArDisplayState.MEASUREMENT_COMPLETE
    is ScanUiState.Error -> ArDisplayState.UNSUPPORTED
}

private fun instructionFor(state: ScanUiState): String? = when (state) {
    is ScanUiState.Ready -> ScanStrings.INSTRUCTION_READY
    is ScanUiState.Scanning -> ScanStrings.INSTRUCTION_SCANNING
    else -> null
}

private fun torchOnFor(state: ScanUiState): Boolean = when (state) {
    is ScanUiState.Ready -> state.torchOn
    is ScanUiState.Scanning -> state.torchOn
    else -> false
}

private fun torchEnabled(state: ScanUiState): Boolean = when (state) {
    is ScanUiState.Stopping, is ScanUiState.Complete, is ScanUiState.Error -> false
    else -> true
}
