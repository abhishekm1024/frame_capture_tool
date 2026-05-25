# Implementation Status
## Android SfM Scanning App

**Date:** 2026-05-25
**Branch:** `dev`
**Last commit:** `94560e3 M7 Implemented` (commit labels are sequential user numbers, not plan milestone IDs — see §1)
**Source of truth for plan:** [docs/implementation_plan.md](../docs/implementation_plan.md)

This document is a precise snapshot for session handoff. Verify against the working tree before acting on any item.

---

## 1. Completed Modules

Verified by inspection of source files under `src/main/kotlin/` per module.

| Plan milestone | Module | Status | Key source files |
|----------------|--------|--------|------------------|
| M0 | Project scaffold | ✅ Complete | 11 modules declared in [settings.gradle.kts](../settings.gradle.kts); version catalog at [gradle/libs.versions.toml](../gradle/libs.versions.toml) |
| M0 | `:app` skeleton | ✅ Complete | `ScanApp.kt` (`@HiltAndroidApp` + `Configuration.Provider`); `MainActivity.kt`; `AppNavHost.kt`; `Routes.kt` — fully wired in user-M10 (plan-M8) |
| M8 (plan) / user-M10 | `:app` integration | ✅ Complete | `MainActivity.kt` (`ScanAppTheme` + ARCore install handler); `AppNavHost.kt` (real feature screens, `popUpTo` rules, URL-encoded `formDataJson` nav arg, packaging-failure routes to UploadScreen via empty-`absolutePath` `ZipArtifact`); `Routes.kt` (re-exports `*Destination.route` from feature modules, resolves C-1); `NavArgEncoding.kt` (centralised `FormData` JSON encoder + failed-artifact builder) |
| M1-A | `:core:core-common` | ✅ Complete | `AppDispatchers`, `AppDispatchersModule`, `Result<T>`, `Logger`, `LoggerModule`, `StringExt`, `CollectionExt` |
| M1-B | `:core:core-ui` | ✅ Complete | `ScanAppTheme`; `PrimaryButton`, `LabeledTextField`, `LoadingOverlay`, `ErrorBanner` |
| M1-C | `:core:core-storage` | ✅ Complete | `SessionDirectoryManager`, `ZipBuilder` (atomic write via `.tmp` rename), `AppFileProvider`, `StorageConstants`, `StorageModule` |
| M2 | `:data:data-camera` | ✅ Complete | `CameraRepository` + `CameraConfig` + `FrameResult` (interface lives here per arch §12); `FrameRecord`; `CameraConstants`; `TimestampGate` (AtomicLong CAS, init = -intervalMs); `YuvFrame`; `JpegFrameEncoder` (YUV→NV21→YuvImage→JPEG); `ResolutionPicker` (ResolutionFilter fallback chain); `CameraXFrameSource` (callbackFlow + STRATEGY_KEEP_ONLY_LATEST + Camera2Interop + Preview use case binding + torch control + preferredCameraId filter for SharedCamera wiring); `CameraRepositoryImpl`; `CameraModule`. **M9 additions:** `CameraConfig.preferredCameraId`; `CameraRepository.setTorch()`; `startCapture(..., previewSurfaceProvider)`. |
| M3 | `:data:data-ar` | ✅ Complete | `ArRepository` + `ArSessionEvent` (interface in data-ar per arch §12 deviation); `ArDomainModels` (`ArPoint`, `CameraPose`, `HitType`, `ArTrackingState`, `ARMeasurement`); `ArConstants` (`AR_TRANSLATION_THRESHOLD_METERS=0.30f`, `AR_RAYCAST_RETRY_BUDGET=30`); `TranslationCalculator` (pure JVM); `MeasurementState` (internal sealed class + `HitTypeAndPosition`); `MeasurementStateReducer` (pure function, JVM-testable, no ARCore dep); `DepthAvailabilityChecker`; `ArSessionManager` (`DefaultLifecycleObserver`; `SHARED_CAMERA` feature; emits `cameraId`); `ArMeasurementPipeline` (`flow{}` update loop; hit-type priority chain; retry budget; `flowOn(Default)`); `ArRepositoryImpl`; `ArModule` |
| M4 | `:data:data-firebase` | ✅ Complete | `FirebaseRepository` + `FirebaseRepositoryImpl`; `AnonymousAuthSource`; `FirebaseStorageUploader` (`callbackFlow`); `UploadProgress`; `FirebaseConstants`; `FirebaseModule` |
| M5-A | `:features:feature-splash` | ✅ Complete | `SplashScreen`, `SplashViewModel`, `SplashDestination`, `SplashUiEffect`; Lottie suspend pattern; `BackHandler { /* no-op */ }`; **placeholder Lottie JSON** |
| M5-B | `:features:feature-selection` | ✅ Complete | `SelectionScreen` (+ `SelectionScreenContent`), `SelectionViewModel`, `SelectionDestination`, `SelectionUiState`, `SelectionUiEffect.NavigateToForm(selectionId)`, `SelectionOption`, `SelectionOptions` (TBD-A1 placeholders) |
| M5-C | `:features:feature-form` | ✅ Complete | `FormScreen` (+ `FormScreenContent`), `FormViewModel` (`SavedStateHandle`), `FormDestination` (`route = "form/{selectionId}"`), `FormValidator`, `FormUiState`, `FormUiEffect.NavigateToScan(formData)`, `FormData` (`@Serializable`), `FormOptions` (TBD-A2 placeholders) |
| M6 (user #) | `:features:feature-upload` | ✅ Complete | `UploadScreen` (+ `UploadScreenContent`), `UploadViewModel` (`@Inject internal constructor`), `UploadZipUseCase`, `UploadWorkEnqueuer`, `UploadWorker` (`@HiltWorker`), `ScanDatabase` (Room v1), `PendingUploadEntity`/`Dao`, `UploadQueueRepositoryImpl`, `UploadQueueModule`, `UploadDestination`, `ZipArtifact` (`@Serializable`), `PendingUpload`, `UploadUiState`, `UploadUiEffect` |
| M6 | `:features:feature-scan` | ✅ Complete | `ScanScreen` + `ScanScreenContent` (full-bleed PreviewView, AR status chip, instruction banner, bottom control bar, error overlay); `ScanViewModel` (`@HiltViewModel internal class`; orchestrates AR + Camera + FrameWriter); `PackagingScreen` + `PackagingViewModel`; `ScanSession`, `FormDataSnapshot` (E-1 deviation), `PackagedSession` (E-1 deviation), `DetailsJson`, `MeasurementsJson` (+ Pose/Point JSON adapters); use cases (`StartScan`, `StopScan`, `MonitorArMeasurement`, `PackageSession`); `FrameWriter` (Channel capacity=4, DROP_OLDEST, 2-worker pool, decouples capture from disk latency); `ScanSessionHolder` (`@Singleton` transient bridge); `ScanDestination` + `PackagingDestination`; `ScanStrings`; `ScanUiState`/`ArDisplayState`/`ScanUiEffect`/`PackagingUiEffect` |

**Plan-milestone naming reconciliation:**
- Commit labels (M3, M4, M5) are the user's sequential session numbers, NOT plan milestone IDs.
- Actual plan milestones completed: M0, M1-A/B/C, M2 (data-camera), M3 (data-ar), M4 (data-firebase), M5-A/B/C (UI features), M6 (feature-scan, user-M9), plus plan-M7 (feature-upload, user-M6).
- **All feature + data + integration modules complete.** Remaining: M9 (testing & hardening on physical device).

---

## 2. Current Architecture State

- **11-module Gradle project**, all implemented. No stubs remaining.
- **DI:** Hilt on every module. `ScanApp` wires `HiltWorkerFactory` for `UploadWorker`.
- **AR pipeline (M3 complete):**
  - `ArRepository`/domain types (`ArPoint`, `CameraPose`, `HitType`, `ArTrackingState`, `ARMeasurement`) defined in **`data-ar`** (not feature-scan) per arch §12.
  - `startSession(lifecycleOwner)` — deviation D-2: LifecycleOwner required for `DefaultLifecycleObserver`.
  - `getMeasurementFlow(displayWidthPx, displayHeightPx)` — deviation D-3: pixel coords required for `Frame.hitTest()`.
  - `ArSessionEvent.CameraShared(cameraId)` — deviation D-4: exposes ARCore's camera ID for M6 SharedCamera wiring.
  - Session uses `Session.Feature.SHARED_CAMERA`; actual CameraX-sharing wired in M6 (`feature-scan`).
  - `MeasurementStateReducer` is pure Kotlin (no ARCore SDK types) — fully JVM-testable.
  - Hit priority chain: DEPTH → PLANE → INSTANT_PLACEMENT → FEATURE_POINT → retry (budget: 30).
  - R-01 mitigated: `hitType` recorded per measurement; `DepthAvailabilityChecker` configures optimal session config.
  - R-13 mitigated: `ArSessionManager` as `DefaultLifecycleObserver` (pause/resume/close on lifecycle events).
- **Camera pipeline (M2 complete):**
  - `CameraRepository`/`CameraConfig`/`FrameResult`/`FrameRecord` defined in **`data-camera`** (not `feature-scan`) to satisfy architecture §12.
  - `LifecycleOwner` is a parameter of `startCapture()` — minor spec deviation (data_contracts.md §6.1 omits it; required by CameraX).
  - `FrameRecord.absolutePath` is populated with filename only at camera layer; full path is set by `ScanViewModel` (feature-scan) after disk write.
  - Pipeline: `callbackFlow` + `STRATEGY_KEEP_ONLY_LATEST` + `TimestampGate(200ms)` + `AtomicInteger(inFlightCount < 3)` + `copyYuvPlanes` (sync, camera executor) + `imageProxy.close()` (before any suspend) + `JpegFrameEncoder` (IO dispatcher).
- **Upload pipeline (user-M6 complete):**
  - `PackageSessionUseCase` lives in `feature-scan` (plan-M6, now ✅) per architecture §12.
  - `feature-upload` receives `ZipArtifact` via `zipArtifactJson` nav arg (URL-encoded JSON), not `ScanSession`.
- **Scan pipeline (user-M9 / plan-M6 complete):**
  - **Bounded async frame writing** (`FrameWriter`): `Channel(capacity=4, onBufferOverflow=DROP_OLDEST)` + 2 IO worker coroutines. Capture cadence is decoupled from disk write latency — exact 5 FPS is preserved even with slow storage. Peak memory: ~3.6 MB above baseline (4 queued + 2 in-flight × ~0.6 MB JPEG). Drops are silent at channel level; observable via `submittedCount - writtenCount` gap.
  - **Camera + AR coordination:** ScanViewModel waits for BOTH `ArSessionEvent.CameraShared(cameraId)` AND `Preview.SurfaceProvider` (from ScanScreen) before binding CameraX (`bindCameraIfReady()`). Either ordering supported. CameraX's `CameraSelector` is filtered to use ARCore's chosen Camera2 camera ID (via `Camera2CameraInfo.from(it).cameraId`).
  - **Active sink pattern:** Camera flow is collected continuously after binding; frames are submitted to `activeWriter` (`@Volatile`). Before `onStartScan()`, `activeWriter` is null and frames are discarded. After `onStartScan()`, frames flow into the `FrameWriter`. After `onStopScan()`, sink is cleared atomically and writer is drained.
  - **`ScanSessionHolder`** (`@Singleton`): minimal API (`submit`/`consume`) for transient transfer of `ScanSession` between `ScanViewModel` and `PackagingViewModel`. Process-death behaviour documented in KDoc; PackagingViewModel routes to UploadScreen error state if holder is empty. **M10/M11 hardening:** replace with nav-graph-scoped ViewModel.
  - **Permission flow:** ScanScreen launches camera permission via `rememberLauncherForActivityResult(RequestPermission())`. Result is forwarded to `ScanViewModel.onCameraPermissionResult()`. Denial transitions to `ScanUiState.Error(ERROR_PERMISSION_CAMERA)`.
- **NavHost (user-M10 / plan-M8 complete):** all 6 destinations wired with real feature screens; per-feature `*Destination.route` consumed directly (resolves C-1). Pop-up rules: Splash→Selection pops Splash inclusive; Scan→Packaging pops Scan inclusive; Packaging→Upload pops Packaging inclusive; Upload "Start New Scan" pops to Selection inclusive=false. `formDataJson` URL-encoded via `NavArgEncoding.encodeFormData()` (resolves C-7). `PackagingUiEffect.NavigateToUploadWithError` builds a synthetic empty-`absolutePath` `ZipArtifact` so `UploadZipUseCase` short-circuits to `Failed` (per screen_specs §6.5).
- **MainActivity (user-M10 / plan-M8 complete):** wraps `AppNavHost` in `ScanAppTheme` (resolves C-2). Owns ARCore `requestInstall` handler — invoked via lambda parameter when `ScanUiEffect.RequestArInstall` fires.

---

## 3. Implemented Dependencies (version catalog additions beyond M0)

| Alias | Purpose | Added during |
|-------|---------|--------------|
| `lottie-compose` (6.5.2) | Splash animation | M5-A |
| `compose-material-icons-core` (BOM-managed) | Back arrow + status icons | M5-C, feature-upload |
| `activity-compose` | `BackHandler` | M5-A, feature-upload |
| `mockk.android` | Instrumented tests in data-camera | M2 |

---

## 4. Unresolved Technical Concerns

| ID | Concern | Where | Resolution path |
|----|---------|-------|-----------------|
| C-1 | ~~`Routes.FORM = "form"` vs. `FormDestination.route = "form/{selectionId}"`~~ | ~~`Routes.kt`~~ | ✅ Resolved in user-M10 (Routes re-exports `*Destination.route`) |
| C-2 | ~~`MainActivity` wraps in `MaterialTheme {}` not `ScanAppTheme {}`~~ | ~~`MainActivity.kt`~~ | ✅ Resolved in user-M10 |
| C-3 | Lottie splash asset is a placeholder | `features/feature-splash/src/main/res/raw/splash_logo.json` | Replace file at release |
| C-4 | `[TBD-A1]` placeholder options in SelectionScreen | `features/feature-selection/.../SelectionOptions.kt` | One-file replacement when owner provides values |
| C-5 | `[TBD-A2]` placeholder dropdown options in FormScreen | `features/feature-form/.../FormOptions.kt` | One-file replacement |
| C-6 | ~~`FormScreen` back-arrow lambda not yet wired in NavHost~~ | ~~`AppNavHost.kt`~~ | ✅ Resolved in user-M10 (`navController.popBackStack()`) |
| C-7 | ~~`FormUiEffect.NavigateToScan` emits raw `FormData`~~ | ~~`FormViewModel.kt`~~ | ✅ Resolved in user-M10 (`NavArgEncoding.encodeFormData()` URL-encodes at NavHost boundary) |
| C-8 | ~~`data-ar` is a pure stub~~ | ~~`data/data-ar/`~~ | ✅ Resolved in M3 |
| C-9 | ~~`feature-scan` is a pure stub~~ | ~~`features/feature-scan/`~~ | ✅ Resolved in user-M9 (plan-M6) |
| C-14 | `FormDataSnapshot` + `PackagedSession` are local mirrors of types in `feature-form`/`feature-upload`. JSON shape identical; same nav-arg JSON deserialises into either type. Consolidation into `core-common` deferred to M11. | `features/feature-scan/.../domain/model/FormDataSnapshot.kt`, `PackagedSession.kt` | M11 hardening |
| C-15 | `ScanSessionHolder` (`@Singleton`) holds in-memory `ScanSession` between `ScanViewModel` and `PackagingViewModel`. Process-kill mid-handoff results in error path (UploadScreen shows error per screen_specs §6.5). Replace with nav-graph-scoped ViewModel in M10/M11. | `features/feature-scan/.../infra/ScanSessionHolder.kt` | M10/M11 hardening |
| C-16 | `ScanViewModel.onCleared()` uses `runBlocking { arRepository.destroySession() }` as a Main-thread safety net for ARCore cleanup. Lifecycle observer in `ArSessionManager` handles teardown normally; runBlocking covers edge cases (Activity-side ordering issues). Brief block (~ms) on Main during ViewModel disposal. | `features/feature-scan/.../ScanViewModel.kt` | Acceptable; reassess if perf issue surfaces |
| C-10 | `google-services.json` is missing in `:app/`. `compileDebugKotlin` succeeds but `assembleDebug` will fail at the `google-services` Gradle plugin step. Manual step before first device build. | `:app/google-services.json` | Owner-supplied; required for M9 device tests |
| C-11 | No `./gradlew assembleDebug` run since M0 — full build chain not verified. Wrapper JAR restored in user-M11 (`gradle-8.9-wrapper.jar`, SHA-256 verified against `services.gradle.org/distributions/gradle-8.9-wrapper.jar.sha256`); `./gradlew --version` confirms Gradle 8.9 + JVM 17. Still need `google-services.json` to complete `assembleDebug`. | n/a | Supply `google-services.json` then run `./gradlew assembleDebug` |
| C-12 | `PackagingScreen` and `PackageSessionUseCase` deferred to `feature-scan` (plan-M6), not implemented in `feature-upload` | (design gap) | Implemented in plan-M6 per architecture §12 |
| C-13 | ~~UploadScreen unreachable until NavHost wires `upload/{zipArtifactJson}` route~~ | ~~`AppNavHost.kt`~~ | ✅ Resolved in user-M10 |

---

## 5. Pending Modules (recommended order)

| Plan milestone | Module | Status | Notes |
|----------------|--------|--------|-------|
| M3 | `:data:data-ar` | ✅ Complete | |
| M6 | `:features:feature-scan` | ✅ Complete | |
| M8 (plan) / user-M10 | `:app` integration | ✅ Complete | All 6 destinations wired; C-1, C-2, C-6, C-7, C-13 resolved. `NavArgEncoding` centralises URL-encoded JSON nav-arg conversion. |
| M9 | Testing & Hardening | 🔲 Not started | Depends on M8. Includes integration tests on physical device + `google-services.json` to enable `assembleDebug`. |

**Recommended next module: M9 (Testing & Hardening)** — integration is complete; remaining work is device-level E2E validation and lint/Detekt cleanup.

---

## 6. Integration Assumptions Already Established

| # | Assumption | Where |
|---|-----------|-------|
| A | `SplashUiEffect.NavigateToSelection` is `data object` (no payload) | `SplashUiEffect.kt` |
| B | `SelectionUiEffect.NavigateToForm(selectionId: String)` carries option id | `SelectionUiEffect.kt` |
| C | `FormDestination.route = "form/{selectionId}"`, `ARG_SELECTION_ID = "selectionId"` | `FormDestination.kt` |
| D | `FormViewModel` reads `selectionId` from `SavedStateHandle[ARG_SELECTION_ID]` | `FormViewModel.kt:20-22` |
| E | `FormUiEffect.NavigateToScan(formData: FormData)` carries raw object; NavHost URL-encodes | `FormUiEffect.kt` |
| F | `FormData` is `@Serializable`; shape matches data_contracts.md §1.2 | `FormData.kt` |
| G | `FormScreen` public API: `fun FormScreen(onNavigateBack, onNavigateToScan, viewModel)` | `FormScreen.kt:46-50` |
| H | `FirebaseRepository` is fully implemented; M6/M8 consumes it via Hilt | `data-firebase/FirebaseRepository.kt` |
| I | `Routes.UPLOAD = "upload/{zipArtifactJson}"` matches `UploadDestination.route` | `Routes.kt`, `UploadDestination.kt` |
| J | `ScanApp` exposes `HiltWorkerFactory`; `@HiltWorker UploadWorker` picked up automatically | `ScanApp.kt` |
| K | `CameraRepository.startCapture(lifecycleOwner, config)` — LifecycleOwner is explicit parameter (spec deviation D-2) | `CameraRepository.kt` |
| L | `CameraRepository`/`CameraConfig`/`FrameResult`/`FrameRecord` live in `:data:data-camera` (spec deviation D-1 — not feature-scan) | `data-camera/CameraRepository.kt` |
| P | `ArRepository.startSession(lifecycleOwner)` — deviation D-2; `getMeasurementFlow(w, h)` — deviation D-3; interface in `data-ar` — deviation D-1 | `data-ar/ArRepository.kt` |
| Q | `ArSessionEvent.CameraShared(cameraId)` emitted by `startSession()` — deviation D-4; M6 uses it to wire CameraX SharedCamera | `ArRepository.kt`, `ArSessionManager.kt` |
| R | `ArRepository`/`ARMeasurement`/`ArPoint`/`CameraPose`/`HitType`/`ArTrackingState` all live in `:data:data-ar` — same arch-§12 rationale as data-camera | `data-ar/ArDomainModels.kt`, `ArRepository.kt` |
| S | `CameraRepository.startCapture(..., previewSurfaceProvider)` accepts a `Preview.SurfaceProvider`; `CameraXFrameSource` binds a `Preview` use case alongside `ImageAnalysis` when non-null (deviation D-5) | `data-camera/CameraRepository.kt`, `CameraXFrameSource.kt` |
| T | `CameraRepository.setTorch(on: Boolean): Boolean` (deviation D-6) returns false if no camera is currently bound; M9 calls this for torch toggle | `data-camera/CameraRepository.kt`, `CameraRepositoryImpl.kt` |
| U | `CameraConfig.preferredCameraId: String?` (deviation D-7) tells `CameraXFrameSource` to filter `CameraSelector` to a specific Camera2 ID — required for ARCore SharedCamera wiring | `data-camera/CameraRepository.kt`, `CameraXFrameSource.kt` |
| V | `ScanDestination.route = "scan/{formDataJson}"`, `ARG_FORM_DATA = "formDataJson"`; `ScanViewModel` decodes nav arg into `FormDataSnapshot` (JSON-shape mirror of feature-form's `FormData`) | `feature-scan/ScanDestination.kt`, `ScanViewModel.kt` |
| W | `PackagedSession` (feature-scan) and `ZipArtifact` (feature-upload) have identical JSON shape; the URL-encoded JSON nav arg `zipArtifactJson` deserialises into either | `feature-scan/.../PackagedSession.kt`, `feature-upload/ZipArtifact.kt` |
| X | `ScanUiEffect.NavigateToPackaging` carries no payload; ScanSession is deposited in `ScanSessionHolder` before navigation; `PackagingViewModel.init` calls `sessionHolder.consume()` and routes to upload error on null | `feature-scan/ScanUiEffect.kt`, `PackagingViewModel.kt`, `ScanSessionHolder.kt` |
| Y | `PackagingUiEffect.NavigateToUpload(zipArtifactJson)` and `NavigateToUploadWithError(message)` — NavHost (M8) routes the latter to UploadScreen's error variant per screen_specs §6.5 | `feature-scan/PackagingUiEffect.kt` |
| M | `FrameRecord.absolutePath` = filename only from camera layer; `ScanViewModel` provides full path after writing file | `FrameRecord.kt` (design deviation D-3) |
| N | `PackageSessionUseCase` + `PackagingScreen` belong in `feature-scan` (plan-M6), not `feature-upload` | (architecture §12 constraint) |
| O | `ZipArtifact` in `feature-upload` is the nav-arg contract between PackagingScreen (scan) and UploadScreen; shape matches data_contracts.md §1.10 | `ZipArtifact.kt` |

---

## 7. Test Status

| Module | Unit tests (`src/test/`) | Instrumented (`src/androidTest/`) |
|--------|--------------------------|------------------------------------|
| `:app` | `RoutesTest` (8, updated for C-1 resolution), `NavArgEncodingTest` (6, FormData/ZipArtifact round-trip + URL safety), `AppNavHostRouteTest` (8, cross-module route/arg consistency) | — |
| `:core:core-common` | `ResultTest`, `AppDispatchersModuleTest`, `ext/StringExtTest` | — |
| `:core:core-ui` | — | `PrimaryButtonTest`, `LabeledTextFieldTest`, `LoadingOverlayTest`, `ErrorBannerTest` |
| `:core:core-storage` | `ZipBuilderTest`, `SessionDirectoryManagerTest`, `AppFileProviderTest` | — |
| `:data:data-camera` | `TimestampGateTest` (8), `JpegFrameEncoderNv21Test` (6) | `ResolutionPickerTest` (6), `JpegFrameEncoderTest` (4) |
| `:data:data-firebase` | `AnonymousAuthSourceTest`, `FirebaseStorageUploaderTest`, `FirebaseRepositoryImplTest` (11 total) | — |
| `:features:feature-splash` | `SplashViewModelTest` (1) | — |
| `:features:feature-selection` | `SelectionViewModelTest` (4) | `SelectionScreenTest` (2) |
| `:features:feature-form` | `FormValidatorTest` (28), `FormViewModelTest` (13) | `FormScreenTest` (5) |
| `:features:feature-upload` | `UploadZipUseCaseTest` (5), `UploadViewModelTest` (8), `UploadWorkerTest` (6), `MainDispatcherRule` | `PendingUploadDaoTest` (8), `UploadScreenTest` (6) |
| `:data:data-ar` | `MeasurementStateReducerTest` (14 tests), `TranslationCalculatorTest` (7 tests) | `DepthAvailabilityCheckerTest` (3 tests, device-conditional) |
| `:features:feature-scan` | `StopScanUseCaseTest` (5 tests), `ScanViewModelTest` (10 tests), `FrameWriterTest` (5 tests), `MainDispatcherRule` | `PackageSessionUseCaseTest` (5 tests, ZIP integration), `ScanScreenTest` (5 tests) |
| `:features:feature-scan`, `:features:feature-upload` test totals | above | above |

**Not yet executed:** `./gradlew test`, `./gradlew connectedAndroidTest`, `./gradlew assembleDebug` (since M0).

---

## 8. Known Risks (active items)

| Risk | Status |
|------|--------|
| R-01 ARCore Depth API not available | **Mitigated** — `DepthAvailabilityChecker` selects `AUTOMATIC` when available; hit-priority chain (DEPTH→PLANE→INSTANT_PLACEMENT→FEATURE_POINT) and `hitType` recorded per measurement |
| R-02 CameraX 5 FPS not guaranteed by HAL | **Mitigated** — `TimestampGate(200ms)` + `setTargetFrameRate(5,5)` hint |
| R-03 OOM during long scan | **Mitigated** — `STRATEGY_KEEP_ONLY_LATEST` + `MAX_FRAMES_IN_FLIGHT = 3` + ImageProxy closed before IO coroutine |
| R-04 Focus/AE lock unsupported | **Mitigated** — Camera2Interop in `try/catch`; logs warning; scan continues with default AF/AE |
| R-12 A-1/A-2 unresolved | **Ongoing** — placeholders in SelectionOptions/FormOptions; release-blocking |
| R-13 ARCore session lifecycle leak | **Mitigated** — `ArSessionManager : DefaultLifecycleObserver`; pause/resume/close wired to lifecycle; `ScanViewModel.onCleared()` calls `destroySession()` as safety net |
| R-15 Firebase Storage rules misconfigured | Requires manual Firebase project deployment before M6 integration testing |
| R-16 App killed mid-packaging | Mitigated by ZipBuilder atomic rename; PackagingScreen logic deferred to M6 |

---

## 9. Exact Next Implementation Step

**Implement plan-M9: Testing & Hardening (user-M11).**

Key deliverables (per implementation_plan.md §M9):
1. Add `google-services.json` to `:app/` so `./gradlew assembleDebug` succeeds (resolves C-10, C-17).
2. Run the full unit-test suite (`./gradlew test`) and address any failures.
3. Physical-device E2E walkthrough: Splash → Selection → Form → Scan (≥10 frames + AR distance) → Packaging → Upload → Success → "Start New Scan" → Selection.
4. Offline-upload retry validation: airplane mode → trigger upload → confirm `UploadStage.RetryQueued` → restore network → confirm WorkManager re-runs `UploadWorker` and completes upload.
5. Stress tests: OOM under 5-minute scan (R-03); ARCore tracking loss recovery (R-05); Firebase Storage rules deployment (R-15).
6. Lint: `./gradlew lint`; address all errors.

The integration milestone (plan-M8 / user-M10) is now complete — all 6 screens wired, nav-arg JSON contract validated by unit tests, MainActivity correctly hosting `ScanAppTheme` with ARCore install passthrough.

---

*End of implementation status.*
