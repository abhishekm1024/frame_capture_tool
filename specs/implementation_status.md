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
| M4 | `:data:data-firebase` | ✅ Complete | `FirebaseRepository` + `FirebaseRepositoryImpl`; `AnonymousAuthSource`; `FirebaseStorageUploader` (`callbackFlow`); `UploadProgress`; `FirebaseConstants`; `FirebaseModule` |
| M5-A | `:features:feature-splash` | ✅ Complete | `SplashScreen`, `SplashViewModel`, `SplashDestination`, `SplashUiEffect`; Lottie suspend pattern; `BackHandler { /* no-op */ }`; **placeholder Lottie JSON** |
| M5-B | `:features:feature-selection` | ✅ Complete | `SelectionScreen` (+ `SelectionScreenContent`), `SelectionViewModel`, `SelectionDestination`, `SelectionUiState`, `SelectionUiEffect.NavigateToForm(selectionId)`, `SelectionOption`, `SelectionOptions` (TBD-A1 placeholders) |
| M5-C | `:features:feature-form` | ✅ Complete | `FormScreen` (+ `FormScreenContent`), `FormViewModel` (`SavedStateHandle`), `FormDestination` (`route = "form/{selectionId}"`), `FormValidator`, `FormUiState`, `FormUiEffect.NavigateToScan(formData)`, `FormData` (`@Serializable`), `FormOptions` (TBD-A2 placeholders) |
| M6 (user #) | `:features:feature-upload` | ✅ Complete | `UploadScreen` (+ `UploadScreenContent`), `UploadViewModel` (`@Inject internal constructor`), `UploadZipUseCase`, `UploadWorkEnqueuer`, `UploadWorker` (`@HiltWorker`), `ScanDatabase` (Room v1), `PendingUploadEntity`/`Dao`, `UploadQueueRepositoryImpl`, `UploadQueueModule`, `UploadDestination`, `ZipArtifact` (`@Serializable`), `PendingUpload`, `UploadUiState`, `UploadUiEffect` |

**Plan-milestone naming reconciliation:**
- Commit labels (M3, M4, M5) are the user's sequential session numbers, NOT plan milestone IDs.
- Actual plan milestones completed: M0, M1-A/B/C, M2 (data-camera), M4 (data-firebase), M5-A/B/C (UI features), plus plan-M7 (feature-upload implemented as user-M6).
- Plan milestones **M3 (`data-ar`)** and **M6 (`feature-scan`)** are **not yet started** — stubs only.

---

## 2. Current Architecture State

- **11-module Gradle project**, all declared. Stubs: `:data:data-ar`, `:features:feature-scan`.
- **DI:** Hilt on every module. `ScanApp` wires `HiltWorkerFactory` for `UploadWorker`.
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
| C-8 | `data-ar` is a pure stub (build.gradle.kts only) | `data/data-ar/` | Plan-M3 |
| C-9 | `feature-scan` is a pure stub (build.gradle.kts only) | `features/feature-scan/` | Plan-M6 |
| C-10 | `google-services.json` presence not verified | `:app/` | Verify before assembly |
| C-11 | No `./gradlew assembleDebug` run since M0 — M2 additions unverified by build | n/a | Run before next session |
| C-12 | `PackagingScreen` and `PackageSessionUseCase` deferred to `feature-scan` (plan-M6), not implemented in `feature-upload` | (design gap) | Implemented in plan-M6 per architecture §12 |
| C-13 | `feature-upload.UploadScreen` hard-codes `BackHandler { }` but `PackagingScreen` has no module yet; UploadScreen will be unreachable until NavHost (M8) wires the `upload/{zipArtifactJson}` route from PackagingScreen | `AppNavHost.kt` | M8 + plan-M6 |

---

## 5. Pending Modules (recommended order)

| Plan milestone | Module | Status | Notes |
|----------------|--------|--------|-------|
| M3 | `:data:data-ar` | 🔲 Not started | Depends on M1 ✅. ARCore Session, measurement pipeline, depth fallback chain |
| M6 | `:features:feature-scan` | 🔲 Not started | Depends on M1 ✅, M2 ✅, M3, M5-B ✅, M5-C ✅. Also owns `PackagingScreen` + `PackageSessionUseCase` |
| M8 | `:app` integration | 🔲 Not started | Depends on all M5 ✅, M6, plan-M7 (feature-upload ✅). Resolves C-1, C-2, C-6, C-7 |
| M9 | Testing & Hardening | 🔲 Not started | Depends on M8 |

**Recommended next module: M3 (`data-ar`)** — last pure data-layer module before feature-scan can be built.

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
| `:data:data-ar` | — (stub) | — |
| `:features:feature-scan`, `:features:feature-upload` test totals | above | above |

**Not yet executed:** `./gradlew test`, `./gradlew connectedAndroidTest`, `./gradlew assembleDebug` (since M0).

---

## 8. Known Risks (active items)

| Risk | Status |
|------|--------|
| R-01 ARCore Depth API not available | Unmitigated until M3 (data-ar) fallback chain implemented |
| R-02 CameraX 5 FPS not guaranteed by HAL | **Mitigated** — `TimestampGate(200ms)` + `setTargetFrameRate(5,5)` hint |
| R-03 OOM during long scan | **Mitigated** — `STRATEGY_KEEP_ONLY_LATEST` + `MAX_FRAMES_IN_FLIGHT = 3` + ImageProxy closed before IO coroutine |
| R-04 Focus/AE lock unsupported | **Mitigated** — Camera2Interop in `try/catch`; logs warning; scan continues with default AF/AE |
| R-12 A-1/A-2 unresolved | **Ongoing** — placeholders in SelectionOptions/FormOptions; release-blocking |
| R-13 ARCore session lifecycle leak | Unmitigated until M3 (ArSessionManager lifecycle observer) |
| R-15 Firebase Storage rules misconfigured | Requires manual Firebase project deployment before M6 integration testing |
| R-16 App killed mid-packaging | Mitigated by ZipBuilder atomic rename; PackagingScreen logic deferred to M6 |

---

## 9. Exact Next Implementation Step

**Implement M3: `:data:data-ar`** per [docs/implementation_plan.md §3 (M3)](../docs/implementation_plan.md).

Deliverables:
1. Domain types: `ArRepository` interface, `ArSessionEvent` sealed class, `CameraRepository`-parallel contracts (all in `data-ar`) — same module-placement logic as data-camera.
2. `ArSessionManager` — ARCore `Session` lifecycle via `DefaultLifecycleObserver`; `pause`/`resume`/`destroy`.
3. `ArMeasurementPipeline` — state machine (IDLE → AWAITING_A → POINT_A → AWAITING_B → COMPLETE); center-pixel raycast; translation monitor; retry budget (30 frames); `hitType` fallback chain per TD-07.
4. `DepthAvailabilityChecker`.
5. `ArModule` Hilt binding.
6. Constants: `AR_TRANSLATION_THRESHOLD_METERS = 0.30f`, `AR_RAYCAST_RETRY_BUDGET = 30`.
7. Unit tests: state-machine transitions, translation computation, hit-type priority, retry budget exhaustion.

---

*End of implementation status.*
