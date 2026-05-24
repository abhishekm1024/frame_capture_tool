# Implementation Status
## Android SfM Scanning App

**Date:** 2026-05-24
**Branch:** `dev`
**Last commit:** `9a215b8 M5 Completed` (commit labels are sequential user numbers, not plan milestone IDs — see §1)
**Source of truth for plan:** [docs/implementation_plan.md](../docs/implementation_plan.md)

This document is a precise snapshot for session handoff. Verify against the working tree before acting on any item.

---

## 1. Completed Modules

Verified by inspection of source files under `src/main/kotlin/` per module.

| Plan milestone | Module | Status | Key source files |
|----------------|--------|--------|------------------|
| M0 | Project scaffold | ✅ Complete | 11 modules declared in [settings.gradle.kts](../settings.gradle.kts); version catalog at [gradle/libs.versions.toml](../gradle/libs.versions.toml) |
| M0 | `:app` skeleton | ✅ Complete | `ScanApp.kt` (`@HiltAndroidApp` + `Configuration.Provider`); `MainActivity.kt` (`enableEdgeToEdge`, hosts `AppNavHost`); `AppNavHost.kt` (6 routes, **all bodies are placeholder `Box` composables**); `Routes.kt` |
| M1-A | `:core:core-common` | ✅ Complete | `AppDispatchers`, `AppDispatchersModule`, `Result<T>`, `Logger`, `LoggerModule`, `StringExt`, `CollectionExt` |
| M1-B | `:core:core-ui` | ✅ Complete | `ScanAppTheme`; `PrimaryButton`, `LabeledTextField`, `LoadingOverlay`, `ErrorBanner` |
| M1-C | `:core:core-storage` | ✅ Complete | `SessionDirectoryManager`, `ZipBuilder` (atomic write via `.tmp` rename), `AppFileProvider`, `StorageConstants`, `StorageModule` |
| M2 | `:data:data-camera` | ✅ Complete | `CameraRepository` + `CameraConfig` + `FrameResult` (interface lives here per arch §12); `FrameRecord`; `CameraConstants`; `TimestampGate` (AtomicLong CAS, init = -intervalMs); `YuvFrame`; `JpegFrameEncoder` (YUV→NV21→YuvImage→JPEG); `ResolutionPicker` (ResolutionFilter fallback chain); `CameraXFrameSource` (callbackFlow + STRATEGY_KEEP_ONLY_LATEST + Camera2Interop); `CameraRepositoryImpl`; `CameraModule` |
| M3 | `:data:data-ar` | ✅ Complete | `ArRepository` + `ArSessionEvent` (interface in data-ar per arch §12 deviation); `ArDomainModels` (`ArPoint`, `CameraPose`, `HitType`, `ArTrackingState`, `ARMeasurement`); `ArConstants` (`AR_TRANSLATION_THRESHOLD_METERS=0.30f`, `AR_RAYCAST_RETRY_BUDGET=30`); `TranslationCalculator` (pure JVM); `MeasurementState` (internal sealed class + `HitTypeAndPosition`); `MeasurementStateReducer` (pure function, JVM-testable, no ARCore dep); `DepthAvailabilityChecker`; `ArSessionManager` (`DefaultLifecycleObserver`; `SHARED_CAMERA` feature; emits `cameraId`); `ArMeasurementPipeline` (`flow{}` update loop; hit-type priority chain; retry budget; `flowOn(Default)`); `ArRepositoryImpl`; `ArModule` |
| M4 | `:data:data-firebase` | ✅ Complete | `FirebaseRepository` + `FirebaseRepositoryImpl`; `AnonymousAuthSource`; `FirebaseStorageUploader` (`callbackFlow`); `UploadProgress`; `FirebaseConstants`; `FirebaseModule` |
| M5-A | `:features:feature-splash` | ✅ Complete | `SplashScreen`, `SplashViewModel`, `SplashDestination`, `SplashUiEffect`; Lottie suspend pattern; `BackHandler { /* no-op */ }`; **placeholder Lottie JSON** |
| M5-B | `:features:feature-selection` | ✅ Complete | `SelectionScreen` (+ `SelectionScreenContent`), `SelectionViewModel`, `SelectionDestination`, `SelectionUiState`, `SelectionUiEffect.NavigateToForm(selectionId)`, `SelectionOption`, `SelectionOptions` (TBD-A1 placeholders) |
| M5-C | `:features:feature-form` | ✅ Complete | `FormScreen` (+ `FormScreenContent`), `FormViewModel` (`SavedStateHandle`), `FormDestination` (`route = "form/{selectionId}"`), `FormValidator`, `FormUiState`, `FormUiEffect.NavigateToScan(formData)`, `FormData` (`@Serializable`), `FormOptions` (TBD-A2 placeholders) |
| M6 (user #) | `:features:feature-upload` | ✅ Complete | `UploadScreen` (+ `UploadScreenContent`), `UploadViewModel` (`@Inject internal constructor`), `UploadZipUseCase`, `UploadWorkEnqueuer`, `UploadWorker` (`@HiltWorker`), `ScanDatabase` (Room v1), `PendingUploadEntity`/`Dao`, `UploadQueueRepositoryImpl`, `UploadQueueModule`, `UploadDestination`, `ZipArtifact` (`@Serializable`), `PendingUpload`, `UploadUiState`, `UploadUiEffect` |

**Plan-milestone naming reconciliation:**
- Commit labels (M3, M4, M5) are the user's sequential session numbers, NOT plan milestone IDs.
- Actual plan milestones completed: M0, M1-A/B/C, M2 (data-camera), M3 (data-ar), M4 (data-firebase), M5-A/B/C (UI features), plus plan-M7 (feature-upload implemented as user-M6).
- Plan milestone **M6 (`feature-scan`)** is **not yet started** — stub only.

---

## 2. Current Architecture State

- **11-module Gradle project**, all declared. Stub: `:features:feature-scan` (build.gradle.kts only).
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
  - `PackageSessionUseCase` is **not** in `feature-upload` — deferred to `feature-scan` (plan-M6) per architecture §12 (feature-upload cannot depend on feature-scan's `ScanSession` type).
  - `feature-upload` receives `ZipArtifact` via `zipArtifactJson` nav arg (URL-encoded JSON), not `ScanSession`.
- **NavHost:** all 6 destinations declared; all bodies are placeholder `Box` composables. No feature screen wired yet (M8).
- **MainActivity:** still uses raw `MaterialTheme {}` instead of `ScanAppTheme {}` — M8 concern.

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
| C-1 | `Routes.FORM = "form"` (no arg) but `FormDestination.route = "form/{selectionId}"` — NavHost out of sync | [Routes.kt](../app/src/main/kotlin/com/sfm/scanner/navigation/Routes.kt) | M8: NavHost uses per-feature `*Destination.route` directly |
| C-2 | `MainActivity` wraps in `MaterialTheme {}` not `ScanAppTheme {}` | [MainActivity.kt](../app/src/main/kotlin/com/sfm/scanner/MainActivity.kt) | M8 |
| C-3 | Lottie splash asset is a placeholder | `features/feature-splash/src/main/res/raw/splash_logo.json` | Replace file at release |
| C-4 | `[TBD-A1]` placeholder options in SelectionScreen | `features/feature-selection/.../SelectionOptions.kt` | One-file replacement when owner provides values |
| C-5 | `[TBD-A2]` placeholder dropdown options in FormScreen | `features/feature-form/.../FormOptions.kt` | One-file replacement |
| C-6 | `FormScreen` back-arrow lambda not yet wired in NavHost | `app/.../AppNavHost.kt` | M8 |
| C-7 | `FormUiEffect.NavigateToScan` emits raw `FormData`; NavHost must URL-encode it | `FormViewModel.kt` | M8 |
| C-8 | ~~`data-ar` is a pure stub~~ | ~~`data/data-ar/`~~ | ✅ Resolved in M3 |
| C-9 | `feature-scan` is a pure stub (build.gradle.kts only) | `features/feature-scan/` | Plan-M6 |
| C-10 | `google-services.json` presence not verified | `:app/` | Verify before assembly |
| C-11 | No `./gradlew assembleDebug` run since M0 — M2 additions unverified by build | n/a | Run before next session |
| C-12 | `PackagingScreen` and `PackageSessionUseCase` deferred to `feature-scan` (plan-M6), not implemented in `feature-upload` | (design gap) | Implemented in plan-M6 per architecture §12 |
| C-13 | `feature-upload.UploadScreen` hard-codes `BackHandler { }` but `PackagingScreen` has no module yet; UploadScreen will be unreachable until NavHost (M8) wires the `upload/{zipArtifactJson}` route from PackagingScreen | `AppNavHost.kt` | M8 + plan-M6 |

---

## 5. Pending Modules (recommended order)

| Plan milestone | Module | Status | Notes |
|----------------|--------|--------|-------|
| M3 | `:data:data-ar` | ✅ Complete | Implemented |
| M6 | `:features:feature-scan` | 🔲 Not started | Depends on M1 ✅, M2 ✅, M3 ✅, M5-B ✅, M5-C ✅. Also owns `PackagingScreen` + `PackageSessionUseCase` |
| M8 | `:app` integration | 🔲 Not started | Depends on all M5 ✅, M6, plan-M7 (feature-upload ✅). Resolves C-1, C-2, C-6, C-7 |
| M9 | Testing & Hardening | 🔲 Not started | Depends on M8 |

**Recommended next module: M6 (`feature-scan`)** — all data-layer dependencies now satisfied (M1 ✅, M2 ✅, M3 ✅, M5-B ✅, M5-C ✅).

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
| M | `FrameRecord.absolutePath` = filename only from camera layer; `ScanViewModel` provides full path after writing file | `FrameRecord.kt` (design deviation D-3) |
| N | `PackageSessionUseCase` + `PackagingScreen` belong in `feature-scan` (plan-M6), not `feature-upload` | (architecture §12 constraint) |
| O | `ZipArtifact` in `feature-upload` is the nav-arg contract between PackagingScreen (scan) and UploadScreen; shape matches data_contracts.md §1.10 | `ZipArtifact.kt` |

---

## 7. Test Status

| Module | Unit tests (`src/test/`) | Instrumented (`src/androidTest/`) |
|--------|--------------------------|------------------------------------|
| `:app` | `RoutesTest` (8) | — |
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

**Implement M6: `:features:feature-scan`** — all dependencies now complete.

Key deliverables:
1. `ScanScreen` composable (camera preview + AR overlay + control bar) per screen_specs.md §5.
2. `ScanViewModel` — coordinates `CameraRepository` + `ArRepository`; SharedCamera wiring via `CameraShared(cameraId)` event; `ScanUiState` machine (Initializing → Ready → Scanning → Stopping → Complete).
3. `StartScanUseCase`, `StopScanUseCase`, `MonitorArMeasurementUseCase` — domain layer per architecture.md §2.1.
4. `PackagingScreen` + `PackageSessionUseCase` — owned here per arch §12 (uses `ScanSession`).
5. `ScanSession`, `FrameRecord` (full path populated here), `ARMeasurement` (imported from data-ar).
6. `ScanDestination` + `PackagingDestination` route objects.
7. `details.json` + `measurements.json` serialization via `kotlinx.serialization`.

---

*End of implementation status.*
