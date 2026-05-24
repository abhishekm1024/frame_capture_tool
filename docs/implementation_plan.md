# Implementation Plan
## Android SfM Scanning App

**Version:** 1.0  
**Status:** Draft — Awaiting Phase 6 Approval  
**Date:** 2026-05-24  
**Depends on:** docs/architecture.md, docs/technical_decisions.md, docs/data_contracts.md, docs/screen_specs.md  
**Derived from:** specs/master_spec.md, specs/constraints.md, specs/architecture_preferences.md

---

## 0. Conventions

| Notation | Meaning |
|----------|---------|
| `[TBD-Ax]` | Unresolved ambiguity; placeholder content required before milestone can be marked complete |
| **Entry criteria** | What must be true before work on this milestone begins |
| **Exit criteria** | What must be true before this milestone is considered done |
| S / M / L | Relative complexity — Small (1–2 days), Medium (3–5 days), Large (1–2 weeks) |

Implementation is **module-by-module, bottom-up** along the dependency graph. No module is implemented until all its dependencies are complete and their exit criteria are satisfied.

---

## 1. Dependency Order

```
M0 Project Setup
    │
    ▼
M1 core-common  ──────────────────────────────────────┐
M1 core-ui       (parallel)                           │
M1 core-storage                                       │
    │                                                 │
    ├──────────────┬─────────────────────┐            │
    ▼              ▼                     ▼            │
M2 data-camera  M3 data-ar          M4 data-firebase  │
    │              │                     │            │
    │              └────────┬────────────┘            │
    │                       │                         │
    │   ┌───────────────────┘                         │
    │   │   M5 feature-splash                         │
    │   │   M5 feature-selection  (parallel, no AR/camera)
    │   │   M5 feature-form                           │
    │   │                                             │
    ▼   ▼                                             │
M6 feature-scan  (integrates data-camera + data-ar)  │
    │                                                 │
    ▼                                                 │
M7 feature-upload  (integrates data-firebase + core-storage)
    │
    ▼
M8 app  (NavHost, integration, end-to-end wiring)
    │
    ▼
M9 Testing & Hardening
```

---

## 2. Pre-Implementation Requirements

Before M0 begins, the following must be resolved:

| Item | Required By |
|------|------------|
| A-1: Initial Selection option labels and keys | M5 feature-selection |
| A-2: Dropdown option labels and keys | M5 feature-form |
| A-3: Confirm min SDK = API 24 | M0 |
| A-4: Confirm AR translation threshold (default 0.30 m) | M3 data-ar |
| A-5: Confirm Firebase Storage path (default `scans/{uid}/{filename}`) | M4 data-firebase |
| A-6: Confirm back navigation policy (default: disabled after Scan) | M8 app |
| A-7: Confirm post-upload navigation (default: reset to Selection) | M7/M8 |
| A-8: Confirm scan stop trigger (default: user Stop button) | M6 feature-scan |
| Firebase project created, `google-services.json` available | M0 |
| Signing keystore configured for debug build | M0 |

---

## 3. Milestones

---

### M0 — Project Setup
**Complexity:** S

**Scope:**
- Multi-module Gradle project scaffolded: all 11 modules declared with `build.gradle.kts` stubs.
- Version catalog (`libs.versions.toml`) configured with all library versions:
  - Kotlin, AGP, Compose BOM, Hilt, CameraX, ARCore, Firebase BoM, Room, WorkManager, kotlinx.serialization, Navigation Compose.
- `minSdk = 24`, `targetSdk = 35`, `compileSdk = 35` applied globally via convention plugin or root `build.gradle.kts`.
- Hilt plugin applied to `:app`.
- `google-services.json` placed in `:app`.
- `ScanApp : Application` skeleton with `@HiltAndroidApp`.
- `MainActivity` skeleton with `@AndroidEntryPoint` + empty `setContent {}`.
- CI build verification: `./gradlew assembleDebug` passes.

**Entry criteria:** Phase 6 approval granted; Firebase project exists; `google-services.json` available.

**Exit criteria:**
- `./gradlew assembleDebug` succeeds with no errors.
- All 11 modules compile (even as stubs).
- Hilt code generation completes without errors.

---

### M1 — Core Modules
**Complexity:** S  
**Modules:** `core-common`, `core-ui`, `core-storage`  
**Can run in parallel across the three modules.**

#### M1-A: `core-common`

**Scope:**
- `AppDispatchers` data class + `@Singleton` Hilt binding (`AppDispatchersModule`).
- `Result<T>` sealed class: `Success(data: T)`, `Failure(cause: Throwable)`, `Loading`.
- String and collection extension utilities as needed.
- Logging wrapper (wraps `android.util.Log`; allows test injection).

**Exit criteria:**
- Unit tests for `Result` transformations pass.
- `AppDispatchers` Hilt binding resolves in a Hilt component test.

#### M1-B: `core-ui`

**Scope:**
- `ScanAppTheme` composable wrapping `MaterialTheme` with color scheme, typography, shapes.
- `PrimaryButton` composable (enabled/disabled states, label, onClick).
- `LabeledTextField` composable (label, value, onValueChange, error, keyboardOptions).
- `LoadingOverlay` composable (spinner + optional label).
- `ErrorBanner` composable (message, optional action button).
- Compose preview for each component.

**Exit criteria:**
- Screenshot tests (or manual preview verification) for all components in both enabled and disabled / error states.
- No hardcoded colors (all via `MaterialTheme.colorScheme`).

#### M1-C: `core-storage`

**Scope:**
- `SessionDirectoryManager`: `createSessionDir(uuid): File`, `deleteSessionDir(uuid)`.
- `ZipBuilder`: `create(destFile: File, block: ZipBuilder.() -> Unit)`, `addFile(entryPath: String, sourceFile: File)`, `addBytes(entryPath: String, bytes: ByteArray)`.
- `AppFileProvider`: resolves `cacheDir/sessions/` and `filesDir/uploads/`.
- `FRAME_FILENAME_FORMAT = "frame_%06d.jpg"` and `ZIP_FILENAME_FORMAT = "%s_%d.zip"` constants.

**Exit criteria:**
- Unit test: `ZipBuilder` produces a valid ZIP containing expected entries.
- Unit test: `SessionDirectoryManager` creates and deletes directories correctly.
- Unit test: `AppFileProvider` returns correct paths for both session and upload dirs.

---

### M2 — `data-camera`
**Complexity:** M  
**Depends on:** M1 complete.

**Scope:**
- `CameraRepositoryImpl` implementing `CameraRepository`.
- `CameraXFrameSource`:
  - Binds `ImageAnalysis` use case to the lifecycle owner.
  - Applies `setTargetFrameRate(Range(5, 5))` hint.
  - Implements timestamp gate: accept frame only if `now - lastAcceptedMs ≥ 200ms`.
  - Closes rejected `ImageProxy` immediately.
  - Backpressure: `STRATEGY_KEEP_ONLY_LATEST`.
- `JpegFrameEncoder`: converts `ImageProxy` (YUV_420_888) → JPEG bytes at quality 95.
- `ResolutionStrategy`: requests 2560×1440; falls back to 1920×1080; falls back to 1280×720.
- `Camera2Interop` focus lock: attempts `CONTROL_AF_MODE_OFF`; logs warning and continues if unsupported.
- `Camera2Interop` exposure lock: attempts `CONTROL_AE_LOCK = true`; falls back to `CONTROL_AE_MODE_OFF` with captured AE values; logs and continues if unsupported.
- `CameraModule` Hilt binding.
- `FrameResult` — `Frame` and `Error` sealed subtypes.

**Exit criteria:**
- Unit test: timestamp gate allows exactly one frame per 200 ms window and drops others.
- Unit test: `ResolutionStrategy` selects correct resolution from a mock list of supported sizes.
- Unit test: `JpegFrameEncoder` produces non-empty JPEG bytes for a mock `ImageProxy`.
- Integration note: full camera pipeline tested end-to-end in M6 on a physical device.

---

### M3 — `data-ar`
**Complexity:** L  
**Depends on:** M1 complete.

**Scope:**
- `ArRepositoryImpl` implementing `ArRepository`.
- `ArSessionManager`:
  - Creates `Session` with `Config.DepthMode.AUTOMATIC` if supported; else `DISABLED`.
  - Exposes `ArSessionEvent` flow (Ready, TrackingChanged, Error, Unsupported).
  - Implements `pauseSession()` / `resumeSession()` / `destroySession()`.
  - Registers `LifecycleEventObserver` on the provided `LifecycleOwner`.
- `ArMeasurementPipeline`:
  - State machine: IDLE → AWAITING_POINT_A → POINT_A_CAPTURED → AWAITING_POINT_B → MEASUREMENT_COMPLETE.
  - Center-pixel raycast via `Frame.hitTest(0.5f, 0.5f)`.
  - Hit type priority: `DepthPoint` → `Plane` → `InstantPlacementPoint` → `FeaturePoint`.
  - Retry budget: 30 consecutive frames before recording `hitType = NONE`.
  - Translation monitor: computes Euclidean distance from `poseA`; triggers Point B at `≥ AR_TRANSLATION_THRESHOLD_METERS`.
  - Emits `ARMeasurement` via `Flow`.
- `DepthAvailabilityChecker`: wraps `session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)`.
- `ArModule` Hilt binding.
- Constants: `AR_TRANSLATION_THRESHOLD_METERS = 0.30f`, `AR_RAYCAST_RETRY_BUDGET = 30`.

**Exit criteria:**
- Unit test: measurement state machine transitions correctly given mocked ARCore frame events.
- Unit test: translation computation is correct (known input vectors produce expected distances).
- Unit test: hit-type priority returns `DEPTH` when a depth hit is present; falls back correctly.
- Unit test: retry budget exhausts at 30 frames and records `hitType = NONE`.
- Integration note: AR pipeline tested end-to-end in M6 on a physical ARCore-capable device.

---

### M4 — `data-firebase`
**Complexity:** S  
**Depends on:** M1 complete; Firebase project and `google-services.json` present.

**Scope:**
- `FirebaseRepositoryImpl` implementing `FirebaseRepository`.
- `AnonymousAuthSource`:
  - `signIn()`: checks for existing `currentUser`; calls `signInAnonymously()` only if null.
  - Returns `Result<String>` (uid on success).
- `FirebaseStorageUploader`:
  - `upload(uid, zipFile)`: uploads to `scans/{uid}/{zipFile.name}` via `putFile()`.
  - Emits `UploadProgress.Uploading(percent)` from `StorageTask` progress listener.
  - Emits `UploadProgress.Success` on completion.
  - Emits `UploadProgress.Failure(cause)` on `StorageException`.
- `FirebaseModule` Hilt binding.
- Constant: `FIREBASE_STORAGE_BASE_PATH = "scans"` `[TBD-A5: confirm]`.

**Exit criteria:**
- Unit test: `AnonymousAuthSource` returns existing user if present; calls `signInAnonymously()` only when no user.
- Unit test: `FirebaseStorageUploader` emits `Success` when `StorageTask` succeeds (mock task).
- Unit test: `FirebaseStorageUploader` emits `Failure` when `StorageTask` throws.
- Integration note: end-to-end upload to real Firebase Storage tested in M7.

---

### M5 — UI Feature Modules (Form-only screens)
**Complexity:** M  
**Modules:** `feature-splash`, `feature-selection`, `feature-form`  
**Depends on:** M1 complete. No camera or AR dependency.  
**Can run in parallel across the three modules.**

#### M5-A: `feature-splash`

**Scope:**
- `SplashScreen` composable: full-screen background, Lottie `LottieAnimation` composable.
- `SplashViewModel`: emits `NavigateToSelection` effect on `onAnimationEnd`.
- `SplashDestination` route object.
- Lottie animation asset placed in `res/raw/`.

**Exit criteria:**
- `SplashViewModel` unit test: `NavigateToSelection` effect emitted after animation-complete signal.
- Manual: animation plays and screen advances on a device.

**Blocker:** Lottie animation asset must be provided.

#### M5-B: `feature-selection`

**Scope:**
- `SelectionScreen` composable per screen_specs.md §3.
- `SelectionViewModel`: holds `selectedOption: SelectionOption?`; emits `NavigateToForm(selectedOption.id)` on Continue tap.
- `SelectionOption` domain model.
- `SelectionDestination` route object.
- Option list hardcoded from resolved `[TBD-A1]` values.

**Exit criteria:**
- `SelectionViewModel` unit test: Continue only emits navigation event when `selectedOption != null`.
- `SelectionViewModel` unit test: selecting an option updates state; selecting another replaces it.
- Compose UI test: Continue button is disabled with no selection; enabled after selection.

**Blocker:** `[TBD-A1]` option content required.

#### M5-C: `feature-form`

**Scope:**
- `FormScreen` composable per screen_specs.md §4.
- `FormViewModel`: holds `FormUiState`; applies all validation rules from functional_spec.md §4.
- `FormValidator`: pure-Kotlin object with static validation functions for each field.
- `FormData` domain model.
- `FormDestination` route object.
- Dropdown options from resolved `[TBD-A2]` values.

**Exit criteria:**
- `FormValidator` unit tests: all valid/invalid combinations for `size`, `detail`, `gt` — minimum 15 test cases covering boundaries.
- `FormViewModel` unit tests: `proceedEnabled` is false until all conditions met; true when all valid.
- `FormViewModel` unit test: `gt` null path serializes correctly.
- Compose UI test: Proceed button state reflects field validity.

**Blocker:** `[TBD-A2]` dropdown content required.

---

### M6 — `feature-scan`
**Complexity:** L  
**Depends on:** M1, M2, M3 complete; M5-B and M5-C complete (nav args from FormScreen).

**Scope:**
- `ScanScreen` composable per screen_specs.md §5 — all 6 `ScanUiState` variants.
- `ScanViewModel`:
  - Injects `CameraRepository`, `ArRepository`, `AppDispatchers`.
  - Implements full scan session state machine from functional_spec.md §5.2.
  - Manages `ScanSession` accumulation (frame list + AR measurement).
  - Torch toggle logic (lock-on-at-scan-start per TD-15).
  - Emits `NavigateToPackaging(scanSession)` on scan complete.
- `StartScanUseCase`, `StopScanUseCase`, `MonitorArMeasurementUseCase`.
- `ScanDestination` route object + `formDataJson` nav argument parsing.
- `BackHandler` interception per screen_specs.md §5.8.
- Permission check gate (camera permission) before camera init.
- ARCore availability check before scan start.
- `ArDisplayState` → AR status chip binding.

**Exit criteria:**
- `ScanViewModel` unit tests (with mocked repositories):
  - State transitions: INITIALIZING → READY → SCANNING → STOPPING → COMPLETE.
  - Frame counter increments on each `FrameResult.Frame` emission.
  - AR state transitions from mocked `ArSessionEvent` and `ARMeasurement` flows.
  - Torch lock: if ON at scan start, hardware state does not change on subsequent toggles.
  - `ScanSession` accumulated correctly at stop (frame count, measurement, UUID).
- Integration test on physical device:
  - Frames captured at ~5 FPS (timestamps verified).
  - AR measurement pipeline captures both points within a 1 m walk.
  - ZIP packaging receives non-empty `frames` list and non-null `measurement`.

---

### M7 — `feature-upload`
**Complexity:** M  
**Depends on:** M1, M4 complete; M6 complete (provides `ScanSession` and packaging).

**Scope:**
- `PackageSessionUseCase`:
  - Accepts `ScanSession`.
  - Calls `ZipBuilder` to create `{UUID}_{timestamp}.zip` in `filesDir/uploads/`.
  - Writes `details.json` and `measurements.json` via `kotlinx.serialization`.
  - Adds frame files from session temp dir.
  - Deletes session temp dir on success.
  - Returns `ZipArtifact`.
- `UploadZipUseCase`:
  - Calls `FirebaseRepository.signInAnonymously()`.
  - Calls `FirebaseRepository.uploadZip()`.
  - On failure: calls `UploadQueueRepository.enqueue()` + enqueues `UploadWorker`.
  - On success: deletes local ZIP file.
- `UploadScreen` composable per screen_specs.md §7 — all 5 `UploadUiState` variants.
- `UploadViewModel`.
- `UploadWorker : CoroutineWorker` (`@HiltWorker`):
  - Reads `PendingUpload` by ID from `UploadQueueRepository`.
  - Re-authenticates and retries upload.
  - On success: `markComplete()`, deletes ZIP, returns `Result.success()`.
  - On failure: `incrementAttempt()`, returns `Result.retry()`.
- `ScanDatabase` (Room), `PendingUploadEntity`, `PendingUploadDao`.
- `UploadQueueModule` Hilt binding.
- `UploadDestination` route object + `zipArtifactJson` nav argument parsing.
- WorkManager configured with `NETWORK_CONNECTED` constraint + `EXPONENTIAL` backoff (initial 30 s).

**Exit criteria:**
- `PackageSessionUseCase` unit test: ZIP produced contains `frames/frame_000001.jpg`, `details.json`, `measurements.json`.
- `PackageSessionUseCase` unit test: `details.json` serializes `gt = null` correctly.
- `PackageSessionUseCase` unit test: `measurements.json` serializes partial measurement (null pointB) correctly.
- `kotlinx.serialization` round-trip tests for `DetailsJson` and `MeasurementsJson`.
- `UploadViewModel` unit test: state transitions for success and failure paths (mocked `FirebaseRepository`).
- `UploadQueueRepository` integration test (Room in-memory): enqueue → getAll → markComplete flow.
- `UploadWorker` unit test: calls `incrementAttempt` on failure; calls `markComplete` on success.

---

### M8 — `app` (Integration)
**Complexity:** M  
**Depends on:** All M5, M6, M7 complete.

**Scope:**
- `AppNavHost` composable: full `NavHost` with all routes and transitions per architecture.md §4.
- `MainActivity`: sets `AppNavHost` as content; `WindowCompat.setDecorFitsSystemWindows(false)`.
- `ScanApp`: registers `HiltWorkerFactory` with `WorkManager`.
- `AndroidManifest.xml`:
  - `CAMERA` permission.
  - `INTERNET` permission.
  - `ACCESS_NETWORK_STATE` permission.
  - `uses-feature android:name="android.hardware.camera"`.
  - ARCore required: `<meta-data android:name="com.google.ar.core" android:value="required"/>`.
  - `android:theme` pointing to splash theme (for system splash if using SplashScreen API).
- Navigation argument wiring: `FormData` JSON from FormScreen → ScanScreen; `ZipArtifact` JSON from PackagingScreen → UploadScreen.
- `BackHandler` verified on ScanScreen.
- Proguard / R8 rules for: `kotlinx.serialization`, Firebase, ARCore, Room, Hilt.

**Exit criteria:**
- `./gradlew assembleRelease` succeeds with R8 enabled.
- End-to-end manual walkthrough on a physical device:
  1. Splash → Selection → Form → Scan (≥ 10 frames, AR measurement complete) → Packaging → Upload → Success screen → "Start New Scan" → Selection.
  2. Offline upload → failure → retry queued → restore network → WorkManager retry → success.
- Back navigation blocked correctly on ScanScreen.

---

### M9 — Testing and Hardening
**Complexity:** M  
**Depends on:** M8 complete.

**Scope:**

#### Unit Tests (augment per-module tests)
- `FormValidator`: full boundary coverage (empty, boundary values, character edge cases).
- `ZipBuilder`: invalid paths, I/O failure handling.
- `ArMeasurementPipeline`: all state transitions; retry budget exhaustion; tracking loss during AWAITING_POINT_B.
- `CameraXFrameSource`: timestamp gate — verify exactly 5 frames accepted per second from a 30 FPS mock stream.
- `PackageSessionUseCase`: zero frames (should error, not produce empty ZIP); I/O failure mid-write.
- `UploadWorker`: network constraint — verify `Result.retry()` on `StorageException`.

#### Instrumentation Tests
- Permission denied → error UI displayed (ScanScreen).
- ARCore unavailable device → `ArSessionEvent.Unsupported` → error UI displayed.
- Full scan session on physical device producing valid ZIP openable by external tool.
- Firebase Storage upload to test project (staging environment).

#### Hardening
- OOM stress test: verify no crash during a 5-minute scan session on a low-memory device (simulate via `ActivityManager` memory pressure).
- ZIP atomicity: kill app mid-packaging; verify no corrupt partial ZIP in `filesDir/uploads/`.
- WorkManager retry: verify `PendingUpload` record survives process death and WorkManager re-enqueues on next boot.
- Lint: `./gradlew lint`; address all errors.
- Detekt (if configured): address all rule violations.

**Exit criteria:**
- All unit tests pass: `./gradlew test`.
- All instrumentation tests pass: `./gradlew connectedAndroidTest`.
- Zero Lint errors.
- No OOM crash in stress test.
- ZIP produced by full session is openable and structurally valid.

---

## 4. Implementation Order Summary

| Milestone | Module(s) | Complexity | Can Parallel With |
|-----------|-----------|------------|------------------|
| M0 | Project setup | S | — |
| M1-A | `core-common` | S | M1-B, M1-C |
| M1-B | `core-ui` | S | M1-A, M1-C |
| M1-C | `core-storage` | S | M1-A, M1-B |
| M2 | `data-camera` | M | M3, M4 |
| M3 | `data-ar` | L | M2, M4 |
| M4 | `data-firebase` | S | M2, M3 |
| M5-A | `feature-splash` | S | M5-B, M5-C |
| M5-B | `feature-selection` | S | M5-A, M5-C |
| M5-C | `feature-form` | M | M5-A, M5-B |
| M6 | `feature-scan` | L | — |
| M7 | `feature-upload` | M | — |
| M8 | `app` | M | — |
| M9 | Testing & hardening | M | — |

---

## 5. Unresolved Blockers

| ID | Blocks | Resolution Required By |
|----|--------|----------------------|
| A-1 | M5-B (feature-selection) | Before M5-B starts |
| A-2 | M5-C (feature-form) | Before M5-C starts |
| Lottie asset | M5-A (feature-splash) | Before M5-A starts |
| `google-services.json` | M0 | Before M0 starts |

---

*End of Implementation Plan*
