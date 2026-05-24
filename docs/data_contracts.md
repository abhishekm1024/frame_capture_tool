# Data Contracts
## Android SfM Scanning App

**Version:** 1.0  
**Status:** Draft — Awaiting Phase 4 Approval  
**Date:** 2026-05-24  
**Depends on:** docs/functional_spec.md, docs/architecture.md, docs/technical_decisions.md  
**Derived from:** specs/master_spec.md, specs/constraints.md

---

## 0. Conventions

- **Domain models** are pure Kotlin; no Android or library imports.
- **`?`** denotes nullable (Kotlin semantics).
- **`[TBD-Ax]`** denotes an unresolved ambiguity carried from PRD Section 9.
- Types shown are Kotlin types. JSON type equivalents are noted where they differ.
- This document defines contracts only. No implementation code is generated here.

---

## 1. Domain Models

### 1.1 `SelectionOption`
**Module:** `feature-selection / domain`

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | `String` | Non-empty | Stored as `initialSelection` in `details.json` |
| `displayLabel` | `String` | Non-empty | Shown in UI |

`[TBD-A1]` Concrete instances not yet defined. Count and values pending owner input.

---

### 1.2 `FormData`
**Module:** `feature-form / domain`  
**Lifecycle:** Created on FormScreen submit; carried through Scan → Packaging → written to `details.json`.

| Field | Type | Constraints |
|-------|------|-------------|
| `initialSelection` | `String` | Non-empty; value from `SelectionOption.id` |
| `dropdownSelection` | `String` | Non-empty; one of two valid option keys `[TBD-A2]` |
| `size` | `Int` | > 0 |
| `detail` | `String` | Non-empty; length ≤ 16; matches `[A-Za-z0-9]+` |
| `gt` | `List<Double>?` | Null if not provided; all elements finite (non-NaN, non-Inf) if provided |

---

### 1.3 `FrameRecord`
**Module:** `feature-scan / domain`  
**Lifecycle:** Created per accepted frame during scan; held in-memory until ZIP packaging; discarded after.

| Field | Type | Constraints |
|-------|------|-------------|
| `index` | `Int` | ≥ 1; sequential, no gaps |
| `filename` | `String` | Format: `frame_NNNNNN.jpg` (6-digit zero-padded) |
| `absolutePath` | `String` | Absolute path within session temp directory |
| `timestampMs` | `Long` | Unix epoch milliseconds; monotonically increasing |

---

### 1.4 `CameraPose`
**Module:** `feature-scan / domain`  
**Purpose:** Camera world-space pose at a given moment; stored in `measurements.json`.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `tx` | `Float` | Finite | Translation X (metres) |
| `ty` | `Float` | Finite | Translation Y (metres) |
| `tz` | `Float` | Finite | Translation Z (metres) |
| `qx` | `Float` | Finite | Rotation quaternion X |
| `qy` | `Float` | Finite | Rotation quaternion Y |
| `qz` | `Float` | Finite | Rotation quaternion Z |
| `qw` | `Float` | Finite | Rotation quaternion W |

Quaternion `(qx, qy, qz, qw)` represents rotation in ARCore world space. Norm should be ≈ 1.0; minor floating-point deviation acceptable.

---

### 1.5 `ArPoint`
**Module:** `feature-scan / domain`  
**Purpose:** A single world-space 3D measurement point captured by ARCore.

| Field | Type | Constraints |
|-------|------|-------------|
| `x` | `Float` | Finite (metres) |
| `y` | `Float` | Finite (metres) |
| `z` | `Float` | Finite (metres) |
| `timestampMs` | `Long` | Unix epoch milliseconds |

---

### 1.6 `HitType`
**Module:** `feature-scan / domain`  
**Kind:** Enum

| Value | Meaning |
|-------|---------|
| `DEPTH` | ARCore depth-based hit (most accurate) |
| `PLANE` | ARCore plane detection hit |
| `INSTANT_PLACEMENT` | ARCore instant placement hit |
| `FEATURE_POINT` | ARCore raw point cloud hit (least accurate) |
| `NONE` | No hit obtained after retry budget exhausted |

---

### 1.7 `ArTrackingState`
**Module:** `feature-scan / domain`  
**Kind:** Enum  
**Maps to:** `TrackingState` in ARCore SDK (string representation stored in JSON)

| Value | Meaning |
|-------|---------|
| `TRACKING` | Full ARCore tracking |
| `PAUSED` | Temporarily lost |
| `STOPPED` | Session stopped |

---

### 1.8 `ARMeasurement`
**Module:** `feature-scan / domain`  
**Lifecycle:** Built during scan; null until Point A is captured; `pointB` and `distanceMeters` null until Point B is captured.

| Field | Type | Constraints |
|-------|------|-------------|
| `pointA` | `ArPoint?` | Null until captured |
| `pointB` | `ArPoint?` | Null until translation threshold crossed |
| `distanceMeters` | `Float?` | Null until both points captured; ≥ 0.0 |
| `cameraPoseA` | `CameraPose?` | Null until Point A captured |
| `cameraPoseB` | `CameraPose?` | Null until Point B captured |
| `trackingState` | `ArTrackingState` | State at time of last measurement event |
| `hitType` | `HitType` | Hit type of the most recent successful raycast |

---

### 1.9 `ScanSession`
**Module:** `feature-scan / domain`  
**Lifecycle:** Created at scan start; completed at scan stop; handed to PackagingUseCase.

| Field | Type | Constraints |
|-------|------|-------------|
| `sessionUUID` | `String` | UUID v4 string (e.g. `"550e8400-e29b-41d4-a716-446655440000"`) |
| `formData` | `FormData` | Non-null; from FormScreen |
| `frames` | `List<FrameRecord>` | Non-empty after any successful capture |
| `measurement` | `ARMeasurement` | Non-null; may have null sub-fields if AR was incomplete |
| `scanStartTimestampMs` | `Long` | Unix epoch ms; timestamp of first accepted frame |

---

### 1.10 `ZipArtifact`
**Module:** `feature-upload / domain`  
**Lifecycle:** Created by `PackageSessionUseCase`; passed to `UploadZipUseCase`.

| Field | Type | Constraints |
|-------|------|-------------|
| `sessionUUID` | `String` | Matches `ScanSession.sessionUUID` |
| `zipFilename` | `String` | Format: `{UUID}_{unixTimestampSeconds}.zip` |
| `absolutePath` | `String` | Absolute path in `context.filesDir/uploads/` |
| `fileSizeBytes` | `Long` | > 0 |

---

### 1.11 `PendingUpload`
**Module:** `feature-upload / domain`  
**Lifecycle:** Created when upload fails; deleted by `UploadWorker` on success.

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | `String` | UUID v4; primary key |
| `sessionUUID` | `String` | Matches originating `ScanSession.sessionUUID` |
| `zipFilename` | `String` | Filename only (not full path) |
| `absolutePath` | `String` | Full path to ZIP on disk |
| `enqueuedAtMs` | `Long` | Unix epoch milliseconds |
| `attemptCount` | `Int` | ≥ 0; incremented by `UploadWorker` per attempt |

---

### 1.12 `UploadProgress`
**Module:** `feature-upload / domain`  
**Kind:** Sealed class; emitted as `Flow<UploadProgress>` by `FirebaseRepository`

| Subtype | Fields | Meaning |
|---------|--------|---------|
| `Uploading` | `percent: Int` (0–100) | Transfer in progress |
| `Success` | — | Upload complete |
| `Failure` | `cause: Throwable` | Terminal failure |

---

## 2. JSON Output Schemas

### 2.1 `details.json`

**Serializer:** `kotlinx.serialization`  
**Location in ZIP:** root level (`details.json`)

#### Full Schema

```json
{
  "initialSelection": "string",
  "dropdownSelection": "string",
  "size": 123,
  "detail": "ABC123",
  "gt": [1.2, 3.4, 5.6],
  "scanTimestamp": "2024-04-05T14:22:58Z",
  "deviceInfo": "Google Pixel 8 (Android 14)",
  "appVersion": "1.0.0"
}
```

#### Field Definitions

| JSON Key | JSON Type | Nullable | Source | Notes |
|----------|-----------|----------|--------|-------|
| `initialSelection` | `string` | No | `FormData.initialSelection` | Canonical option key `[TBD-A1]` |
| `dropdownSelection` | `string` | No | `FormData.dropdownSelection` | Canonical option key `[TBD-A2]` |
| `size` | `number` (integer) | No | `FormData.size` | Positive integer |
| `detail` | `string` | No | `FormData.detail` | Alphanumeric, ≤ 16 chars |
| `gt` | `array` of `number` | Yes (JSON `null`) | `FormData.gt` | Float array or `null`; never `[]` (empty list treated as `null`) |
| `scanTimestamp` | `string` | No | `ScanSession.scanStartTimestampMs` | ISO-8601 UTC (`yyyy-MM-dd'T'HH:mm:ss'Z'`) |
| `deviceInfo` | `string` | No | `Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")"` | |
| `appVersion` | `string` | No | `BuildConfig.VERSION_NAME` | Semantic version string |

#### Serialization Notes
- `gt: null` is serialized as JSON `null`, not the string `"null"` and not an absent key.
- `gt` with a single value is still serialized as a JSON array: `[1.5]`.
- `scanTimestamp` is formatted in UTC, not local time.

---

### 2.2 `measurements.json`

**Serializer:** `kotlinx.serialization`  
**Location in ZIP:** root level (`measurements.json`)

#### Full Schema — Measurement Complete

```json
{
  "pointA": {
    "x": 0.12,
    "y": -0.05,
    "z": 1.23,
    "timestamp": 1712345600123
  },
  "pointB": {
    "x": 0.45,
    "y": -0.05,
    "z": 1.23,
    "timestamp": 1712345607456
  },
  "distanceMeters": 0.33,
  "cameraPoseA": {
    "tx": 0.0,  "ty": 0.0,  "tz": 0.0,
    "qx": 0.0,  "qy": 0.0,  "qz": 0.0,  "qw": 1.0
  },
  "cameraPoseB": {
    "tx": 0.33, "ty": 0.0,  "tz": 0.0,
    "qx": 0.0,  "qy": 0.0,  "qz": 0.0,  "qw": 1.0
  },
  "trackingState": "TRACKING",
  "hitType": "DEPTH"
}
```

#### Full Schema — Point B Not Reached (scan stopped early)

```json
{
  "pointA": {
    "x": 0.12,
    "y": -0.05,
    "z": 1.23,
    "timestamp": 1712345600123
  },
  "pointB": null,
  "distanceMeters": null,
  "cameraPoseA": {
    "tx": 0.0, "ty": 0.0, "tz": 0.0,
    "qx": 0.0, "qy": 0.0, "qz": 0.0, "qw": 1.0
  },
  "cameraPoseB": null,
  "trackingState": "TRACKING",
  "hitType": "DEPTH"
}
```

#### Full Schema — Point A Not Reached (AR never tracked)

```json
{
  "pointA": null,
  "pointB": null,
  "distanceMeters": null,
  "cameraPoseA": null,
  "cameraPoseB": null,
  "trackingState": "PAUSED",
  "hitType": "NONE"
}
```

#### Field Definitions

| JSON Key | JSON Type | Nullable | Source |
|----------|-----------|----------|--------|
| `pointA` | `object` | Yes | `ARMeasurement.pointA` |
| `pointA.x` | `number` | — | `ArPoint.x` (metres) |
| `pointA.y` | `number` | — | `ArPoint.y` (metres) |
| `pointA.z` | `number` | — | `ArPoint.z` (metres) |
| `pointA.timestamp` | `number` (integer) | — | `ArPoint.timestampMs` (Unix epoch ms) |
| `pointB` | `object` | Yes | `ARMeasurement.pointB` |
| `pointB.*` | same as `pointA.*` | — | |
| `distanceMeters` | `number` | Yes | `ARMeasurement.distanceMeters` (Euclidean distance in metres) |
| `cameraPoseA` | `object` | Yes | `ARMeasurement.cameraPoseA` |
| `cameraPoseA.tx/ty/tz` | `number` | — | Translation in metres |
| `cameraPoseA.qx/qy/qz/qw` | `number` | — | Rotation quaternion |
| `cameraPoseB` | `object` | Yes | `ARMeasurement.cameraPoseB` |
| `cameraPoseB.*` | same as `cameraPoseA.*` | — | |
| `trackingState` | `string` | No | `ARMeasurement.trackingState.name` — one of `"TRACKING"`, `"PAUSED"`, `"STOPPED"` |
| `hitType` | `string` | No | `ARMeasurement.hitType.name` — one of `"DEPTH"`, `"PLANE"`, `"INSTANT_PLACEMENT"`, `"FEATURE_POINT"`, `"NONE"` |

#### Serialization Notes
- Nullable fields (`pointB`, `distanceMeters`, `cameraPoseB`) must appear in the JSON as `null`, not be omitted.
- Float precision in JSON: use default `kotlinx.serialization` float formatting (sufficient for 6 significant digits).

---

## 3. ZIP Archive Contract

### 3.1 Structure

```
{UUID}_{unixTimestampSeconds}.zip
├── frames/
│   ├── frame_000001.jpg
│   ├── frame_000002.jpg
│   └── ... frame_NNNNNN.jpg
├── details.json
└── measurements.json
```

### 3.2 Filename

| Component | Format | Example |
|-----------|--------|---------|
| `UUID` | UUID v4, lowercase, hyphens | `550e8400-e29b-41d4-a716-446655440000` |
| `_` | Literal underscore | |
| `unixTimestampSeconds` | Unix epoch seconds (`Long`) | `1712345678` |
| Extension | `.zip` | |

Full example: `550e8400-e29b-41d4-a716-446655440000_1712345678.zip`

The `UUID` matches `ScanSession.sessionUUID`. The timestamp is derived from `ScanSession.scanStartTimestampMs / 1000`.

### 3.3 Frame File Rules

| Rule | Constraint |
|------|-----------|
| Naming | `frame_NNNNNN.jpg` — 6-digit zero-padded counter |
| Counter | Starts at `000001`; no gaps |
| JPEG quality | 95 |
| ZIP entry path | `frames/frame_NNNNNN.jpg` (forward slash; not OS path separator) |
| Frame order | Files added to ZIP in ascending frame index order |

### 3.4 Invariants

- `details.json` and `measurements.json` are always present, even if AR tracking was incomplete.
- `frames/` always contains ≥ 1 file (scan produces at least one frame before stop).
- ZIP is written atomically: file is renamed into place only after `ZipOutputStream.close()` succeeds.

---

## 4. Room Schema

### 4.1 Database

| Property | Value |
|----------|-------|
| Name | `scan_database` |
| Class | `ScanDatabase` |
| Version | `1` |
| Module | `feature-upload` |
| Export schema | `true` (schema JSON committed to repo) |

### 4.2 Entity: `PendingUploadEntity`

**Table name:** `pending_uploads`

| Column | Type | Constraints | Maps to |
|--------|------|-------------|---------|
| `id` | `TEXT` | PRIMARY KEY, NOT NULL | `PendingUpload.id` |
| `session_uuid` | `TEXT` | NOT NULL | `PendingUpload.sessionUUID` |
| `zip_filename` | `TEXT` | NOT NULL | `PendingUpload.zipFilename` |
| `absolute_path` | `TEXT` | NOT NULL | `PendingUpload.absolutePath` |
| `enqueued_at_ms` | `INTEGER` | NOT NULL | `PendingUpload.enqueuedAtMs` |
| `attempt_count` | `INTEGER` | NOT NULL, DEFAULT 0 | `PendingUpload.attemptCount` |

### 4.3 DAO: `PendingUploadDao`

| Method | Query | Notes |
|--------|-------|-------|
| `insert(entity)` | `INSERT OR REPLACE INTO pending_uploads` | `suspend` |
| `deleteById(id)` | `DELETE FROM pending_uploads WHERE id = :id` | `suspend` |
| `getAll()` | `SELECT * FROM pending_uploads ORDER BY enqueued_at_ms ASC` | Returns `Flow<List<PendingUploadEntity>>` |
| `getById(id)` | `SELECT * FROM pending_uploads WHERE id = :id LIMIT 1` | `suspend`, returns nullable |
| `incrementAttempt(id)` | `UPDATE pending_uploads SET attempt_count = attempt_count + 1 WHERE id = :id` | `suspend` |

---

## 5. Navigation Argument Contracts

### 5.1 `FormScreen` → `ScanScreen`

**Mechanism:** URL-encoded JSON string nav argument  
**Argument name:** `formDataJson`  
**Type:** `NavType.StringType`  
**Encoding:** `FormData` serialized via `kotlinx.serialization`, then `URLEncoder.encode(json, "UTF-8")`  
**Decoding:** `URLDecoder.decode(arg, "UTF-8")`, then deserialized via `kotlinx.serialization`

`FormData` serialized shape:
```json
{
  "initialSelection": "...",
  "dropdownSelection": "...",
  "size": 123,
  "detail": "ABC123",
  "gt": [1.2, 3.4]
}
```

### 5.2 `PackagingScreen` → `UploadScreen`

**Mechanism:** URL-encoded JSON string nav argument  
**Argument name:** `zipArtifactJson`  
**Encoding/decoding:** Same pattern as above

`ZipArtifact` serialized shape:
```json
{
  "sessionUUID": "...",
  "zipFilename": "..._....zip",
  "absolutePath": "/data/user/0/.../files/uploads/...",
  "fileSizeBytes": 12345678
}
```

### 5.3 `SavedStateHandle` Recovery

Both `ScanViewModel` and `UploadViewModel` read their respective nav arguments from `SavedStateHandle` so that process-death restoration works correctly without re-navigating.

---

## 6. Repository Interface Contracts

Full Kotlin signatures. These are contracts — implementations live in `data-*` modules.

### 6.1 `CameraRepository`
**Module:** `feature-scan / domain`

```
interface CameraRepository {
    fun startCapture(config: CameraConfig): Flow<FrameResult>
    suspend fun stopCapture()
}

data class CameraConfig(
    val targetFps: Int,                  // 5
    val preferredWidth: Int,             // 2560
    val preferredHeight: Int,            // 1440
    val jpegQuality: Int,                // 95
    val lockFocus: Boolean,              // true
    val lockExposure: Boolean            // true
)

sealed class FrameResult {
    data class Frame(val record: FrameRecord, val jpegBytes: ByteArray) : FrameResult()
    data class Error(val cause: Throwable) : FrameResult()
}
```

### 6.2 `ArRepository`
**Module:** `feature-scan / domain`

```
interface ArRepository {
    fun startSession(): Flow<ArSessionEvent>
    fun getMeasurementFlow(): Flow<ARMeasurement>
    suspend fun pauseSession()
    suspend fun resumeSession()
    suspend fun destroySession()
}

sealed class ArSessionEvent {
    object Ready : ArSessionEvent()
    data class TrackingChanged(val state: ArTrackingState) : ArSessionEvent()
    data class Error(val cause: Throwable) : ArSessionEvent()
    object Unsupported : ArSessionEvent()
}
```

### 6.3 `FirebaseRepository`
**Module:** `feature-upload / domain`

```
interface FirebaseRepository {
    suspend fun signInAnonymously(): Result<String>   // String = Firebase UID
    fun uploadZip(uid: String, zipFile: File): Flow<UploadProgress>
}
```

### 6.4 `UploadQueueRepository`
**Module:** `feature-upload / domain`

```
interface UploadQueueRepository {
    suspend fun enqueue(upload: PendingUpload)
    suspend fun markComplete(id: String)
    suspend fun incrementAttempt(id: String)
    fun getAll(): Flow<List<PendingUpload>>
    suspend fun getById(id: String): PendingUpload?
}
```

---

## 7. UI State Contracts

These sealed classes define the complete observable state surface for each ViewModel. No intermediate mutable states are exposed.

### 7.1 `ScanUiState`

```
sealed class ScanUiState {
    object Initializing : ScanUiState()
    object Ready : ScanUiState()
    data class Scanning(
        val frameCount: Int,
        val torchOn: Boolean,
        val arDisplayState: ArDisplayState
    ) : ScanUiState()
    object Stopping : ScanUiState()
    object Complete : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

enum class ArDisplayState {
    INITIALIZING,
    TRACKING,
    POINT_A_CAPTURED,
    MEASUREMENT_COMPLETE,
    TRACKING_LOST,
    UNSUPPORTED
}
```

### 7.2 `UploadUiState`

```
sealed class UploadUiState {
    object Authenticating : UploadUiState()
    data class Uploading(val percent: Int, val filename: String) : UploadUiState()
    object Success : UploadUiState()
    data class Failed(val filename: String) : UploadUiState()
    object RetryQueued : UploadUiState()
}
```

### 7.3 `FormUiState`

```
data class FormUiState(
    val dropdownSelection: String? = null,
    val sizeInput: String = "",
    val sizeError: String? = null,
    val detailInput: String = "",
    val detailError: String? = null,
    val gtInput: String = "",
    val gtError: String? = null,
    val proceedEnabled: Boolean = false
)
```

---

## 8. Constants

| Constant | Value | Module | Notes |
|----------|-------|--------|-------|
| `TARGET_FPS` | `5` | `data-camera` | |
| `FRAME_INTERVAL_MS` | `200L` | `data-camera` | `1000 / TARGET_FPS` |
| `PREFERRED_WIDTH` | `2560` | `data-camera` | |
| `PREFERRED_HEIGHT` | `1440` | `data-camera` | |
| `FALLBACK_WIDTH` | `1920` | `data-camera` | |
| `FALLBACK_HEIGHT` | `1080` | `data-camera` | |
| `JPEG_QUALITY` | `95` | `data-camera` | |
| `MAX_FRAMES_IN_FLIGHT` | `3` | `data-camera` | OOM guard |
| `AR_TRANSLATION_THRESHOLD_METERS` | `0.30f` | `data-ar` | `[TBD-A4]` |
| `AR_RAYCAST_RETRY_BUDGET` | `30` | `data-ar` | Frames before giving up |
| `FIREBASE_STORAGE_BASE_PATH` | `"scans"` | `data-firebase` | Full path: `scans/{uid}/{filename}` `[TBD-A5]` |
| `WORKMANAGER_INITIAL_BACKOFF_SECS` | `30L` | `feature-upload` | |
| `DETAIL_MAX_LENGTH` | `16` | `feature-form` | |
| `FRAME_FILENAME_FORMAT` | `"frame_%06d.jpg"` | `core-storage` | |
| `ZIP_FILENAME_FORMAT` | `"%s_%d.zip"` | `core-storage` | `(UUID, unixSecs)` |

---

## 9. Carried-Forward Ambiguities

| ID | Impact on Data Contracts |
|----|--------------------------|
| A-1 | `SelectionOption` instances undefined; `initialSelection` string values unknown |
| A-2 | `dropdownSelection` valid string values unknown; validation rule in `FormValidator` incomplete |
| A-4 | `AR_TRANSLATION_THRESHOLD_METERS = 0.30f` is a default; pending confirmation |
| A-5 | `FIREBASE_STORAGE_BASE_PATH = "scans"` is a default; pending confirmation |

A-3, A-6, A-7, A-8 have no impact on data contracts.

---

*End of Phase 4 — Data Contracts*  
*Awaiting approval to proceed to Phase 5: docs/screen_specs.md*
