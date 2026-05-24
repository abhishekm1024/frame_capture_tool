# Functional Specification
## Android SfM Scanning App

**Version:** 1.0  
**Status:** Draft — Awaiting Phase 2 Approval  
**Date:** 2026-05-24  
**Depends on:** docs/PRD.md v1.0  
**Derived from:** specs/master_spec.md, specs/constraints.md, specs/architecture_preferences.md

---

## 0. Conventions Used in This Document

| Notation | Meaning |
|----------|---------|
| `[TBD-Ax]` | Unresolved ambiguity from PRD Section 9; ID matches PRD entry |
| **SHALL** | Non-negotiable functional requirement |
| **SHOULD** | Strong preference; deviation requires explicit justification |
| `→` | State transition or navigation |

---

## 1. Application Lifecycle Overview

The app defines a single, linear user journey with no branching paths. Navigation is forward-only during normal operation.

```
App Launch
    │
    ▼
SplashScreen
    │ animation complete (auto)
    ▼
SelectionScreen
    │ valid selection + user tap
    ▼
FormScreen
    │ all fields valid + user tap "Proceed to Scan"
    ▼
ScanScreen
    │ user taps Stop [TBD-A8]
    ▼
PackagingScreen (transient — progress indicator only)
    │ ZIP ready
    ▼
UploadScreen
    │ success or failure acknowledged
    ▼
[TBD-A7: result screen / reset to SelectionScreen]
```

### 1.1 Back Navigation Policy

`[TBD-A6 — default applied]` Back navigation is **disabled** once the user transitions from FormScreen to ScanScreen. On ScanScreen and beyond, the system back gesture/button shall have no effect or show a confirmation dialog before abandoning the scan. Back navigation between SplashScreen → SelectionScreen → FormScreen is allowed (no scan state exists yet).

---

## 2. Screen: SplashScreen

### 2.1 Entry Condition
App process start or cold launch.

### 2.2 Behavior

| Step | Description |
|------|-------------|
| 1 | Display animated app logo centered on screen. |
| 2 | Animation runs to completion. Duration determined by animation asset. |
| 3 | On animation complete, automatically navigate → SelectionScreen. |

### 2.3 Constraints
- No user interaction accepted (tap, swipe, back) shall trigger early navigation.
- Screen background and logo treatment deferred to screen specs (Phase 5).

### 2.4 Error States
None. Splash has no failure modes.

---

## 3. Screen: SelectionScreen

### 3.1 Entry Condition
Automatic navigation from SplashScreen.

### 3.2 State Model

```
UNSELECTED (initial)
    │ user taps an option
    ▼
SELECTED (exactly one item highlighted)
    │ user taps "Continue" / "Next"  [button label TBD Phase 5]
    ▼
→ FormScreen (carries selectedOption value)
```

### 3.3 Option Definitions

`[TBD-A1]` The exact option labels and their corresponding `initialSelection` string values are not yet defined. The functional behavior is:

- The screen SHALL display **N ≥ 2** options `[TBD-A1: count and labels]`.
- Options are mutually exclusive; selecting one deselects any previously selected option.
- The stored value for `initialSelection` in `details.json` is the canonical string key for the chosen option `[TBD-A1: key mapping]`.

### 3.4 Validation
- Proceed action is **disabled** while state = UNSELECTED.
- Proceed action is **enabled** when exactly one option is selected.

### 3.5 Output
`selectedOption: String` — carried to FormScreen and ultimately written to `details.json`.

---

## 4. Screen: FormScreen

### 4.1 Entry Condition
Navigation from SelectionScreen with `selectedOption` in nav arguments.

### 4.2 Fields

#### 4.2.1 Dropdown — `dropdownSelection`

| Property | Value |
|----------|-------|
| Type | Single-select dropdown |
| Options | Exactly 2 `[TBD-A2: labels and stored values]` |
| Default | No selection (empty / placeholder shown) |
| Required | Yes |
| Stored value | String key of chosen option `[TBD-A2]` |

#### 4.2.2 Text Field — `size`

| Property | Rule |
|----------|------|
| Type | Numeric integer input |
| Required | Yes |
| Valid condition | Non-empty AND parses as `Int` AND value > 0 |
| Invalid condition | Empty, non-integer characters, zero, negative |
| Keyboard type | Number |
| Error message | "Must be a positive integer" (exact copy TBD Phase 5) |

#### 4.2.3 Text Field — `detail`

| Property | Rule |
|----------|------|
| Type | Text input |
| Required | Yes |
| Valid condition | Non-empty AND length ≤ 16 AND all characters match `[A-Za-z0-9]` |
| Invalid condition | Empty, contains non-alphanumeric characters, length > 16 |
| Keyboard type | ASCII-capable |
| Error message | "Alphanumeric only, max 16 characters" (exact copy TBD Phase 5) |

#### 4.2.4 Text Field — `gt`

| Property | Rule |
|----------|------|
| Type | Text input |
| Required | No (optional) |
| Valid condition (provided) | All comma-separated tokens parse as `Double` (IEEE 754); surrounding whitespace trimmed per token |
| Valid condition (empty) | Field empty or contains only whitespace → stored as `null` |
| Invalid condition | Non-empty AND any token fails `Double` parse |
| Keyboard type | Decimal / ASCII |
| Example valid input | `12.3, 15.0, 0.42` |
| Error message | "Enter comma-separated numbers (e.g. 1.0, 2.5)" (exact copy TBD Phase 5) |
| Stored value (provided) | `List<Double>` — parsed values in input order |
| Stored value (absent) | `null` |

### 4.3 Proceed Button State Machine

```
DISABLED (initial)
    │ all conditions below become true simultaneously
    ▼
ENABLED

Conditions:
  C1: dropdownSelection != null
  C2: size field valid (non-empty, positive integer)
  C3: detail field valid (non-empty, ≤16, alphanumeric)
  C4: gt field valid (either empty OR all tokens parse as Double)
```

- Button text: `"Proceed to Scan"` (exact string from spec).
- Validation SHALL be evaluated live (on every keystroke / selection change), not only on submit.

### 4.4 Output
On tap of enabled Proceed button → navigate to ScanScreen carrying:
```
FormData {
    selectedOption: String       // from SelectionScreen
    dropdownSelection: String    // [TBD-A2]
    size: Int
    detail: String
    gt: List<Double>?            // null if not provided
}
```

### 4.5 Error States
- All validation errors shown inline below the relevant field.
- No network or system errors on this screen.

---

## 5. Screen: ScanScreen

### 5.1 Entry Condition
Navigation from FormScreen with `FormData` payload.

### 5.2 Scan Session State Machine

```
INITIALIZING
    │ Camera ready AND ARCore session started
    ▼
READY
    │ user taps "Start Scan" [TBD-A8 default: explicit Start button]
    ▼
SCANNING
    │   ├─ frames captured at 5 FPS
    │   ├─ ARCore pose monitored continuously
    │   ├─ AR measurement pipeline running (§5.5)
    │   └─ user taps "Stop" [TBD-A8]
    ▼
STOPPING
    │ camera pipeline flushed, AR session suspended
    ▼
COMPLETE → navigate to PackagingScreen
```

Error states embedded within SCANNING:
- `TRACKING_LOST` — ARCore tracking degraded (sub-state, scan continues)
- `CAMERA_ERROR` — unrecoverable camera failure → user notified, session aborted

### 5.3 Camera Pipeline

#### 5.3.1 Configuration

| Parameter | Value |
|-----------|-------|
| API | CameraX ImageAnalysis + ImageCapture |
| Target resolution | 2560×1440 |
| Fallback resolution | Highest supported resolution ≥ 1920×1080; if unavailable, highest supported ≥ 1280×720 |
| Frame rate | 5 FPS (target); implemented via software throttling in ImageAnalysis |
| Format | JPEG |
| Focus mode | Fixed focus locked at scan start (if device supports `CONTROL_AF_MODE_OFF`); otherwise continuous autofocus |
| Exposure mode | Fixed exposure locked at scan start (if device supports manual exposure); otherwise auto exposure |
| Flash / torch | Controlled independently (§5.4) |

#### 5.3.2 FPS Throttling Strategy

- CameraX ImageAnalysis delivers frames at camera maximum rate.
- A timestamp gate SHALL be applied: a frame is accepted only if `currentTimestamp - lastAcceptedTimestamp ≥ 200 ms` (= 5 FPS).
- Frames that arrive before the gate opens are **dropped** (not queued).
- The timestamp of each accepted frame SHALL be recorded in milliseconds since Unix epoch.

#### 5.3.3 Frame Storage

- Each accepted frame is written to a session-scoped temporary directory under `context.cacheDir` as:
  ```
  frames/frame_NNNNNN.jpg
  ```
  where `NNNNNN` is a zero-padded 6-digit sequential counter starting at `000001`.
- A companion metadata list is maintained in memory:
  ```
  FrameRecord { index: Int, filename: String, timestampMs: Long }
  ```
- JPEG encoding quality: 95 (photogrammetry-friendly; not the Android default of 85).

#### 5.3.4 Memory Management

- At most **N frames** are held in the pending-write queue at any time; N SHALL be determined during implementation based on per-frame memory cost but SHALL NOT exceed 3 frames in-flight.
- If the write queue is full, the incoming frame is **dropped** (not the queued frames).
- Frame write operations run on an IO dispatcher (not the camera callback thread).

#### 5.3.5 Session Temp Directory Lifecycle

| Event | Action |
|-------|--------|
| Scan session start | Create `cacheDir/sessions/{UUID}/frames/` |
| Scan complete | Directory handed to packaging module |
| ZIP packaging complete | Session temp directory deleted |
| App killed mid-scan | Temp directory left on disk; cleaned on next cold start if no pending upload matches |

### 5.4 Flash / Torch Control

| State | Behavior |
|-------|----------|
| OFF (default) | Torch disabled |
| ON | Torch set to `FLASH_MODE_TORCH` at MAX intensity |
| Toggle | User can toggle before scan start and during scan |
| Constraint | If torch is ON when scan starts, it SHALL remain ON for the entire scan duration regardless of subsequent toggle state changes — `[clarification: the spec says "maintain constant torch if enabled during scan"; interpreted as lock-on-at-start-time; will carry to technical decisions]` |

> **Note:** Torch-during-scan locking behavior is an interpretation of "maintain constant torch if enabled during scan." If the intent is that the user can freely toggle at any time during a scan, this SHALL be resolved before Phase 3.

### 5.5 ARCore Measurement Pipeline

#### 5.5.1 AR Session Initialization

- ARCore session SHALL be initialized at ScanScreen entry.
- Depth API availability SHALL be queried at session start:
  - If `Config.DepthMode.AUTOMATIC` is supported → use depth-based raycasting.
  - Otherwise → fall back to plane/feature-point raycasting (§5.5.5).
- If ARCore is not supported on the device → display error and block scan start.

#### 5.5.2 AR Measurement State Machine

```
AR_IDLE
    │ scan starts (user taps Start)
    ▼
AWAITING_POINT_A
    │ ARCore tracking state = TRACKING
    │ AND depth/plane hit available at screen center
    ▼
POINT_A_CAPTURED  { pointA: Vector3, poseA: CameraPose, timestampA: Long, hitType }
    │ continuously monitor camera translation from poseA
    ▼
AWAITING_POINT_B  (translation < threshold)
    │ translation ≥ TRANSLATION_THRESHOLD_METERS
    ▼
POINT_B_CAPTURED  { pointB: Vector3, poseB: CameraPose, timestampB: Long, hitType }
    │ compute distance = |pointB - pointA|
    ▼
MEASUREMENT_COMPLETE  { pointA, pointB, distanceMeters, poseA, poseB, trackingState, hitType }
```

#### 5.5.3 Translation Threshold

`[TBD-A4 — default proposed]` Default: **0.30 m** (30 cm). This is a compile-time constant `AR_TRANSLATION_THRESHOLD_METERS`. Awaiting owner confirmation or override.

Translation is computed as the Euclidean distance between the camera origin at Point A capture and the current camera origin:
```
translation = sqrt(
    (cx - ax)² + (cy - ay)² + (cz - az)²
)
```
where `(ax, ay, az)` is the camera origin when Point A was captured and `(cx, cy, cz)` is the current camera origin.

#### 5.5.4 Center Pixel Raycast

- Raycast origin: center of the camera preview surface (screen center in normalized coordinates 0.5, 0.5).
- Preferred: `Frame.hitTest()` using `DepthPoint` or `Plane` anchors.
- Required fields extracted per hit:
  - World-space position `(x, y, z)` in meters
  - Hit type (`DEPTH`, `PLANE`, `FEATURE_POINT`)
  - Tracking state at capture moment
  - Camera pose (translation + rotation quaternion)

#### 5.5.5 Depth Unavailability Fallback

| Scenario | Behavior |
|----------|---------|
| Depth API not supported | Use plane/feature-point hit test; record `hitType = PLANE` or `FEATURE_POINT` |
| No hit at center pixel | Retry on next ARCore frame (up to 30 consecutive frames); if still no hit, record `hitType = NONE` and log; AWAITING_POINT_A remains active |
| Tracking state = PAUSED / STOPPED | Suspend measurement; resume on TRACKING; UI shows "Tracking lost" warning |

#### 5.5.6 Measurement Data Contract (preview — full contract in Phase 4)

```json
{
  "pointA": { "x": 0.0, "y": 0.0, "z": 0.0, "timestamp": 0 },
  "pointB": { "x": 1.0, "y": 0.0, "z": 0.0, "timestamp": 0 },
  "distanceMeters": 1.0,
  "cameraPoseA": { "tx": 0.0, "ty": 0.0, "tz": 0.0, "qx": 0.0, "qy": 0.0, "qz": 0.0, "qw": 1.0 },
  "cameraPoseB": { "tx": 0.0, "ty": 0.0, "tz": 0.0, "qx": 0.0, "qy": 0.0, "qz": 0.0, "qw": 1.0 },
  "trackingState": "TRACKING",
  "hitType": "DEPTH"
}
```

If Point B was never captured (e.g., scan ended before threshold reached): `pointB`, `cameraPoseB`, and `distanceMeters` are `null`.

### 5.6 UX Guidance on ScanScreen

| Element | Behavior |
|---------|---------|
| Instruction overlay | On READY state: "Point camera at the center of the object" instruction shown before scan starts |
| Frame counter | During SCANNING: "Frames: NNN" updated in real time |
| AR status indicator | Shows: Initializing / Tracking / Point A captured / Measurement complete / Tracking lost |
| Torch toggle button | Visible at all times; shows current state (OFF / ON) |
| Stop button | Visible during SCANNING state |
| Scan progress | Frame counter serves as primary progress indicator; no fixed end count `[TBD-A8]` |

### 5.7 Scan Termination

`[TBD-A8 — default applied]` Scan ends when the user taps the **Stop** button. No fixed frame count or time limit unless specified by owner.

On stop:
1. Frame capture pipeline halted (no new frames accepted).
2. All pending frame writes flushed to disk.
3. ARCore session suspended (not destroyed — lifecycle managed per §5.5.1).
4. Navigate → PackagingScreen with session context.

---

## 6. Screen: PackagingScreen (Transient)

### 6.1 Behavior

This is a non-interactive progress screen. The user cannot interact with it.

| Step | Operation |
|------|-----------|
| 1 | Generate session UUID (random UUID v4). |
| 2 | Compute Unix timestamp in seconds. |
| 3 | Create ZIP file: `{UUID}_{timestamp}.zip` in app-specific external files dir or internal files dir. |
| 4 | Add `frames/frame_NNNNNN.jpg` files to ZIP in sequential order. |
| 5 | Serialize and add `details.json` (§6.2). |
| 6 | Serialize and add `measurements.json` (§6.3). |
| 7 | Close ZIP stream. |
| 8 | Delete session temp directory. |
| 9 | Navigate → UploadScreen with ZIP path + UUID. |

### 6.2 details.json

```json
{
  "initialSelection": "<string from SelectionScreen>",
  "dropdownSelection": "<string from FormScreen>",
  "size": 123,
  "detail": "ABC123",
  "gt": [1.2, 3.4, 5.6],
  "scanTimestamp": "ISO-8601 UTC string",
  "deviceInfo": "<Build.MANUFACTURER> <Build.MODEL> (Android <Build.VERSION.RELEASE>)",
  "appVersion": "<versionName from BuildConfig>"
}
```

- `gt` is `null` (JSON null, not the string `"null"`) when not provided.
- `scanTimestamp` is the ISO-8601 UTC timestamp of when the scan session started (first frame captured).
- Serialized via `kotlinx.serialization`.

### 6.3 measurements.json

See §5.5.6 for schema. Serialized via `kotlinx.serialization`.

### 6.4 ZIP Filename

```
{UUID}_{unixTimestampSeconds}.zip
```

Example: `550e8400-e29b-41d4-a716-446655440000_1712345678.zip`

---

## 7. Screen: UploadScreen

### 7.1 Entry Condition
Navigation from PackagingScreen with `zipPath: String` and `sessionUUID: String`.

### 7.2 Upload Flow State Machine

```
AUTHENTICATING
    │ Firebase signInAnonymously() completes
    ▼
UPLOADING  { progress: 0..100% }
    │ success
    ▼
UPLOAD_SUCCESS → [TBD-A7]

    │ failure (network, Firebase error)
    ▼
UPLOAD_FAILED
    │ ZIP preserved locally; WorkManager job enqueued
    ▼
RETRY_QUEUED → [TBD-A7]
```

### 7.3 Firebase Anonymous Auth

- `FirebaseAuth.getInstance().signInAnonymously()` called if no current user.
- If a valid anonymous user already exists in the session, re-use without re-authenticating.
- Auth failure is treated as upload failure (→ UPLOAD_FAILED).

### 7.4 Firebase Storage Upload

- Upload path: `[TBD-A5 — default proposed]` `scans/{firebaseUid}/{zipFilename}`
- Upload using `StorageReference.putFile()`.
- Upload progress exposed to UI as percentage.
- On `StorageException` or auth error → transition to UPLOAD_FAILED.

### 7.5 Failure Handling

| Action | Detail |
|--------|--------|
| Preserve ZIP | File already in app-specific storage; no move needed |
| Persist retry metadata | Room entity `PendingUpload { id, zipPath, zipFilename, sessionUUID, enqueuedAt }` |
| Enqueue WorkManager job | `UploadWorker` with `NETWORK_CONNECTED` constraint and `ExponentialBackoffPolicy` |
| WorkManager retry | Max retries: unlimited; backoff: exponential starting at 30 s |

### 7.6 UploadWorker Behavior

| Step | Detail |
|------|--------|
| 1 | Load `PendingUpload` record from Room by job input UUID |
| 2 | Re-authenticate anonymously (or reuse existing session) |
| 3 | Attempt upload |
| 4 | On success: delete `PendingUpload` record; delete local ZIP file |
| 5 | On failure: return `Result.retry()` |

### 7.7 UI States

| State | UI |
|-------|----|
| AUTHENTICATING | Spinner + "Preparing upload…" |
| UPLOADING | Progress bar + percentage + filename |
| UPLOAD_SUCCESS | Success indicator + `[TBD-A7: next action]` |
| UPLOAD_FAILED | Error message + "Will retry automatically" + `[TBD-A7: next action]` |
| RETRY_QUEUED | "Upload scheduled for retry" indicator |

---

## 8. Permissions

### 8.1 Required Permissions

| Permission | Type | When Requested |
|------------|------|----------------|
| `CAMERA` | Dangerous | At ScanScreen entry, before camera init |
| `INTERNET` | Normal (manifest only) | N/A |
| `ACCESS_NETWORK_STATE` | Normal (manifest only) | N/A |
| Storage (scoped) | Handled via app-specific dirs | No runtime request needed for API 29+ |

### 8.2 ARCore

- ARCore availability check SHALL be performed at ScanScreen entry via `ArCoreApk.getInstance().checkAvailability()`.
- If `UNSUPPORTED_DEVICE`: display non-dismissable error; scan cannot proceed.
- If `UNKNOWN_CHECKING` / `UNKNOWN_TIMED_OUT`: retry once; if still unknown, treat as unsupported.
- If install required (`SUPPORTED_NOT_INSTALLED`): trigger ARCore install flow.

### 8.3 Permission Denial Handling

| Scenario | Behavior |
|----------|---------|
| Camera denied | Show rationale and "Open Settings" button; scan cannot proceed without camera |
| Camera permanently denied | Deep-link to app settings; inform user |
| ARCore unsupported | Non-recoverable error screen; no further navigation |

---

## 9. Validation Summary

| Field | Rule | Error Trigger |
|-------|------|--------------|
| `initialSelection` | Non-null selection | Proceed disabled if null |
| `dropdownSelection` | Non-null selection | Proceed disabled if null |
| `size` | Int, > 0, non-empty | Inline error + Proceed disabled |
| `detail` | Non-empty, ≤ 16 chars, `[A-Za-z0-9]+` | Inline error + Proceed disabled |
| `gt` | Empty OR all tokens parse as Double | Inline error + Proceed disabled |

---

## 10. Data Flow Summary

```
SelectionScreen
    └─ selectedOption: String
FormScreen
    └─ FormData { selectedOption, dropdownSelection, size, detail, gt }
ScanScreen
    └─ ScanSession {
           formData: FormData,
           sessionUUID: UUID,
           frames: List<FrameRecord>,
           measurement: ARMeasurement?
       }
PackagingScreen
    └─ ZipArtifact { zipPath: String, sessionUUID: String, zipFilename: String }
UploadScreen
    └─ UploadResult { success: Boolean, zipPath: String (if failed) }
```

---

## 11. Carried-Forward Ambiguities

The following items from PRD Section 9 remain unresolved. Defaults applied are marked **(default applied)** and SHALL be confirmed before Phase 3.

| ID | Applied Default / Status |
|----|--------------------------|
| A-1 | No default. Option labels/keys MUST be provided before screen specs (Phase 5). |
| A-2 | No default. Dropdown labels/keys MUST be provided before screen specs (Phase 5). |
| A-3 | **Default applied:** min SDK = API 24. |
| A-4 | **Default applied:** translation threshold = 0.30 m (compile-time constant). |
| A-5 | **Default applied:** Firebase Storage path = `scans/{firebaseUid}/{zipFilename}`. Confirm in Phase 3. |
| A-6 | **Default applied:** back navigation disabled after FormScreen → ScanScreen transition. |
| A-7 | **Default applied:** show UploadResultScreen with "Start New Scan" button that resets to SelectionScreen. |
| A-8 | **Default applied:** scan ends on explicit user tap of Stop button (no fixed count or time limit). |

---

*End of Phase 2 — Functional Specification*  
*Awaiting approval to proceed to Phase 3: docs/architecture.md + docs/technical_decisions.md*
