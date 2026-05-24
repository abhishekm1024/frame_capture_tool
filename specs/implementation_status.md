# Implementation Status
## Android SfM Scanning App

**Date:** 2026-05-24
**Branch:** `dev`
**Last commit:** `9a215b8 M5 Completed` (note: commit labels number commits sequentially, not by plan milestone ID; see §1)
**Source of truth for plan:** [docs/implementation_plan.md](../docs/implementation_plan.md)

This document is a precise snapshot for session handoff. Verify against the working tree before acting on any item.

---

## 1. Completed Modules

Verified by inspection of source files under `src/main/kotlin/` per module.

| Plan milestone | Module | Status | Key source files |
|----------------|--------|--------|------------------|
| M0 | Project scaffold | ✅ Complete | 11 modules declared in [settings.gradle.kts](../settings.gradle.kts); root + per-module `build.gradle.kts`; version catalog at [gradle/libs.versions.toml](../gradle/libs.versions.toml) |
| M0 | `:app` skeleton | ✅ Complete | [ScanApp.kt](../app/src/main/kotlin/com/sfm/scanner/ScanApp.kt) (`@HiltAndroidApp` + `Configuration.Provider` with `HiltWorkerFactory`); [MainActivity.kt](../app/src/main/kotlin/com/sfm/scanner/MainActivity.kt) (`@AndroidEntryPoint`, `enableEdgeToEdge`, hosts `AppNavHost`); [AppNavHost.kt](../app/src/main/kotlin/com/sfm/scanner/navigation/AppNavHost.kt) (declares all 6 routes — **bodies are placeholder `Box` composables**); [Routes.kt](../app/src/main/kotlin/com/sfm/scanner/navigation/Routes.kt) |
| M1-A | `:core:core-common` | ✅ Complete | `AppDispatchers` + `AppDispatchersModule`; `Result<T>` sealed class; `Logger` + `LoggerModule`; `StringExt`, `CollectionExt` |
| M1-B | `:core:core-ui` | ✅ Complete | `ScanAppTheme` (`Theme.kt`, `Color.kt`, `Typography.kt`, `Shape.kt`); `PrimaryButton`, `LabeledTextField`, `LoadingOverlay`, `ErrorBanner` |
| M1-C | `:core:core-storage` | ✅ Complete | `SessionDirectoryManager`, `ZipBuilder`, `AppFileProvider`, `StorageConstants`, `StorageModule` |
| M4 | `:data:data-firebase` | ✅ Complete | `FirebaseRepository` interface + `FirebaseRepositoryImpl`; `AnonymousAuthSource` (`suspendCancellableCoroutine`); `FirebaseStorageUploader` (`callbackFlow` + `awaitClose { uploadTask.cancel() }`); `UploadProgress` sealed class; `FirebaseConstants` (`FIREBASE_STORAGE_BASE_PATH = "scans"`); `FirebaseModule` |
| M5-A | `:features:feature-splash` | ✅ Complete | `SplashScreen`, `SplashViewModel`, `SplashDestination`, `SplashUiEffect`; Lottie via `rememberLottieAnimatable().animate(composition, iterations = 1)` suspend pattern; `BackHandler { /* no-op */ }`; **placeholder Lottie JSON at `features/feature-splash/src/main/res/raw/splash_logo.json`** |
| M5-B | `:features:feature-selection` | ✅ Complete | `SelectionScreen` (+ internal `SelectionScreenContent`), `SelectionViewModel`, `SelectionDestination`, `SelectionUiState`, `SelectionUiEffect.NavigateToForm(selectionId)`, `SelectionOption`, internal `SelectionOptions` (TBD-A1 placeholders) |
| M5-C | `:features:feature-form` | ✅ Complete | `FormScreen` (+ internal `FormScreenContent` + private `DropdownField`/`SizeField`/`DetailField`/`GtField`), `FormViewModel` (reads `selectionId` from `SavedStateHandle`), `FormDestination` (`route = "form/{selectionId}"`), `FormValidator` (internal, pure Kotlin), `FormUiState`, `FormUiEffect.NavigateToScan(formData)`, `FormData` (`@Serializable`), `FormOption`/`FormOptions` (TBD-A2 placeholders) |

**Important naming reconciliation:** the three visible git commits — `M3 Completed`, `M4 Completed`, `M5 Completed` — number commits sequentially, **not** by plan milestone ID. The actual plan-milestone progress is M0, M1-A/B/C, M4, M5-A/B/C. Plan milestones **M2 (`data-camera`)** and **M3 (`data-ar`)** have **not** started — only `build.gradle.kts` stubs exist for them.

---

## 2. Current Architecture State

- **Multi-module Gradle layout (11 modules, all declared and compiling as stubs at minimum):**
  - `:app`
  - `:core:core-common`, `:core:core-ui`, `:core:core-storage`
  - `:data:data-camera`, `:data:data-ar`, `:data:data-firebase`
  - `:features:feature-splash`, `:features:feature-selection`, `:features:feature-form`, `:features:feature-scan`, `:features:feature-upload`
- **DI:** Hilt applied to every module that needs injection. `ScanApp : Application, Configuration.Provider` wires `HiltWorkerFactory` (ready for M7 `UploadWorker`).
- **Compose:** Jetpack Compose + Material3 (BOM `2024.12.01`). All implemented screens follow the **stateful `XScreen` (public, Hilt) + stateless `XScreenContent` (internal)** pattern, with `hiltViewModel()`, `collectAsStateWithLifecycle()`, and `LaunchedEffect(viewModel) { effects.collect { ... } }`.
- **State/effects pattern (consistent across all ViewModels):**
  - State: `MutableStateFlow<XUiState>` exposed as `StateFlow`, mutated via `update { it.copy(...) }`.
  - Effects: `MutableSharedFlow<XUiEffect>(replay = 0, extraBufferCapacity = 1)` exposed as `SharedFlow`; emitted via `tryEmit(...)`. Consequence: no `viewModelScope.launch` is needed for effect emission, and therefore no `Dispatchers.setMain` is needed in unit tests.
- **Navigation graph:** [AppNavHost.kt](../app/src/main/kotlin/com/sfm/scanner/navigation/AppNavHost.kt) declares all six destinations but **every `composable { ... }` body is `Box(modifier = Modifier.fillMaxSize())`**. No feature screen is wired into the host yet. Deferred to M8 by design.
- **Theme:** `ScanAppTheme` exists in `:core:core-ui` but `MainActivity` currently wraps `AppNavHost` in `MaterialTheme {}` (raw Material3 defaults), with an in-source TODO comment. Switching to `ScanAppTheme {}` is part of M8.
- **Edge-to-edge:** `MainActivity.enableEdgeToEdge()` applied. Each screen handles its own insets (`Modifier.systemBarsPadding()` for splash/selection; `Scaffold { paddingValues -> ... }` for form).

---

## 3. Implemented Dependencies (version catalog additions)

The catalog in [gradle/libs.versions.toml](../gradle/libs.versions.toml) was extended during implementation. Notable additions beyond M0:

| Alias | Purpose | Added during |
|-------|---------|--------------|
| `lottie-compose` (6.5.2) | Splash animation | M5-A |
| `activity-compose` | `BackHandler` on splash; was already in catalog but added to feature-splash deps | M5-A |
| `compose-material-icons-core` (BOM-managed, no version pin) | `Icons.AutoMirrored.Filled.ArrowBack` on Form's `TopAppBar` | M5-C |

Pre-existing aliases (from M0) cover Compose BOM, Hilt, CameraX, ARCore, Firebase BoM, Room, WorkManager, kotlinx-serialization, navigation-compose, coroutines, junit/mockk/turbine. **No version bumps occurred during M1–M5.**

---

## 4. Unresolved Technical Concerns

Repository-grounded — each one verifiable in source.

| ID | Concern | Where | Resolution path |
|----|---------|-------|-----------------|
| C-1 | **`Routes.kt` (app) and per-feature `*Destination` objects are out of sync.** `Routes.FORM = "form"` (no arg), but `FormDestination.route = "form/{selectionId}"`. The NavHost currently uses `Routes.FORM` (no arg). | [Routes.kt:6](../app/src/main/kotlin/com/sfm/scanner/navigation/Routes.kt#L6) vs [FormDestination.kt:5](../features/feature-form/src/main/kotlin/com/sfm/scanner/feature/form/FormDestination.kt#L5) | M8: NavHost should call into per-feature `*Destination.route` directly, or `Routes.kt` should be reconciled to match. |
| C-2 | **`MainActivity` does not wrap content in `ScanAppTheme`.** Uses raw `MaterialTheme {}`. | [MainActivity.kt:20](../app/src/main/kotlin/com/sfm/scanner/MainActivity.kt#L20) | Replace with `ScanAppTheme { AppNavHost() }` in M8. |
| C-3 | **Lottie splash asset is a placeholder.** Minimal hand-authored JSON (blue circle, 90 frames @ 30 fps), not the brand asset. | `features/feature-splash/src/main/res/raw/splash_logo.json` | Replace file at release. Sole code touchpoint is `R.raw.splash_logo` in `SplashScreen.kt`. Blocker per implementation_plan.md §5. |
| C-4 | **`[TBD-A1]` placeholder options in SelectionScreen.** Currently `option_a / option_b / option_c` with placeholder labels. | `features/feature-selection/.../SelectionOptions.kt` | Replace contents of `SelectionOptions.all` and `screenTitle`. No other files affected. |
| C-5 | **`[TBD-A2]` placeholder dropdown options in FormScreen.** Currently `type_a / type_b` with labels `Type A` / `Type B`. | `features/feature-form/.../FormOptions.kt` | Same containment pattern as C-4. |
| C-6 | **`FormScreen` back-arrow lambda not yet wired.** `FormScreen(onNavigateBack, onNavigateToScan, ...)` expects a back callback. AppNavHost passes none. | `features/feature-form/.../FormScreen.kt`, `app/.../AppNavHost.kt` | M8 NavHost wiring (`navController.popBackStack()`). |
| C-7 | **`FormUiEffect.NavigateToScan` carries a raw `FormData` object, not URL-encoded JSON.** Encoding is deferred to NavHost. | [FormViewModel.kt onProceed()](../features/feature-form/src/main/kotlin/com/sfm/scanner/feature/form/FormViewModel.kt) | M8 callback: `URLEncoder.encode(Json.encodeToString(formData), "UTF-8")` per data_contracts.md §5.1. |
| C-8 | **`data-camera` and `data-ar` are pure stubs** (only `build.gradle.kts`; no `src/`). The domain interfaces `CameraRepository`, `ArRepository` defined in data_contracts.md §6.1–6.2 do **not exist anywhere in the repo yet**. | `data/data-camera/`, `data/data-ar/` | M2 and M3 introduce them. |
| C-9 | **`feature-scan` and `feature-upload` are pure stubs** (only `build.gradle.kts`; no `src/`). Their build files already declare module deps and serialization plugin, ready for M6/M7. | `features/feature-scan/`, `features/feature-upload/` | M6 and M7. |
| C-10 | **`google-services.json` presence not verified by this audit.** Required for `:app` to assemble per implementation_plan.md §2. | `:app/` | Confirm exists before next assembly attempt. |
| C-11 | **Last `./gradlew assembleDebug` was at M0.** Build correctness of M5-A/B/C changes (new catalog entry `compose-material-icons-core`, FormScreen experimental opt-in, etc.) has not been re-verified by an actual Gradle run during these sessions. | n/a | Run `./gradlew assembleDebug` before starting M2. |

---

## 5. Pending Modules (recommended order)

Per [docs/implementation_plan.md §1, §4](../docs/implementation_plan.md), dependency order:

1. **M2 — `data-camera`** (Complexity: M). Depends on M1 ✅. **Unblocked.**
2. **M3 — `data-ar`** (Complexity: L). Depends on M1 ✅. **Unblocked.** Can run in parallel with M2.
3. **M6 — `feature-scan`** (Complexity: L). Depends on M1 ✅, M2, M3, M5-B ✅, M5-C ✅. **Blocked on M2 + M3.**
4. **M7 — `feature-upload`** (Complexity: M). Depends on M1 ✅, M4 ✅, M6. **Blocked on M6.**
5. **M8 — `:app` integration** (Complexity: M). Depends on all M5 ✅, M6, M7. **Blocked on M6 + M7.** Resolves C-1, C-2, C-6, C-7.
6. **M9 — Testing & Hardening** (Complexity: M). Depends on M8. **Blocked on M8.**

Recommended next module: **M2 (`data-camera`)** — smaller of the two unblocked options.

---

## 6. Integration Assumptions Already Established

These are contracts present in current code that future modules will rely on. If M6/M7/M8 ever conflict with them, the conflict is here, not in the spec.

| # | Assumption | Established in |
|---|-----------|---------------|
| A | `SplashUiEffect.NavigateToSelection` is `data object`; carries no payload. | [SplashUiEffect.kt](../features/feature-splash/src/main/kotlin/com/sfm/scanner/feature/splash/SplashUiEffect.kt) |
| B | `SelectionUiEffect.NavigateToForm(selectionId: String)` — `selectionId` is `SelectionOption.id`. | [SelectionUiEffect.kt](../features/feature-selection/src/main/kotlin/com/sfm/scanner/feature/selection/SelectionUiEffect.kt) |
| C | `FormDestination.route = "form/{selectionId}"` with `ARG_SELECTION_ID = "selectionId"`. `FormDestination.createRoute(selectionId)` produces concrete paths. | [FormDestination.kt](../features/feature-form/src/main/kotlin/com/sfm/scanner/feature/form/FormDestination.kt) |
| D | `FormViewModel` reads `selectionId` from `SavedStateHandle` via `FormDestination.ARG_SELECTION_ID`. | [FormViewModel.kt:20-22](../features/feature-form/src/main/kotlin/com/sfm/scanner/feature/form/FormViewModel.kt#L20-L22) |
| E | `FormUiEffect.NavigateToScan(formData: FormData)` carries a **raw `FormData` object**, not its JSON. NavHost is responsible for URL-encoding (data_contracts.md §5.1). | [FormUiEffect.kt](../features/feature-form/src/main/kotlin/com/sfm/scanner/feature/form/FormUiEffect.kt) |
| F | `FormData` is `@kotlinx.serialization.Serializable` (ready for JSON nav arg). Field order/types match data_contracts.md §1.2 exactly. | [FormData.kt](../features/feature-form/src/main/kotlin/com/sfm/scanner/feature/form/FormData.kt) |
| G | `FormScreen` public API: `fun FormScreen(onNavigateBack: () -> Unit, onNavigateToScan: (FormData) -> Unit, viewModel: FormViewModel = hiltViewModel())`. | [FormScreen.kt:46-50](../features/feature-form/src/main/kotlin/com/sfm/scanner/feature/form/FormScreen.kt#L46-L50) |
| H | `FirebaseRepository` contract from data_contracts.md §6.3 is **already implemented**; M7 consumes it directly via Hilt. | [FirebaseRepository.kt](../data/data-firebase/src/main/kotlin/com/sfm/scanner/data/firebase/FirebaseRepository.kt) |
| I | App-level `Routes.SCAN = "scan/{formDataJson}"` and `Routes.UPLOAD = "upload/{zipArtifactJson}"` use path-param syntax with `NavType.StringType`. URL-encoded JSON is safe in path segments. | [Routes.kt](../app/src/main/kotlin/com/sfm/scanner/navigation/Routes.kt), [AppNavHost.kt:49-67](../app/src/main/kotlin/com/sfm/scanner/navigation/AppNavHost.kt#L49-L67) |
| J | `ScanApp` exposes `HiltWorkerFactory` via `Configuration.Provider`. M7 `UploadWorker` (`@HiltWorker`) will be picked up automatically without further plumbing. | [ScanApp.kt](../app/src/main/kotlin/com/sfm/scanner/ScanApp.kt) |
| K | `FIREBASE_STORAGE_BASE_PATH = "scans"` (internal const) — full path is `scans/{uid}/{filename}` per data_contracts.md §8. | [FirebaseConstants.kt](../data/data-firebase/src/main/kotlin/com/sfm/scanner/data/firebase/FirebaseConstants.kt) |
| L | Domain interfaces `CameraRepository`/`ArRepository` are **not yet declared** in any module — M2 and M3 will introduce them in their respective `data-*` modules; `:features:feature-scan` already declares `implementation(project(":data:data-camera"))` and `implementation(project(":data:data-ar"))` in its build file, awaiting those modules' source. | (gap — see C-8); [feature-scan/build.gradle.kts:37-38](../features/feature-scan/build.gradle.kts#L37-L38) |

---

## 7. Test Status

Test file counts (not pass/fail — no test run was executed by this audit). Unit tests run on JVM (`testImplementation`); UI tests run on device/emulator (`androidTestImplementation`).

| Module | Unit tests (`src/test/`) | Instrumented (`src/androidTest/`) |
|--------|--------------------------|------------------------------------|
| `:app` | `RoutesTest` (8 tests) | — |
| `:core:core-common` | `ResultTest`, `AppDispatchersModuleTest`, `ext/StringExtTest` | — |
| `:core:core-ui` | — | `PrimaryButtonTest`, `LabeledTextFieldTest`, `LoadingOverlayTest`, `ErrorBannerTest` |
| `:core:core-storage` | `ZipBuilderTest`, `SessionDirectoryManagerTest`, `AppFileProviderTest` | — |
| `:data:data-firebase` | `AnonymousAuthSourceTest`, `FirebaseStorageUploaderTest`, `FirebaseRepositoryImplTest` (11 tests total) | — |
| `:features:feature-splash` | `SplashViewModelTest` (1 test) | — |
| `:features:feature-selection` | `SelectionViewModelTest` (4 tests) | `SelectionScreenTest` (2 tests) |
| `:features:feature-form` | `FormValidatorTest` (28 tests), `FormViewModelTest` (13 tests) | `FormScreenTest` (5 tests) |
| `:data:data-camera`, `:data:data-ar` | — (stub modules; no `src/`) | — |
| `:features:feature-scan`, `:features:feature-upload` | — (stub modules; no `src/`) | — |

**Not yet executed in any visible session:**
- `./gradlew test` (unit suite) end-to-end.
- `./gradlew connectedAndroidTest` (instrumentation) — requires emulator/device.
- `./gradlew assembleDebug` since M0 — should be re-run before starting M2 to surface any latent compile issues from M5-A/B/C.
- `./gradlew assembleRelease` (with R8) — deferred to M8 exit criteria.

---

## 8. Known Risks

From [docs/risk_register.md](../docs/risk_register.md), filtered to the items relevant to upcoming work. Numbering matches the register.

| Risk | Relevance to upcoming milestones |
|------|----------------------------------|
| **R-01** ARCore Depth API not available on device | M3 must implement plane/feature-point fallback (`DepthAvailabilityChecker`). |
| **R-02** CameraX cannot hit 5 FPS target | M2 timestamp gate is the mitigation; physical verification deferred to M6. |
| **R-03** OOM during long scan | M2 enforces `MAX_FRAMES_IN_FLIGHT = 3`; M6 uses session-scoped temp dir + IO-dispatcher writes. |
| **R-04** Fixed focus / AE lock unsupported on device | M2 `Camera2Interop` must log warning and continue (not crash). |
| **R-11** Nav argument size limit exceeded | `FormData` is tiny (5 fields). Higher risk for `ZipArtifact` if `absolutePath` is long; monitor in M8. |
| **R-12** A-1 / A-2 unresolved at M5 | **Currently true** — placeholders in `SelectionOptions.kt` and `FormOptions.kt`. Tracked as C-4/C-5; release-blocking, not implementation-blocking. |
| **R-13** ARCore session lifecycle leak | M3 must register `LifecycleEventObserver`; M6 must invoke `destroySession()`. |
| **R-16** App killed mid-packaging | M7 atomic ZIP write (close-then-rename); cleanup on cold start. |

Lower-severity risks (R-05–R-10, R-14, R-15, R-17) are device/runtime concerns deferred to M6–M9.

---

## 9. Exact Next Implementation Step

**Implement M2: `:data:data-camera`** per [docs/implementation_plan.md §3 (M2)](../docs/implementation_plan.md).

Concrete deliverables for the next session:

1. Domain types in `:data:data-camera/src/main/kotlin/com/sfm/scanner/data/camera/`:
   - `CameraRepository` interface, `CameraConfig` data class, `FrameResult` sealed class — exact signatures from [docs/data_contracts.md §6.1](../docs/data_contracts.md). Note `FrameResult.Frame` carries a `FrameRecord` — `FrameRecord` is defined in data_contracts.md §1.3 and should be placed in this module (it is consumed downstream by `:features:feature-scan`).
2. Implementations:
   - `CameraXFrameSource` — binds `ImageAnalysis` use case; applies `setTargetFrameRate(Range(5, 5))`; timestamp gate `now - lastAcceptedMs ≥ 200 ms`; `STRATEGY_KEEP_ONLY_LATEST` backpressure; closes rejected `ImageProxy` immediately.
   - `JpegFrameEncoder` — YUV_420_888 → JPEG bytes at quality 95.
   - `ResolutionStrategy` — 2560×1440 → 1920×1080 → 1280×720 fallback chain.
   - `Camera2Interop` focus lock (`CONTROL_AF_MODE_OFF`) + exposure lock (`CONTROL_AE_LOCK = true`, fallback `CONTROL_AE_MODE_OFF`); both must log-and-continue on unsupported, not crash (mitigates R-04).
   - `CameraRepositoryImpl`.
   - `CameraModule` Hilt binding.
3. Constants per [data_contracts.md §8](../docs/data_contracts.md): `TARGET_FPS = 5`, `FRAME_INTERVAL_MS = 200L`, `PREFERRED_WIDTH = 2560`, `PREFERRED_HEIGHT = 1440`, `FALLBACK_WIDTH = 1920`, `FALLBACK_HEIGHT = 1080`, `JPEG_QUALITY = 95`, `MAX_FRAMES_IN_FLIGHT = 3`.
4. Unit tests (per implementation_plan.md M2 exit criteria):
   - Timestamp gate — exactly one frame per 200 ms window from a mock high-rate source.
   - `ResolutionStrategy` selection across mocked supported-size lists (preferred / fallback-1 / fallback-2).
   - `JpegFrameEncoder` produces non-empty JPEG bytes for a mock `ImageProxy`.
5. **Out of scope for M2** (per plan): physical-device end-to-end capture — deferred to M6's integration test.

**Pre-flight check:** before starting M2, run `./gradlew assembleDebug` to confirm the M5-A/B/C additions (new `compose-material-icons-core` catalog entry, FormScreen `@file:OptIn(ExperimentalMaterial3Api::class)`, full FormViewModel/Validator/Screen/tests) build cleanly. See C-11.

---

*End of implementation status.*
