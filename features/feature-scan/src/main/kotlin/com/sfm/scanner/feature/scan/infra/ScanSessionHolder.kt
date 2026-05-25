package com.sfm.scanner.feature.scan.infra

import com.sfm.scanner.feature.scan.domain.model.ScanSession
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory transient bridge for handing a [ScanSession] from [ScanViewModel] to
 * [PackagingViewModel] across separate navigation destinations.
 *
 * Architectural decision (approved): TEMPORARY pattern for M9.
 *  - Architecture §4 forbids passing ScanSession as a nav argument (too large —
 *    contains the in-memory frame list and ARMeasurement).
 *  - The two ViewModels have separate scopes and cannot share `SavedStateHandle`.
 *  - A nav-graph-scoped ViewModel would be the architecturally cleaner alternative
 *    (see M10/M11 hardening).
 *
 * Lifecycle ownership: `@Singleton` (held in `SingletonComponent` for the process lifetime).
 *
 * **Process-death behaviour:** If the process is killed between ScanViewModel writing the
 * session and PackagingViewModel reading it, the holder is reset to null on next process
 * start. PackagingViewModel detects null and routes to the upload error state via
 * `PackagingUiEffect.NavigateToUploadWithError("Session lost — please re-scan")`. The
 * session JPEG files in cacheDir are orphaned and swept on next cold start by
 * `SessionDirectoryManager.cleanupStaleSessions()`.
 *
 * Minimal API surface (intentionally narrow):
 *  - [submit]: ScanViewModel writes a session (replaces any previous).
 *  - [consume]: PackagingViewModel reads-and-clears atomically (returns null if none).
 */
@Singleton
internal class ScanSessionHolder @Inject constructor() {

    private val ref = AtomicReference<ScanSession?>(null)

    fun submit(session: ScanSession) {
        ref.set(session)
    }

    fun consume(): ScanSession? = ref.getAndSet(null)
}
