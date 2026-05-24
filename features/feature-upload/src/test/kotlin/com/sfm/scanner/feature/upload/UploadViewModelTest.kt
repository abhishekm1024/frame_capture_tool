package com.sfm.scanner.feature.upload

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.URLEncoder

class UploadViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testArtifact = ZipArtifact(
        sessionUUID = "uuid-abc",
        zipFilename = "uuid-abc_12345.zip",
        absolutePath = "/data/user/0/com.sfm/files/uploads/uuid-abc_12345.zip",
        fileSizeBytes = 1024L,
    )

    private fun savedStateHandleWith(artifact: ZipArtifact): SavedStateHandle {
        val json = Json.encodeToString(ZipArtifact.serializer(), artifact)
        val encoded = URLEncoder.encode(json, Charsets.UTF_8.name())
        return SavedStateHandle(mapOf(UploadDestination.ARG_ZIP_ARTIFACT to encoded))
    }

    private fun viewModel(
        artifact: ZipArtifact = testArtifact,
        stageFlow: kotlinx.coroutines.flow.Flow<UploadStage> = flowOf(),
    ): UploadViewModel {
        val useCase: UploadZipUseCase = mockk()
        every { useCase.invoke(any()) } returns stageFlow
        return UploadViewModel(
            savedStateHandle = savedStateHandleWith(artifact),
            uploadZipUseCase = useCase,
        )
    }

    @Test
    fun `initial state is Authenticating before flow emits`() {
        val vm = viewModel(stageFlow = MutableSharedFlow())
        assertEquals(UploadUiState.Authenticating, vm.uiState.value)
    }

    @Test
    fun `Authenticating stage maps to Authenticating UI state`() {
        val vm = viewModel(stageFlow = flowOf(UploadStage.Authenticating))
        assertEquals(UploadUiState.Authenticating, vm.uiState.value)
    }

    @Test
    fun `Uploading stage maps to Uploading UI state with filename`() {
        val vm = viewModel(stageFlow = flowOf(UploadStage.Uploading(42)))
        val state = vm.uiState.value
        assertTrue(state is UploadUiState.Uploading)
        state as UploadUiState.Uploading
        assertEquals(42, state.percent)
        assertEquals(testArtifact.zipFilename, state.filename)
    }

    @Test
    fun `Success stage maps to Success UI state`() {
        val vm = viewModel(stageFlow = flowOf(UploadStage.Success))
        assertEquals(UploadUiState.Success, vm.uiState.value)
    }

    @Test
    fun `Failed stage maps to Failed UI state with filename`() {
        val vm = viewModel(stageFlow = flowOf(UploadStage.Failed(RuntimeException("oops"))))
        val state = vm.uiState.value
        assertTrue(state is UploadUiState.Failed)
        assertEquals(testArtifact.zipFilename, (state as UploadUiState.Failed).filename)
    }

    @Test
    fun `RetryQueued stage maps to RetryQueued UI state`() {
        val vm = viewModel(stageFlow = flowOf(UploadStage.RetryQueued))
        assertEquals(UploadUiState.RetryQueued, vm.uiState.value)
    }

    @Test
    fun `state transitions through full upload lifecycle`() = runTest {
        val vm = viewModel(
            stageFlow = flow {
                emit(UploadStage.Authenticating)
                emit(UploadStage.Uploading(10))
                emit(UploadStage.Uploading(90))
                emit(UploadStage.Success)
            },
        )
        // Because UnconfinedTestDispatcher is installed as Main, the init-block flow
        // runs synchronously. The final state should be Success.
        assertEquals(UploadUiState.Success, vm.uiState.value)
    }

    @Test
    fun `onStartNewScan emits NavigateToSelection effect`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onStartNewScan()
            assertEquals(UploadUiEffect.NavigateToSelection, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
