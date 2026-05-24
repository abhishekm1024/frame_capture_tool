# Architecture Specification
## Android SfM Scanning App

**Version:** 1.0  
**Status:** Draft — Awaiting Phase 3 Approval  
**Date:** 2026-05-24  
**Depends on:** docs/PRD.md, docs/functional_spec.md  
**Derived from:** specs/architecture_preferences.md, specs/constraints.md, specs/master_spec.md

---

## 1. Architectural Style

The app follows **MVVM + Clean Architecture** within a **multi-module Android Gradle project**.

Three layers are defined across all modules:

| Layer | Responsibility | Android Components |
|-------|---------------|-------------------|
| **Presentation** | UI rendering, user events, UI state management | `@Composable`, `ViewModel` |
| **Domain** | Business rules, use cases, domain models, repository interfaces | Plain Kotlin classes, `interface` |
| **Data** | Repository implementations, data sources, external integrations | `RepositoryImpl`, Room DAOs, Firebase SDKs, CameraX, ARCore |

Dependency direction is strictly **inward**: Presentation → Domain ← Data. The domain layer has zero Android dependencies.

---

## 2. Module Map

The following 11 modules match specs/architecture_preferences.md exactly. No refinements have been made.

```
┌─────────────────────────────────────────────────────────────────────┐
│  app                                                                │
│  (Application, MainActivity, NavHost, Hilt root)                   │
└──────────┬──────────────────────────────────────────────────────────┘
           │ depends on
    ┌──────▼───────────────────────────────────────────────────────┐
    │  feature-splash │ feature-selection │ feature-form           │
    │  feature-scan   │ feature-upload                             │
    └──────┬───────────────────────────────────────────────────────┘
           │ depends on
    ┌──────▼──────────┐   ┌─────────────┐   ┌──────────────────┐
    │  data-camera    │   │  data-ar    │   │  data-firebase   │
    └──────┬──────────┘   └──────┬──────┘   └────────┬─────────┘
           │                     │                    │
    ┌──────▼─────────────────────▼────────────────────▼──────────┐
    │  core-common    │  core-storage    │  core-ui               │
    └────────────────────────────────────────────────────────────┘
```

### 2.1 Module Responsibilities

#### `app`
- `ScanApp : Application` — Hilt entry point (`@HiltAndroidApp`)
- `MainActivity` — single activity host (`@AndroidEntryPoint`)
- `AppNavHost` — root `NavHost` with all navigation routes
- Gradle dependency aggregation (depends on all feature + core modules)
- `AndroidManifest.xml` — permissions, `uses-feature` for ARCore/camera

#### `core-ui`
- Compose `MaterialTheme` setup (colors, typography, shapes)
- Shared Composable primitives: `PrimaryButton`, `LabeledTextField`, `LoadingOverlay`, `ErrorBanner`
- Shared Compose preview utilities
- No ViewModels; no business logic

#### `core-common`
- `AppDispatchers` — injectable coroutine dispatcher bindings (`IO`, `Default`, `Main`)
- `Result<T>` sealed class (Success / Failure / Loading)
- Kotlin extension utilities (string, collection)
- Logging abstraction wrapper
- No Android framework dependencies beyond `Context` where unavoidable

#### `core-storage`
- `SessionDirectoryManager` — creates/deletes `cacheDir/sessions/{UUID}/frames/`
- `ZipBuilder` — wraps `java.util.zip.ZipOutputStream`; adds files by path
- `AppFileProvider` — resolves app-specific file storage paths
- No Room definitions here (Room lives in the module that owns the schema)

#### `feature-splash`
- `SplashScreen` Composable
- `SplashViewModel` (minimal — triggers navigation on animation complete)
- Domain: none

#### `feature-selection`
- `SelectionScreen` Composable
- `SelectionViewModel` — holds selected option state
- Domain model: `SelectionOption(id: String, displayLabel: String)`
- Route: `SelectionDestination`

#### `feature-form`
- `FormScreen` Composable
- `FormViewModel` — holds all field states, drives validation, emits `FormData`
- Domain: `FormValidator` (pure Kotlin, no Android deps) — validates all field rules
- Domain model: `FormData`
- Route: `FormDestination`

#### `feature-scan`
- `ScanScreen` Composable
- `ScanViewModel` — orchestrates camera + AR pipelines; holds `ScanSessionState`
- Domain use cases:
  - `StartScanUseCase`
  - `StopScanUseCase`
  - `MonitorArMeasurementUseCase`
- Domain models: `ScanSession`, `FrameRecord`, `ARMeasurement`
- Repository interfaces consumed: `CameraRepository`, `ArRepository`
- Route: `ScanDestination`

#### `feature-upload`
- `UploadScreen` Composable
- `UploadViewModel` — drives auth + upload + retry state
- `UploadWorker : CoroutineWorker` — background retry via WorkManager
- Domain use cases:
  - `PackageSessionUseCase` — creates ZIP from `ScanSession`
  - `UploadZipUseCase`
- Domain models: `ZipArtifact`, `PendingUpload`
- Repository interfaces consumed: `FirebaseRepository`, `UploadQueueRepository`
- Room entity + DAO for `PendingUpload` lives in this module
- Route: `UploadDestination`

#### `data-camera`
- `CameraRepositoryImpl : CameraRepository`
- `CameraXFrameSource` — configures `ImageAnalysis` use case, applies FPS gate
- `JpegFrameEncoder` — encodes `ImageProxy` → JPEG bytes at quality 95
- `ResolutionStrategy` — negotiates 2560×1440 with fallback chain
- Hilt module: `CameraModule`

#### `data-ar`
- `ArRepositoryImpl : ArRepository`
- `ArSessionManager` — ARCore `Session` lifecycle (create, resume, pause, destroy)
- `ArMeasurementPipeline` — center-pixel raycast, translation monitor, point capture
- `DepthAvailabilityChecker` — checks `Config.DepthMode` support
- Hilt module: `ArModule`

#### `data-firebase`
- `FirebaseRepositoryImpl : FirebaseRepository`
- `AnonymousAuthSource` — wraps `FirebaseAuth.signInAnonymously()`
- `FirebaseStorageUploader` — wraps `StorageReference.putFile()`; exposes progress Flow
- Hilt module: `FirebaseModule`
- Firebase Storage path constant: `scans/{uid}/{filename}` `[TBD-A5: confirm]`

---

## 3. Layer Boundaries Per Feature Module

Each feature module that owns non-trivial business logic is internally structured as:

```
feature-scan/
├── presentation/
│   ├── ScanScreen.kt          (Composable)
│   ├── ScanViewModel.kt       (@HiltViewModel)
│   └── ScanUiState.kt         (sealed class / data class)
├── domain/
│   ├── model/
│   │   ├── ScanSession.kt
│   │   ├── FrameRecord.kt
│   │   └── ARMeasurement.kt
│   ├── repository/
│   │   ├── CameraRepository.kt   (interface)
│   │   └── ArRepository.kt       (interface)
│   └── usecase/
│       ├── StartScanUseCase.kt
│       ├── StopScanUseCase.kt
│       └── MonitorArMeasurementUseCase.kt
└── (no data layer — implemented in data-camera / data-ar)
```

simpler feature modules (splash, selection) collapse presentation + domain into fewer files.

---

## 4. Navigation Architecture

- **Pattern:** Single-activity, Compose Navigation (`androidx.navigation:navigation-compose`)
- **NavHost:** defined in `app` module, `AppNavHost.kt`
- **Routes:** each feature module exposes a `object XDestination { const val route = "..." }` in its public API
- **Argument passing:** typed nav arguments via `navArgument` for `FormData` (serialized to JSON string for nav); `ScanSession` passed via `SavedStateHandle` in ViewModel (not nav args — too large)

### 4.1 Route Definitions

| Screen | Route Pattern |
|--------|--------------|
| Splash | `splash` |
| Selection | `selection` |
| Form | `form` |
| Scan | `scan` |
| Packaging (transient) | `packaging` |
| Upload | `upload/{zipPath}` |

### 4.2 Navigation Rules

- SplashScreen → SelectionScreen: `popUpTo("splash") { inclusive = true }` (splash removed from back stack)
- FormScreen → ScanScreen: back stack cleared to `selection` start; back from ScanScreen not allowed (popUpTo inclusive or system back intercepted)
- After upload result: `popUpTo("selection") { inclusive = false }` to reset to SelectionScreen

---

## 5. Dependency Injection (Hilt)

- `@HiltAndroidApp` on `ScanApp`
- `@AndroidEntryPoint` on `MainActivity`
- `@HiltViewModel` on all ViewModels
- `UploadWorker` uses `@HiltWorker` + `HiltWorkerFactory`

### 5.1 Hilt Modules

| Module | Lives in | Provides |
|--------|----------|---------|
| `AppDispatchersModule` | `core-common` | `AppDispatchers` (coroutine dispatchers) |
| `StorageModule` | `core-storage` | `SessionDirectoryManager`, `ZipBuilder`, `AppFileProvider` |
| `CameraModule` | `data-camera` | `CameraRepository` bound to `CameraRepositoryImpl` |
| `ArModule` | `data-ar` | `ArRepository` bound to `ArRepositoryImpl`; `ArSessionManager` |
| `FirebaseModule` | `data-firebase` | `FirebaseRepository` bound to `FirebaseRepositoryImpl`; `FirebaseApp` |
| `UploadQueueModule` | `feature-upload` | `UploadQueueRepository`, Room `PendingUploadDatabase` |

All bindings scoped to `@Singleton` unless noted otherwise in technical decisions.

---

## 6. Threading Model

| Operation | Dispatcher |
|-----------|-----------|
| UI rendering | `Main` |
| ViewModel state updates | `Main` (via `StateFlow`) |
| Frame capture callback | Camera executor (CameraX-managed) |
| Frame JPEG encoding + disk write | `IO` |
| ARCore frame processing | `Default` (CPU-bound math) |
| ZIP construction | `IO` |
| Firebase upload | `IO` (Firebase SDK internal) |
| WorkManager `UploadWorker` | `IO` (via `CoroutineWorker`) |
| Room queries | `IO` |

All `ViewModel` launched coroutines use `viewModelScope`. All repository operations return `Flow` or `suspend fun`.

---

## 7. State Management

- UI state is modelled as a **single sealed `UiState`** per screen, held in `StateFlow<UiState>` in the ViewModel.
- ViewModels expose `StateFlow` only (never `MutableStateFlow`) to the presentation layer.
- Side effects (navigation, one-shot events) are emitted via `SharedFlow<UiEffect>` with `replay = 0`.

### 7.1 Example: ScanViewModel State

```
sealed class ScanUiState {
    object Initializing : ScanUiState()
    object Ready : ScanUiState()
    data class Scanning(
        val frameCount: Int,
        val arState: ArDisplayState,
        val torchOn: Boolean
    ) : ScanUiState()
    object Stopping : ScanUiState()
    object Complete : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}
```

---

## 8. Key Interface Contracts

### 8.1 CameraRepository

```kotlin
interface CameraRepository {
    fun startCapture(config: CameraConfig): Flow<FrameResult>
    suspend fun stopCapture()
    fun getAvailableResolutions(): List<Size>
}
```

### 8.2 ArRepository

```kotlin
interface ArRepository {
    fun startSession(context: Context): Flow<ArSessionEvent>
    suspend fun pauseSession()
    suspend fun destroySession()
    fun getMeasurementFlow(): Flow<ArMeasurementState>
}
```

### 8.3 FirebaseRepository

```kotlin
interface FirebaseRepository {
    suspend fun signInAnonymously(): Result<String>       // returns uid
    fun uploadZip(uid: String, zipFile: File): Flow<UploadProgress>
}
```

### 8.4 UploadQueueRepository

```kotlin
interface UploadQueueRepository {
    suspend fun enqueue(upload: PendingUpload)
    suspend fun markComplete(id: String)
    fun getPending(): Flow<List<PendingUpload>>
}
```

---

## 9. Persistence Architecture

### 9.1 Room Database

- One database: `ScanDatabase` in `feature-upload`.
- One table: `pending_uploads` (entity: `PendingUploadEntity`).
- Schema migrations: manual (no `fallbackToDestructiveMigration` in production).

### 9.2 App-Specific File Storage

| Content | Location |
|---------|----------|
| Session temp frames | `context.cacheDir/sessions/{uuid}/frames/` |
| Completed ZIPs (before upload) | `context.filesDir/uploads/{filename}` |
| Completed ZIPs (retry queue) | Same as above; cleaned by `UploadWorker` on success |

---

## 10. Firebase Storage Path

Upload path: `scans/{firebaseUid}/{zipFilename}` `[TBD-A5: confirm or override]`

Example: `scans/abc123uid/550e8400-e29b-41d4-a716-446655440000_1712345678.zip`

---

## 11. Min SDK

**API 24** (Android 7.0 Nougat) `[TBD-A3: confirm]`

Rationale: ARCore minimum is API 24. CameraX minimum is API 21. Effective floor is API 24.

Target SDK: **API 35** (latest stable at time of writing).

---

## 12. Module Dependency Rules (Enforced)

```
Allowed:
  app           → feature-*, core-*
  feature-scan  → data-camera, data-ar, core-common, core-ui, core-storage
  feature-upload→ data-firebase, core-common, core-ui, core-storage
  feature-*     → core-common, core-ui
  data-*        → core-common, core-storage
  core-ui       → core-common
  core-storage  → core-common

Forbidden:
  data-* → feature-*          (no upward dependency)
  core-* → feature-*          (no upward dependency)
  core-* → data-*             (cores are library-neutral)
  feature-X → feature-Y       (no cross-feature dependencies)
  domain layer → Android SDK  (domain is pure Kotlin)
```

---

*End of Architecture Specification*
