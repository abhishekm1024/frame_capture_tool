package com.sfm.scanner.feature.scan

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sfm.scanner.core.common.AppDispatchers
import com.sfm.scanner.data.ar.ARMeasurement
import com.sfm.scanner.data.ar.ArRepository
import com.sfm.scanner.data.ar.ArSessionEvent
import com.sfm.scanner.data.ar.ArTrackingState
import com.sfm.scanner.data.camera.CameraConfig
import com.sfm.scanner.data.camera.CameraRepository
import com.sfm.scanner.data.camera.FrameRecord
import com.sfm.scanner.data.camera.FrameResult
import com.sfm.scanner.feature.scan.domain.model.FormDataSnapshot
import com.sfm.scanner.feature.scan.domain.usecase.MonitorArMeasurementUseCase
import com.sfm.scanner.feature.scan.domain.usecase.StartScanUseCase
import com.sfm.scanner.feature.scan.domain.usecase.StopScanUseCase
import com.sfm.scanner.feature.scan.infra.FrameWriter
import com.sfm.scanner.feature.scan.infra.ScanSessionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLDecoder
import java.util.UUID
import javax.inject.Inject

/**
 * Orchestrates the scan session lifecycle.
 *
 * High-level flow:
 *   1. composition → [onScreenEntered] supplies LifecycleOwner + display dims
 *   2. [startArSession] launches AR; emits [ArSessionEvent.CameraShared] with cameraId
 *   3. ScanScreen provides [Preview.SurfaceProvider] via [onSurfaceProviderAvailable]
 *   4. When both are available → [bindCameraIfReady] starts the camera collector
 *      (Preview + ImageAnalysis bound; frames flow but go to a null sink — discarded)
 *   5. AR signals Ready → transition Initializing → Ready (user sees viewfinder)
 *   6. User taps Start → FrameWriter created and assigned to [activeWriter]; AR
 *      measurement collection begins; state → Scanning
 *   7. User taps Stop → camera stopped; writer drained; ScanSession built; deposited
 *      in [ScanSessionHolder]; emit NavigateToPackaging
 *
 * Threading:
 *   - All state mutations happen on Main (via `viewModelScope` default dispatcher).
 *   - Camera flow collection runs on IO via `viewModelScope.launch(dispatchers.io)`.
 *   - [FrameWriter] manages its own IO worker pool (bounded queue + DROP_OLDEST).
 *   - [activeWriter] is `@Volatile`; the camera flow collector reads it on each frame
 *     and submits to the writer (or drops if null).
 */
@HiltViewModel
class ScanViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    private val arRepository: ArRepository,
    private val cameraRepository: CameraRepository,
    private val startScanUseCase: StartScanUseCase,
    private val stopScanUseCase: StopScanUseCase,
    private val monitorArMeasurementUseCase: MonitorArMeasurementUseCase,
    private val sessionHolder: ScanSessionHolder,
    private val dispatchers: AppDispatchers,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val formData: FormDataSnapshot = decodeFormData(
        checkNotNull(savedStateHandle[ScanDestination.ARG_FORM_DATA]),
    )

    private val sessionUuid: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Initializing)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ScanUiEffect>(replay = 0, extraBufferCapacity = 1)
    val effects: SharedFlow<ScanUiEffect> = _effects.asSharedFlow()

    // ── External inputs supplied by ScanScreen ──────────────────────────────────

    private var lifecycleOwner: LifecycleOwner? = null
    private var displayWidthPx: Int = 0
    private var displayHeightPx: Int = 0
    private var surfaceProvider: Preview.SurfaceProvider? = null

    // ── Session-scoped state ────────────────────────────────────────────────────

    @Volatile private var arCameraId: String? = null
    @Volatile private var cameraBound: Boolean = false
    @Volatile private var activeWriter: FrameWriter? = null

    private var torchOn: Boolean = false
    private var torchLockedOn: Boolean = false

    private var arDisplayState: ArDisplayState = ArDisplayState.INITIALIZING
    private var latestMeasurement: ARMeasurement? = null

    private val frameRecords = mutableListOf<FrameRecord>()
    private var scanStartTimestampMs: Long = 0L
    private var sessionDir: File? = null

    private var arSessionJob: Job? = null
    private var arMeasurementJob: Job? = null
    private var cameraFlowJob: Job? = null
    private var frameWrittenJob: Job? = null

    // ── Public API called from ScanScreen ───────────────────────────────────────

    /**
     * Called once on composition (from a `LaunchedEffect` with Unit key). Supplies
     * Activity-owned values that the ViewModel cannot inject.
     */
    fun onScreenEntered(lifecycleOwner: LifecycleOwner, displayWidthPx: Int, displayHeightPx: Int) {
        if (this.lifecycleOwner != null) return
        this.lifecycleOwner = lifecycleOwner
        this.displayWidthPx = displayWidthPx
        this.displayHeightPx = displayHeightPx
        startArSession()
    }

    fun onSurfaceProviderAvailable(provider: Preview.SurfaceProvider) {
        if (surfaceProvider != null) return
        surfaceProvider = provider
        bindCameraIfReady()
    }

    fun onCameraPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.value = ScanUiState.Error(ScanStrings.ERROR_PERMISSION_CAMERA)
        }
    }

    fun onToggleTorch() {
        // During Scanning, torch is locked per TD-15 — UI toggle is visual-only.
        torchOn = !torchOn
        viewModelScope.launch {
            if (!torchLockedOn) {
                cameraRepository.setTorch(torchOn)
            }
            updateTorchInState()
        }
    }

    fun onStartScan() {
        val state = _uiState.value
        if (state !is ScanUiState.Ready) return
        val owner = lifecycleOwner ?: return

        val dir = startScanUseCase.createSessionDirectory(sessionUuid).also { sessionDir = it }

        val writer = FrameWriter(dir, dispatchers).also {
            it.start(viewModelScope)
        }

        // Lock torch state per TD-15 for scan duration
        torchLockedOn = torchOn

        frameWrittenJob = viewModelScope.launch {
            writer.frameWritten.collect { record ->
                if (frameRecords.isEmpty()) {
                    scanStartTimestampMs = System.currentTimeMillis()
                }
                frameRecords.add(record)
                updateFrameCount(frameRecords.size)
            }
        }

        // Activate writer atomically so the camera flow collector starts submitting frames.
        activeWriter = writer

        // Begin AR measurement collection
        arMeasurementJob = viewModelScope.launch {
            monitorArMeasurementUseCase.execute(displayWidthPx, displayHeightPx).collect { m ->
                latestMeasurement = m
                arDisplayState = mapToArDisplayState(m)
                _uiState.update { s ->
                    if (s is ScanUiState.Scanning) s.copy(arDisplayState = arDisplayState) else s
                }
            }
        }

        _uiState.value = ScanUiState.Scanning(
            frameCount = 0,
            torchOn = torchOn,
            arDisplayState = arDisplayState,
        )
    }

    fun onStopScan() {
        if (_uiState.value !is ScanUiState.Scanning) return
        _uiState.value = ScanUiState.Stopping

        viewModelScope.launch {
            // 1. Disconnect sink so the camera collector stops submitting new frames
            val writer = activeWriter
            activeWriter = null
            // 2. Tell camera to stop frame delivery (clears analyzer + unbinds)
            cameraRepository.stopCapture()
            cameraFlowJob?.cancel()
            // 3. Drain in-flight writes (workers finish remaining queue items)
            writer?.stopAndDrain()
            // 4. Stop AR measurement collection
            arMeasurementJob?.cancel()
            // 5. Build the session
            val session = stopScanUseCase.buildSession(
                sessionUuid = sessionUuid,
                formData = formData,
                frames = frameRecords.toList(),
                measurement = latestMeasurement,
                scanStartTimestampMs = if (scanStartTimestampMs == 0L) System.currentTimeMillis() else scanStartTimestampMs,
                appVersion = readAppVersion(),
                deviceInfo = readDeviceInfo(),
                sessionDir = sessionDir ?: File(""),
            )
            // 6. Hand off to PackagingViewModel
            sessionHolder.submit(session)
            // 7. Pause AR session (lifecycle observer handles full teardown on screen exit)
            arRepository.pauseSession()
            // 8. Transition + emit navigation effect
            _uiState.value = ScanUiState.Complete
            _effects.tryEmit(ScanUiEffect.NavigateToPackaging)
        }
    }

    override fun onCleared() {
        super.onCleared()
        arSessionJob?.cancel()
        arMeasurementJob?.cancel()
        cameraFlowJob?.cancel()
        frameWrittenJob?.cancel()
        // R-13 safety net: ensure ARCore session is closed even if Activity lifecycle
        // teardown didn't reach the observer's onDestroy.
        runBlocking { runCatching { arRepository.destroySession() } }
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private fun startArSession() {
        val owner = lifecycleOwner ?: return
        arSessionJob = viewModelScope.launch {
            arRepository.startSession(owner).collect { event ->
                when (event) {
                    is ArSessionEvent.CameraShared -> {
                        arCameraId = event.cameraId
                        bindCameraIfReady()
                    }
                    ArSessionEvent.Ready -> {
                        if (_uiState.value is ScanUiState.Initializing) {
                            arDisplayState = ArDisplayState.TRACKING
                            _uiState.value = ScanUiState.Ready(torchOn, arDisplayState)
                        }
                    }
                    is ArSessionEvent.TrackingChanged -> { /* covered by measurement flow */ }
                    ArSessionEvent.Unsupported -> {
                        arDisplayState = ArDisplayState.UNSUPPORTED
                        _uiState.value = ScanUiState.Error(ScanStrings.ERROR_AR_UNSUPPORTED)
                    }
                    ArSessionEvent.InstallRequired -> {
                        _effects.tryEmit(ScanUiEffect.RequestArInstall)
                    }
                    is ArSessionEvent.Error -> {
                        _uiState.value = ScanUiState.Error(
                            event.cause.message ?: ScanStrings.ERROR_AR_UNSUPPORTED,
                        )
                    }
                }
            }
        }
    }

    /**
     * Starts the camera flow collector once BOTH the ARCore camera ID (from
     * [ArSessionEvent.CameraShared]) and the [Preview.SurfaceProvider] (from ScanScreen)
     * are available. Either ordering is possible.
     *
     * Frames are delivered into the [activeWriter]; before [onStartScan] is called,
     * [activeWriter] is null and frames are silently discarded (no IO work).
     */
    private fun bindCameraIfReady() {
        if (cameraBound) return
        val owner = lifecycleOwner ?: return
        val provider = surfaceProvider ?: return
        val cameraId = arCameraId ?: return

        cameraBound = true
        cameraFlowJob = viewModelScope.launch(dispatchers.io) {
            val config = CameraConfig(preferredCameraId = cameraId)
            try {
                cameraRepository.startCapture(owner, config, provider).collect { result ->
                    when (result) {
                        is FrameResult.Frame -> activeWriter?.submit(result.record, result.jpegBytes)
                        is FrameResult.Error -> {
                            Log.e(LOG_TAG, "Camera frame error: ${result.cause.message}", result.cause)
                            withContext(dispatchers.main) {
                                _uiState.value = ScanUiState.Error(ScanStrings.ERROR_CAMERA_UNAVAILABLE)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Camera startCapture failed: ${e.message}", e)
                withContext(dispatchers.main) {
                    _uiState.value = ScanUiState.Error(ScanStrings.ERROR_CAMERA_UNAVAILABLE)
                }
            }
        }
    }

    private fun mapToArDisplayState(m: ARMeasurement): ArDisplayState = when {
        m.trackingState == ArTrackingState.PAUSED || m.trackingState == ArTrackingState.STOPPED ->
            ArDisplayState.TRACKING_LOST
        m.pointB != null && m.distanceMeters != null ->
            ArDisplayState.MEASUREMENT_COMPLETE
        m.pointA != null ->
            ArDisplayState.POINT_A_CAPTURED
        else ->
            ArDisplayState.TRACKING
    }

    private fun updateFrameCount(count: Int) {
        _uiState.update { s ->
            if (s is ScanUiState.Scanning) s.copy(frameCount = count) else s
        }
    }

    private fun updateTorchInState() {
        _uiState.update { s ->
            when (s) {
                is ScanUiState.Ready -> s.copy(torchOn = torchOn)
                is ScanUiState.Scanning -> s.copy(torchOn = torchOn)
                else -> s
            }
        }
    }

    private fun readAppVersion(): String =
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "unknown"
        }.getOrDefault("unknown")

    private fun readDeviceInfo(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

    private fun decodeFormData(encoded: String): FormDataSnapshot {
        val decoded = URLDecoder.decode(encoded, Charsets.UTF_8.name())
        return Json.decodeFromString(FormDataSnapshot.serializer(), decoded)
    }

    companion object {
        private const val LOG_TAG = "ScanViewModel"
    }
}
