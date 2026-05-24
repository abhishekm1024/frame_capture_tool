package com.sfm.scanner.feature.upload

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    private val uploadZipUseCase: UploadZipUseCase,
) : ViewModel() {

    private val zipArtifact: ZipArtifact = decodeZipArtifact(
        checkNotNull(savedStateHandle[UploadDestination.ARG_ZIP_ARTIFACT]),
    )

    private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Authenticating)
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UploadUiEffect>(replay = 0, extraBufferCapacity = 1)
    val effects: SharedFlow<UploadUiEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            uploadZipUseCase(zipArtifact).collect { stage ->
                _uiState.value = mapStage(stage)
            }
        }
    }

    fun onStartNewScan() {
        _effects.tryEmit(UploadUiEffect.NavigateToSelection)
    }

    private fun mapStage(stage: UploadStage): UploadUiState = when (stage) {
        UploadStage.Authenticating -> UploadUiState.Authenticating
        is UploadStage.Uploading -> UploadUiState.Uploading(stage.percent, zipArtifact.zipFilename)
        UploadStage.Success -> UploadUiState.Success
        is UploadStage.Failed -> UploadUiState.Failed(zipArtifact.zipFilename)
        UploadStage.RetryQueued -> UploadUiState.RetryQueued
    }

    private fun decodeZipArtifact(encoded: String): ZipArtifact {
        val decoded = URLDecoder.decode(encoded, Charsets.UTF_8.name())
        return Json.decodeFromString(ZipArtifact.serializer(), decoded)
    }
}
