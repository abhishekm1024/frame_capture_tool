package com.sfm.scanner.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sfm.scanner.core.common.AppDispatchers
import com.sfm.scanner.feature.scan.domain.model.PackagedSession
import com.sfm.scanner.feature.scan.domain.usecase.PackageSessionUseCase
import com.sfm.scanner.feature.scan.infra.ScanSessionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Drives the transient PackagingScreen: builds the upload ZIP via [PackageSessionUseCase],
 * then emits a navigation effect with the resulting [PackagedSession] JSON-encoded for the
 * UploadScreen nav arg (which decodes it as `ZipArtifact`).
 *
 * Process-death behaviour: if [ScanSessionHolder.consume] returns null (process killed
 * between scan completion and packaging start), an error effect is emitted so UploadScreen
 * shows the failure state per screen_specs §6.5.
 */
@HiltViewModel
class PackagingViewModel @Inject internal constructor(
    private val packageSessionUseCase: PackageSessionUseCase,
    private val sessionHolder: ScanSessionHolder,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _effects = MutableSharedFlow<PackagingUiEffect>(replay = 0, extraBufferCapacity = 1)
    val effects: SharedFlow<PackagingUiEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            // Read-and-clear; survives a recomposition but not a process kill.
            val session = sessionHolder.consume()
            if (session == null) {
                _effects.tryEmit(
                    PackagingUiEffect.NavigateToUploadWithError("Session data lost; please re-scan."),
                )
                return@launch
            }

            val result = withContext(dispatchers.io) {
                packageSessionUseCase.execute(session)
            }

            result
                .onSuccess { packaged ->
                    val encoded = encode(packaged)
                    _effects.tryEmit(PackagingUiEffect.NavigateToUpload(encoded))
                }
                .onFailure { cause ->
                    _effects.tryEmit(
                        PackagingUiEffect.NavigateToUploadWithError(
                            cause.message ?: "Packaging failed",
                        ),
                    )
                }
        }
    }

    private fun encode(packaged: PackagedSession): String {
        val json = Json.encodeToString(PackagedSession.serializer(), packaged)
        return URLEncoder.encode(json, Charsets.UTF_8.name())
    }
}
