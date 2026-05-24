# Technical Decisions
## Android SfM Scanning App

**Version:** 1.0  
**Status:** Draft — Awaiting Phase 3 Approval  
**Date:** 2026-05-24  
**Depends on:** docs/architecture.md  
**Derived from:** specs/constraints.md, specs/master_spec.md

---

## Format

Each decision follows:
- **Decision:** what was decided
- **Rationale:** why
- **Alternatives considered:** what was rejected and why
- **Constraints satisfied:** which spec constraints this fulfills
- **Risk / caveat:** anything to watch

---

## TD-01: Single-Activity Architecture

**Decision:** One `MainActivity`, all screens rendered as Compose destinations within a single `NavHost`.

**Rationale:** Aligns with modern Android architecture guidance. Compose Navigation manages the back stack; no fragment transactions required. Simplifies shared ViewModel scoping and nav argument passing.

**Alternatives considered:**
- Multiple Activities: rejected — unnecessary complexity with Compose; complicates shared state across ScanScreen → UploadScreen.
- Fragment + Navigation Component: rejected — Compose-first app; mixing fragments adds overhead with no benefit.

**Constraints satisfied:** Jetpack Compose only; MVVM.

---

## TD-02: CameraX `ImageAnalysis` Use Case for Frame Capture

**Decision:** Use `CameraX ImageAnalysis` (not `ImageCapture`) as the primary frame acquisition path, with `ImageCapture` bound simultaneously for optional still-capture capability.

**Rationale:** `ImageCapture` triggers a full shutter cycle per frame and is not designed for continuous 5 FPS capture. `ImageAnalysis` delivers a continuous `ImageProxy` stream from the camera pipeline, which can be throttled to 5 FPS in software. This is the correct API for real-time frame extraction.

The `ImageCapture` use case is bound alongside `ImageAnalysis` only if explicit still-capture behavior is needed; otherwise `ImageAnalysis` alone satisfies the spec.

**Alternatives considered:**
- `VideoCapture` + frame extraction: rejected — spec explicitly prohibits video compression pipeline; VideoCapture uses codec encoding.
- `Camera2` API directly: rejected — CameraX is a hard constraint; Camera2 can be accessed via `Camera2Interop` for fine-grained controls if needed.

**Constraints satisfied:** CameraX; capture image frames NOT video; exact 5 FPS; avoid OOM.

**Risk:** `ImageAnalysis` delivers frames on a background executor; back-pressure must be managed via `STRATEGY_KEEP_ONLY_LATEST` to prevent queue buildup → OOM.

---

## TD-03: Software FPS Throttling via Timestamp Gate

**Decision:** A timestamp gate is applied inside the `ImageAnalysis.Analyzer`: accept a frame only if `now - lastAcceptedMs ≥ 200ms`. Rejected frames are returned immediately (`imageProxy.close()`).

**Rationale:** CameraX does not guarantee exact FPS output at arbitrary non-standard rates. Requesting 5 FPS via `setTargetFrameRate` is advisory; actual delivery varies. The timestamp gate is the only reliable enforcement mechanism.

**Alternatives considered:**
- `setTargetFrameRate(Range(5, 5))`: kept as a hint to the camera HAL (reduces power consumption) but NOT relied upon as the sole mechanism.
- Timer-triggered `ImageCapture.takePicture()`: rejected — does not align with `ImageAnalysis` continuous stream; adds shutter lag and is not suitable for 5 FPS continuous capture.

**Constraints satisfied:** exact 5 FPS target.

**Risk:** On some devices, the camera HAL may still deliver frames at a higher rate internally. The gate handles this correctly — excess frames are dropped before encoding.

---

## TD-04: JPEG Encoding Quality = 95

**Decision:** Frames are encoded as JPEG at quality 95 (not the Android system default of 85).

**Rationale:** The spec requires "photogrammetry-friendly settings" with "minimal processing." JPEG Q95 retains sufficient texture detail for feature matching (SIFT/SuperPoint) while remaining vastly smaller than lossless PNG. Q85 introduces visible blocking artifacts at feature-rich surfaces. Q100 produces files 3–5× larger than Q95 with no measurable reconstruction benefit.

**Alternatives considered:**
- PNG (lossless): rejected — typical 2K PNG is 4–8 MB/frame vs ~0.4–0.8 MB at JPEG Q95; 100-frame scan = 400–800 MB vs 40–80 MB. OOM and ZIP size become untenable.
- WEBP lossless: rejected — decoder availability on COLMAP side is less universal than JPEG.
- HEIC: rejected — ARCore/CameraX `ImageProxy` delivers `YUV_420_888`; HEIC encoding on Android < API 28 is not available.

**Constraints satisfied:** JPEG acceptable; minimal processing; avoid OOM.

---

## TD-05: Fixed Focus via Camera2Interop

**Decision:** Apply fixed focus (manual focus mode) at scan session start using `Camera2Interop.Extender` on the `ImageAnalysis` use case, if `CONTROL_AF_MODE_OFF` and `LENS_FOCUS_DISTANCE` are supported by the device.

**Rationale:** Autofocus hunting between frames is one of the primary sources of frame-to-frame inconsistency in photogrammetry datasets. Locking focus eliminates this. `Camera2Interop` provides access to Camera2 controls through the CameraX abstraction.

**Alternatives considered:**
- Rely on CameraX `FocusMeteringAction`: rejected — triggers AF cycles, not focus lock.
- Accept default autofocus: rejected — spec requires fixed focus if feasible.

**Constraints satisfied:** fixed focus if feasible.

**Risk:** Not all devices report `CONTROL_AF_MODE_OFF`. The implementation must fall back to continuous autofocus gracefully and log a warning.

---

## TD-06: Fixed Exposure via Camera2Interop

**Decision:** Apply manual exposure (disable AE) at scan session start using `Camera2Interop.Extender`, locking `CONTROL_AE_MODE_OFF` with the current auto-exposure values captured immediately before lock.

**Rationale:** Exposure variation between frames creates intensity inconsistencies that affect SfM feature descriptors. Locking exposure at a scene-appropriate value before scan start eliminates this.

**Alternatives considered:**
- AE lock (`CONTROL_AE_LOCK = true`): preferred first attempt — cleaner than full manual mode; falls back to full manual if AE lock is not supported.
- Accept default AE: rejected — spec requires fixed exposure if feasible.

**Constraints satisfied:** fixed exposure if feasible.

**Risk:** Very dark or very bright scenes may result in a poor exposure lock. This is acceptable — the user controls the torch and scene setup.

---

## TD-07: ARCore Depth API with `HitResult` Fallback Chain

**Decision:** ARCore depth-based raycasting is preferred. The implementation follows this fallback chain:

1. `DepthPoint` hit (requires `Config.DepthMode.AUTOMATIC` support)
2. `Plane` hit (requires plane detection; no depth)
3. `InstantPlacementPoint` hit (instant placement, less accurate)
4. `FeaturePoint` hit (raw point cloud; least accurate)
5. No hit → retry on next frame (up to 30 frames); record `hitType = NONE` if exhausted

**Rationale:** Depth-based hit testing gives world-space positions directly on object surfaces, which is what COLMAP scale references require. Plane hits are less accurate for arbitrary objects not on a flat surface. The fallback chain ensures the app never blocks on missing depth support while recording hit quality for downstream use.

**Alternatives considered:**
- Depth-only, no fallback: rejected — spec explicitly requires depth-preferred fallback strategy.
- Manual tap-based placement: rejected — spec explicitly prohibits manual tapping.

**Constraints satisfied:** ARCore; automatic measurement; center-pixel raycast; depth-preferred fallback strategy required.

---

## TD-08: ARCore Session Lifecycle Tied to ScanScreen

**Decision:** The ARCore `Session` is created when `ScanScreen` enters composition and destroyed when `ScanScreen` leaves composition permanently. Session is paused/resumed on Activity lifecycle events (`onPause`/`onResume`).

**Rationale:** ARCore session creation is expensive. Tying it to the screen that uses it (ScanScreen) and managing pause/resume correctly prevents resource leaks and unnecessary initialization.

**Alternatives considered:**
- Application-scoped ARCore session: rejected — wastes resources on screens that don't need AR; complicates state machine.
- ViewModel-scoped session: correct scope, but lifecycle callbacks (`onPause`/`onResume`) must still be forwarded from the Activity; implemented via `LifecycleEventObserver` in `ArSessionManager`.

**Constraints satisfied:** ARCore session lifecycle management.

**Risk:** If the user navigates away from ScanScreen mid-scan (e.g., phone call), the session must pause cleanly and resume correctly. `ArSessionManager` handles `Session.pause()` / `Session.resume()` via lifecycle observer.

---

## TD-09: WorkManager `ExponentialBackoffPolicy` for Upload Retry

**Decision:** `UploadWorker` is enqueued with:
- `Constraints`: `NetworkType.CONNECTED`
- `BackoffPolicy`: `EXPONENTIAL`
- Initial backoff delay: 30 seconds
- Max retries: no explicit limit (WorkManager default: 10× then abandons → override with `KEEP` policy)

**Rationale:** Upload failures are typically transient (network unavailable, Firebase quota). Exponential backoff avoids hammering Firebase on repeated failures. `NETWORK_CONNECTED` constraint ensures no retry attempt is made offline.

**Alternatives considered:**
- Linear backoff: rejected — unnecessary load on Firebase if there is a sustained outage.
- Foreground Service for upload: considered — overkill for file upload that can tolerate delay; WorkManager is sufficient and correct.
- Manual retry button only: rejected — spec requires WorkManager retry strategy.

**Constraints satisfied:** WorkManager retry uploads.

---

## TD-10: Room for `PendingUpload` Persistence Only

**Decision:** Room is used for one purpose only: persisting `PendingUpload` records that represent ZIPs awaiting WorkManager retry. Room is NOT used for scan session metadata, frame lists, or any other data.

**Rationale:** The spec says "Room if needed." The only data that must survive process death and be queryable is the upload retry queue. All other scan data is either in the ZIP file itself or transient in-memory state.

**Alternatives considered:**
- `DataStore` (Proto or Preferences): rejected — not suited to a list of entities with per-record lifecycle management.
- File-based JSON persistence for pending uploads: considered — simpler but fragile under concurrent access and harder to query/clean up reliably.

**Constraints satisfied:** Room if needed.

---

## TD-11: `java.util.zip.ZipOutputStream` for ZIP Construction

**Decision:** Use `java.util.zip.ZipOutputStream` directly (standard library). No third-party ZIP library.

**Rationale:** The ZIP structure is simple: a flat `frames/` folder + two JSON files. `ZipOutputStream` handles this without additional dependencies. Introducing a third-party library for a use case the stdlib handles correctly adds APK size and a dependency maintenance burden.

**Alternatives considered:**
- `zip4j`: rejected — no features needed beyond what stdlib provides.
- `apache-commons-compress`: rejected — same reason.

**Constraints satisfied:** ZIP archive output; production-grade code.

---

## TD-12: `kotlinx.serialization` for All JSON

**Decision:** All JSON encoding/decoding uses `kotlinx.serialization` exclusively. No Gson or Moshi.

**Rationale:** Hard constraint from specs/constraints.md. Additionally, `kotlinx.serialization` is the idiomatic choice in Kotlin-first projects; it integrates with data classes without reflection (compile-time code generation), which is safer and faster.

**Constraints satisfied:** kotlinx.serialization JSON.

---

## TD-13: Session Temporary Directory in `cacheDir`

**Decision:** In-progress frame files are written to `context.cacheDir/sessions/{uuid}/frames/`. Completed ZIPs are moved to `context.filesDir/uploads/`.

**Rationale:**
- `cacheDir`: OS may evict contents if storage is critically low. This is acceptable for in-progress sessions — they are not yet committed. Eviction during active scan is handled by detecting missing files at packaging time (error → user re-scans).
- `filesDir`: persistent, not evicted by OS. Correct for ZIPs awaiting upload (must survive across process restarts for WorkManager retry).

**Alternatives considered:**
- External app-specific storage (`getExternalFilesDir`): rejected — unavailable without storage permission on older APIs; unnecessary for internal data.
- `cacheDir` for ZIPs too: rejected — eviction would silently delete uploads-in-queue.

**Constraints satisfied:** app-specific storage for failed uploads; avoid OOM (cacheDir has lower priority under memory pressure — OS handles cleanup).

---

## TD-14: `FormData` Passed via Navigation JSON Argument

**Decision:** `FormData` is serialized to a JSON string using `kotlinx.serialization` and passed as a nav argument from `FormScreen` to `ScanScreen`. The JSON string is URL-encoded.

**Rationale:** Compose Navigation does not natively support complex object types as nav arguments. JSON string is the standard workaround. `FormData` is small (5 primitive fields) so the serialized size is negligible.

**Alternatives considered:**
- Shared ViewModel scoped to the nav graph: viable alternative; rejected in favor of nav args to keep ViewModel scopes minimal and avoid accidental state retention.
- `SavedStateHandle` injection: used for `ScanViewModel` to restore `FormData` after process death.

---

## TD-15: `BackHandler` Interception on ScanScreen

**Decision:** `BackHandler(enabled = true) { /* no-op or show confirmation */ }` is placed in `ScanScreen` composable to intercept system back while scanning is in progress.

**Rationale:** Functional spec §1.1 specifies back navigation is disabled after FormScreen → ScanScreen transition. Abandoning a scan mid-capture would leave a partial temp directory and corrupt the session. A no-op (or confirmation dialog) is safer than accidental back navigation.

**Constraints satisfied:** [TBD-A6 default: back disabled during scan].

---

## TD-16: `@HiltWorker` for UploadWorker

**Decision:** `UploadWorker` is a `CoroutineWorker` annotated with `@HiltWorker`. `HiltWorkerFactory` is registered in `ScanApp` as the `WorkManager` factory.

**Rationale:** `UploadWorker` requires injected dependencies (`FirebaseRepository`, `UploadQueueRepository`, `AppDispatchers`). Without `@HiltWorker`, these cannot be injected into the Worker. `HiltWorkerFactory` is the standard Hilt solution.

**Constraints satisfied:** Hilt DI; WorkManager retry.

---

## TD-17: ARCore Translation Threshold = 0.30 m

`[TBD-A4 — default applied]`

**Decision:** The threshold for triggering Point B capture is **0.30 m**, defined as `const val AR_TRANSLATION_THRESHOLD_METERS = 0.30f` in `data-ar`.

**Rationale:** 0.30 m is a practical baseline — large enough that the two reference points produce a measurable and useful scale baseline for COLMAP, but small enough to be achievable in typical indoor scanning distances of 0.5–2 m.

**Risk:** If objects are scanned at very close range (< 0.3 m), the threshold may never be reached. This edge case should be evaluated against the target use case. Owner confirmation required.

---

## TD-18: Firebase Anonymous Auth Reuse Within Session

**Decision:** At upload time, the app checks for an existing `FirebaseUser` via `FirebaseAuth.getInstance().currentUser`. If non-null and not expired, the existing anonymous session is reused. `signInAnonymously()` is only called when no current user exists.

**Rationale:** Anonymous Firebase sessions persist across app restarts. Re-using the session avoids an unnecessary network round-trip and prevents accumulation of abandoned anonymous accounts in the Firebase project.

**Constraints satisfied:** Firebase Anonymous Auth.

---

## Open Technical Questions

| ID | Question | Impact |
|----|----------|--------|
| TQ-01 | Does the target device fleet support `CONTROL_AF_MODE_OFF`? | If not, focus lock is unavailable; spec says "if feasible" so fallback is acceptable but worth knowing |
| TQ-02 | Does the target device fleet support ARCore Depth API? | Determines whether `hitType = DEPTH` will be the common case or the fallback |
| TQ-03 | What is the expected frame count per scan session? | Determines ZIP size estimates and whether `filesDir` capacity is a concern |
| TQ-04 | Should `AR_TRANSLATION_THRESHOLD_METERS` be runtime-configurable (e.g., via Firebase Remote Config)? | Currently hardcoded; if tuning is expected post-release, Remote Config binding should be added in this phase |

---

*End of Technical Decisions*
