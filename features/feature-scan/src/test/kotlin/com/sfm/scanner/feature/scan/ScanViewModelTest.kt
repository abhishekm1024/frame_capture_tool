package com.sfm.scanner.feature.scan

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.sfm.scanner.core.common.AppDispatchers
import com.sfm.scanner.data.ar.ARMeasurement
import com.sfm.scanner.data.ar.ArRepository
import com.sfm.scanner.data.ar.ArSessionEvent
import com.sfm.scanner.data.ar.ArTrackingState
import com.sfm.scanner.data.ar.HitType
import com.sfm.scanner.data.camera.CameraRepository
import com.sfm.scanner.feature.scan.domain.model.FormDataSnapshot
import com.sfm.scanner.feature.scan.domain.usecase.MonitorArMeasurementUseCase
import com.sfm.scanner.feature.scan.domain.usecase.StartScanUseCase
import com.sfm.scanner.feature.scan.domain.usecase.StopScanUseCase
import com.sfm.scanner.feature.scan.infra.ScanSessionHolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.net.URLEncoder

class ScanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var arRepository: ArRepository
    private lateinit var cameraRepository: CameraRepository
    private lateinit var startScanUseCase: StartScanUseCase
    private lateinit var stopScanUseCase: StopScanUseCase
    private lateinit var monitorArMeasurementUseCase: MonitorArMeasurementUseCase
    private lateinit var sessionHolder: ScanSessionHolder
    private lateinit var context: Context
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var arSessionEvents: MutableSharedFlow<ArSessionEvent>

    private val testFormData = FormDataSnapshot(
        initialSelection = "option_a",
        dropdownSelection = "type_a",
        size = 5,
        detail = "ABC",
        gt = null,
    )

    @Before
    fun setUp() {
        arRepository = mockk(relaxUnitFun = true)
        cameraRepository = mockk(relaxUnitFun = true)
        startScanUseCase = mockk()
        stopScanUseCase = StopScanUseCase()
        monitorArMeasurementUseCase = mockk()
        sessionHolder = ScanSessionHolder()
        context = mockk()
        lifecycleOwner = mockk(relaxed = true)
        arSessionEvents = MutableSharedFlow(extraBufferCapacity = 8)

        // Default mocks
        every { arRepository.startSession(any()) } returns arSessionEvents
        every { cameraRepository.startCapture(any(), any(), any()) } returns emptyFlow()
        coEvery { cameraRepository.setTorch(any()) } returns true
        every { monitorArMeasurementUseCase.execute(any(), any()) } returns emptyFlow()
        every { startScanUseCase.createSessionDirectory(any()) } returns File.createTempFile(
            "test-session-",
            "-frames",
        ).apply { delete(); mkdirs() }

        // Context mocks for app version + device info
        every { context.packageName } returns "com.sfm.scanner"
        val pkgManager = mockk<PackageManager>()
        every { context.packageManager } returns pkgManager
        val pkgInfo = PackageInfo().apply { versionName = "1.0.0" }
        every { pkgManager.getPackageInfo("com.sfm.scanner", 0) } returns pkgInfo
    }

    private fun buildViewModel(): ScanViewModel {
        val json = Json.encodeToString(FormDataSnapshot.serializer(), testFormData)
        val encoded = URLEncoder.encode(json, Charsets.UTF_8.name())
        val savedStateHandle = SavedStateHandle(mapOf(ScanDestination.ARG_FORM_DATA to encoded))
        return ScanViewModel(
            savedStateHandle = savedStateHandle,
            arRepository = arRepository,
            cameraRepository = cameraRepository,
            startScanUseCase = startScanUseCase,
            stopScanUseCase = stopScanUseCase,
            monitorArMeasurementUseCase = monitorArMeasurementUseCase,
            sessionHolder = sessionHolder,
            dispatchers = AppDispatchers(
                io = UnconfinedTestDispatcher(),
                default = UnconfinedTestDispatcher(),
                main = UnconfinedTestDispatcher(),
            ),
            context = context,
        )
    }

    @Test
    fun `initial state is Initializing`() = runTest {
        val vm = buildViewModel()
        assertEquals(ScanUiState.Initializing, vm.uiState.value)
    }

    @Test
    fun `Ready event transitions state to Ready`() = runTest {
        val vm = buildViewModel()
        vm.onScreenEntered(lifecycleOwner, 1080, 1920)
        arSessionEvents.emit(ArSessionEvent.Ready)
        val state = vm.uiState.value
        assertTrue("expected Ready got $state", state is ScanUiState.Ready)
    }

    @Test
    fun `Unsupported event transitions state to Error AR unavailable`() = runTest {
        val vm = buildViewModel()
        vm.onScreenEntered(lifecycleOwner, 1080, 1920)
        arSessionEvents.emit(ArSessionEvent.Unsupported)
        val state = vm.uiState.value
        assertTrue(state is ScanUiState.Error)
        assertEquals(ScanStrings.ERROR_AR_UNSUPPORTED, (state as ScanUiState.Error).message)
    }

    @Test
    fun `InstallRequired emits RequestArInstall effect`() = runTest {
        val vm = buildViewModel()
        vm.effects.test {
            vm.onScreenEntered(lifecycleOwner, 1080, 1920)
            arSessionEvents.emit(ArSessionEvent.InstallRequired)
            assertEquals(ScanUiEffect.RequestArInstall, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `camera permission denied transitions to Error permission required`() = runTest {
        val vm = buildViewModel()
        vm.onCameraPermissionResult(false)
        val state = vm.uiState.value
        assertTrue(state is ScanUiState.Error)
        assertEquals(ScanStrings.ERROR_PERMISSION_CAMERA, (state as ScanUiState.Error).message)
    }

    @Test
    fun `camera permission granted does not change state`() = runTest {
        val vm = buildViewModel()
        vm.onCameraPermissionResult(true)
        assertEquals(ScanUiState.Initializing, vm.uiState.value)
    }

    @Test
    fun `onStartScan from non-Ready state is ignored`() = runTest {
        val vm = buildViewModel()
        vm.onStartScan()
        assertEquals(ScanUiState.Initializing, vm.uiState.value)
    }

    @Test
    fun `onStopScan from non-Scanning state is ignored`() = runTest {
        val vm = buildViewModel()
        vm.onStopScan()
        assertEquals(ScanUiState.Initializing, vm.uiState.value)
    }

    @Test
    fun `torch toggle flips the torch flag`() = runTest {
        val vm = buildViewModel()
        vm.onScreenEntered(lifecycleOwner, 1080, 1920)
        arSessionEvents.emit(ArSessionEvent.Ready)

        vm.onToggleTorch()
        val state = vm.uiState.value
        assertTrue(state is ScanUiState.Ready)
        assertTrue("torch should be on after first toggle", (state as ScanUiState.Ready).torchOn)

        coVerify(atLeast = 1) { cameraRepository.setTorch(true) }
    }

    @Test
    fun `Ready measurement maps to ArDisplayState TRACKING`() = runTest {
        val vm = buildViewModel()
        vm.onScreenEntered(lifecycleOwner, 1080, 1920)
        arSessionEvents.emit(ArSessionEvent.Ready)
        val state = vm.uiState.value as ScanUiState.Ready
        assertEquals(ArDisplayState.TRACKING, state.arDisplayState)
    }

    @Test
    fun `error from AR session uses cause message`() = runTest {
        val vm = buildViewModel()
        vm.onScreenEntered(lifecycleOwner, 1080, 1920)
        arSessionEvents.emit(ArSessionEvent.Error(RuntimeException("Boom")))
        val state = vm.uiState.value
        assertTrue(state is ScanUiState.Error)
        assertEquals("Boom", (state as ScanUiState.Error).message)
    }
}
