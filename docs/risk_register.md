# Risk Register
## Android SfM Scanning App

**Version:** 1.0  
**Status:** Draft — Awaiting Phase 6 Approval  
**Date:** 2026-05-24  
**Depends on:** docs/implementation_plan.md  
**Derived from:** specs/master_spec.md, specs/constraints.md

---

## 0. Scoring

| Dimension | 1 | 2 | 3 |
|-----------|---|---|---|
| **Likelihood** | Low — unlikely in normal use | Medium — occurs on some devices or conditions | High — expected to occur regularly |
| **Impact** | Low — degraded UX, workaround exists | Medium — feature partially broken, data at risk | High — app crash, data loss, or blocked ship |
| **Priority** = Likelihood × Impact | 1–2: Monitor | 3–4: Mitigate | 6–9: Must resolve before ship |

---

## 1. Risk Table

| ID | Title | L | I | P | Status |
|----|-------|---|---|---|--------|
| R-01 | ARCore Depth API not available | 3 | 1 | 3 | Mitigated |
| R-02 | CameraX cannot achieve 5 FPS target on device | 2 | 2 | 4 | Mitigated |
| R-03 | OOM crash during long scan session | 2 | 3 | 6 | Must resolve |
| R-04 | Fixed focus / AE lock not supported on device | 3 | 1 | 3 | Mitigated |
| R-05 | ARCore tracking lost mid-scan | 3 | 2 | 6 | Must resolve |
| R-06 | Translation threshold never reached | 2 | 2 | 4 | Mitigated |
| R-07 | Firebase anonymous auth session expires mid-upload | 1 | 2 | 2 | Monitor |
| R-08 | ZIP too large for device storage | 1 | 3 | 3 | Mitigated |
| R-09 | WorkManager job evicted (Doze / battery saver) | 2 | 1 | 2 | Monitor |
| R-10 | Corrupt ZIP from mid-write process kill | 1 | 3 | 3 | Mitigated |
| R-11 | Nav argument size limit exceeded | 1 | 2 | 2 | Monitor |
| R-12 | A-1 / A-2 ambiguities unresolved at M5 | 3 | 2 | 6 | Must resolve |
| R-13 | ARCore session lifecycle leak | 2 | 2 | 4 | Mitigated |
| R-14 | Camera2Interop breaks on specific OEM firmware | 2 | 1 | 2 | Monitor |
| R-15 | Firebase Storage rules misconfigured | 2 | 3 | 6 | Must resolve |
| R-16 | App killed mid-packaging; temp frames lost | 1 | 2 | 2 | Monitor |
| R-17 | JPEG Q95 produces ZIP too large for upload | 1 | 2 | 2 | Monitor |

---

## 2. Risk Details

---

### R-01 — ARCore Depth API Not Available
**Likelihood:** High (many mid-range devices lack depth sensors)  
**Impact:** Low (fallback produces valid but lower-accuracy measurements)  
**Priority:** 3 — Mitigated

**Description:**  
`Config.DepthMode.AUTOMATIC` requires a depth sensor (ToF or structured light). The majority of Android devices do not have one. If Depth API is unavailable, center-pixel raycasts fall through to plane or feature-point hits, which are less geometrically accurate for arbitrary objects.

**Mitigation:**  
Fallback chain implemented in `ArMeasurementPipeline` (TD-07): `DEPTH` → `PLANE` → `INSTANT_PLACEMENT` → `FEATURE_POINT`. `hitType` is recorded in `measurements.json` so downstream COLMAP pipelines can weight measurements accordingly. No user-facing error; AR chip shows `"Tracking"` regardless of hit type.

**Residual risk:**  
Feature-point hits on textureless objects may produce `hitType = NONE` if no point cloud features are visible at screen center. See R-06.

**Owner action:** Confirm whether depth availability should gate scan start or only affect measurement accuracy annotation.

---

### R-02 — CameraX Cannot Achieve 5 FPS Target
**Likelihood:** Medium (unusual camera HALs; CameraX `setTargetFrameRate` is advisory)  
**Impact:** Medium (SfM reconstruction quality degrades with inconsistent frame spacing)  
**Priority:** 4 — Mitigated

**Description:**  
CameraX `setTargetFrameRate(Range(5, 5))` is a hint to the HAL, not a guarantee. Some devices may not support low-FPS modes cleanly, delivering frames in bursts rather than evenly spaced.

**Mitigation:**  
Software timestamp gate (TD-03) enforces the 200 ms minimum interval regardless of HAL behavior. Timestamps are recorded per frame in `FrameRecord.timestampMs`, allowing downstream tools to detect jitter. Gate is independent of HAL behavior.

**Residual risk:**  
If the HAL cannot deliver frames faster than ~2 FPS on certain devices, the effective rate will be below 5 FPS. This is a device capability limitation; no software workaround exists.

**Owner action:** Identify and test on the lowest-capability target device before M9.

---

### R-03 — OOM Crash During Long Scan Session
**Likelihood:** Medium (large JPEG buffers × high frame count × ARCore memory footprint)  
**Impact:** High (crash loses all captured frames)  
**Priority:** 6 — **Must resolve before ship**

**Description:**  
Each 2K JPEG frame at Q95 is approximately 0.4–0.8 MB. A 5-minute scan at 5 FPS produces 1,500 frames (~600 MB–1.2 GB of data). Combined with ARCore session memory and Compose UI allocations, heap pressure is substantial.

**Mitigations:**  
1. `STRATEGY_KEEP_ONLY_LATEST` on `ImageAnalysis` — only one frame queued at a time in the CameraX pipeline.
2. `MAX_FRAMES_IN_FLIGHT = 3` — at most 3 JPEG write operations pending simultaneously; new frames dropped if queue full (data_contracts.md §8).
3. Frame files written to `cacheDir` immediately; raw `ByteArray` released after write (not retained in memory).
4. `FrameRecord` objects held in memory contain only metadata (path + timestamp), not JPEG bytes.
5. OOM stress test required in M9.

**Residual risk:**  
Very long scans (> 500 frames) on devices with < 3 GB RAM may still approach OOM. A maximum frame count warning (e.g., after 300 frames) should be considered.

**Owner action:** Define maximum expected scan duration / frame count to scope memory requirements.

---

### R-04 — Fixed Focus / AE Lock Not Supported
**Likelihood:** High (many devices do not support `CONTROL_AF_MODE_OFF` or AE lock)  
**Impact:** Low (spec says "if feasible"; autofocus degradation is acceptable)  
**Priority:** 3 — Mitigated

**Description:**  
`Camera2Interop` focus and exposure lock depends on device-reported camera capabilities. Devices that do not support `CONTROL_AF_MODE_OFF` will continue with continuous autofocus. This produces frame-to-frame focus variation, which reduces SfM feature match quality.

**Mitigation:**  
Implementation falls back gracefully (TD-05, TD-06): logs a warning; does not block scan start. `CameraConfig.lockFocus` and `lockExposure` flags allow this to be toggled per-device if needed.

**Residual risk:**  
Scans captured without focus lock on high-motion sequences may have more feature match failures in COLMAP. This is an inherent device limitation, not an app defect.

---

### R-05 — ARCore Tracking Lost Mid-Scan
**Likelihood:** High (poor lighting, fast motion, textureless surfaces)  
**Impact:** Medium (measurements.json may be incomplete; reconstruction scale reference missing)  
**Priority:** 6 — **Must resolve before ship**

**Description:**  
ARCore tracking state transitions to `PAUSED` or `STOPPED` when visual features are insufficient. If tracking is lost before Point A is captured, `measurements.json` will have `pointA = null`. If lost between Point A and the translation threshold, `pointB = null` and `distanceMeters = null`.

**Mitigations:**  
1. AR status chip provides real-time `TRACKING_LOST` feedback to the user (screen_specs.md §5.5).
2. `measurements.json` schema explicitly supports partial measurements — all nullable fields (data_contracts.md §2.2).
3. Instruction overlay during READY state prompts user to ensure good lighting and slowly move around the object.
4. `ArMeasurementPipeline` resumes automatically when tracking is restored (functional_spec.md §5.5.5).

**Residual risk:**  
If tracking is permanently lost (e.g., entirely dark room), the scan produces a ZIP with a null measurement. The COLMAP pipeline must handle this gracefully. `hitType = NONE` and `trackingState != TRACKING` in `measurements.json` signal this condition.

**Owner action:** Define minimum acceptable scanning conditions and whether to warn the user when `TRACKING_LOST` persists for > N seconds.

---

### R-06 — Translation Threshold Never Reached
**Likelihood:** Medium (user scans in place; short scan duration; objects scanned at close range)  
**Impact:** Medium (no scale reference captured; `distanceMeters = null`)  
**Priority:** 4 — Mitigated

**Description:**  
If the user does not move the camera ≥ 0.30 m from the Point A capture location, Point B is never triggered and `distanceMeters` remains null. This is the most common foreseeable incomplete-measurement scenario.

**Mitigation:**  
1. Instruction overlay (`"Slowly move around the object"`) encourages camera movement.
2. `measurements.json` correctly records the partial state — downstream processing handles null distance.
3. Default threshold (0.30 m) is conservative enough to be reachable in typical use without being so large it requires extreme camera movement.

**Residual risk:**  
Users scanning very small objects at very close range (< 30 cm working distance) may never reach the threshold. Consider a dynamic threshold based on initial hit distance.

**Owner action:** Confirm `AR_TRANSLATION_THRESHOLD_METERS = 0.30f` against expected object sizes and scanning distances `[TBD-A4]`.

---

### R-07 — Firebase Anonymous Auth Session Expires Mid-Upload
**Likelihood:** Low (anonymous sessions are long-lived; upload is typically < 5 minutes)  
**Impact:** Medium (upload fails; triggers WorkManager retry)  
**Priority:** 2 — Monitor

**Description:**  
Firebase anonymous auth tokens expire but are auto-refreshed by the SDK. Session expiry during a single upload is extremely unlikely. If it does occur, the upload fails and WorkManager re-authenticates on the next attempt.

**Mitigation:**  
`UploadWorker` calls `signInAnonymously()` at the start of each retry attempt, ensuring a fresh token. `AnonymousAuthSource` reuses existing valid users (TD-18).

---

### R-08 — ZIP Too Large for Device Storage
**Likelihood:** Low (typical scans are manageable; extreme edge cases possible)  
**Impact:** High (I/O error during packaging; session data lost)  
**Priority:** 3 — Mitigated

**Description:**  
A 10-minute scan at 5 FPS produces ~3,000 frames. At JPEG Q95 2K (~0.6 MB average), this is ~1.8 GB. Many devices have limited internal storage. `filesDir` has no hard quota on most devices but I/O failure is possible.

**Mitigation:**  
1. `ZipBuilder` wraps `ZipOutputStream`; `IOException` is caught and propagated as `PackagingFailure`.
2. `PackagingScreen` handles failure and navigates to `UploadScreen` with an error state.
3. Session temp dir in `cacheDir` (OS-managed); if `cacheDir` is evicted mid-scan, packaging detects missing frames.
4. For extreme scans, a frame count warning in `ScanScreen` (after N frames) is a recommended future safeguard.

**Owner action:** Define maximum expected scan duration to confirm storage budget assumptions.

---

### R-09 — WorkManager Job Evicted (Doze / Battery Saver)
**Likelihood:** Medium (aggressive battery saver modes on some OEMs)  
**Impact:** Low (job re-runs when constraints are met; ZIP preserved on disk)  
**Priority:** 2 — Monitor

**Description:**  
WorkManager jobs with `NETWORK_CONNECTED` constraint are deferred in Doze mode. Some OEM battery optimizations (Huawei, Xiaomi) may kill WorkManager jobs aggressively.

**Mitigation:**  
WorkManager is designed for this scenario; jobs survive process death and are re-enqueued after reboot. `PendingUpload` in Room ensures the ZIP and metadata are not lost. No data loss risk — only upload delay.

**Residual risk:**  
On heavily restricted OEM devices, background work may require the user to whitelist the app in battery settings. This is a known Android ecosystem limitation.

---

### R-10 — Corrupt ZIP from Mid-Write Process Kill
**Likelihood:** Low (process kill during packaging is rare)  
**Impact:** High (partial ZIP is not a valid archive; data effectively lost)  
**Priority:** 3 — Mitigated

**Description:**  
If the app process is killed while `ZipOutputStream` is open, the resulting file is an incomplete ZIP (no central directory). The file appears to exist in `filesDir/uploads/` but cannot be opened.

**Mitigation:**  
ZIP atomicity strategy (architecture.md §9.2, TD-11):  
1. ZIP is written to a temp path (`filesDir/uploads/{uuid}.zip.tmp`).
2. After `ZipOutputStream.close()` succeeds, the file is renamed to `{uuid}.zip` (rename is atomic on the same filesystem).
3. `UploadWorker` only processes files ending in `.zip` (not `.zip.tmp`).
4. On next cold start, any `.zip.tmp` files in `filesDir/uploads/` are deleted (stale partial writes).

---

### R-11 — Nav Argument Size Limit Exceeded
**Likelihood:** Low (`FormData` is small; ~200 bytes serialized)  
**Impact:** Medium (navigation crash; `TransactionTooLargeException`)  
**Priority:** 2 — Monitor

**Description:**  
Android's `Bundle` size limit is approximately 1 MB. `FormData` serialized as JSON is ~200 bytes, well within limits. `ZipArtifact` is similar. This risk is theoretical.

**Mitigation:**  
`FormData` and `ZipArtifact` are small, bounded objects. No arrays of unbounded size are passed as nav args. `ScanSession` (large: frame list) is NOT passed as a nav arg — it is held in `SavedStateHandle` / ViewModel scope.

---

### R-12 — A-1 / A-2 Ambiguities Unresolved at M5
**Likelihood:** High (no owner response yet)  
**Impact:** Medium (M5-B and M5-C cannot be completed; blocks M6, M7, M8, M9)  
**Priority:** 6 — **Must resolve before ship**

**Description:**  
The Initial Selection option labels (A-1) and Dropdown option labels (A-2) have no defined values. `feature-selection` and `feature-form` contain hardcoded `[TBD]` placeholders. These modules cannot reach their exit criteria without real content.

**Mitigation:**  
Implementation of M5-B and M5-C can begin with placeholder strings. Exit criteria requiring correct stored values (`initialSelection`, `dropdownSelection`) are blocked until real values are provided.

**Owner action:** Provide option labels and stored string keys for A-1 and A-2 before M5 exit criteria are evaluated.

---

### R-13 — ARCore Session Lifecycle Leak
**Likelihood:** Medium (incorrect lifecycle observer teardown)  
**Impact:** Medium (memory leak; possible GPU resource leak; crash on re-entry)  
**Priority:** 4 — Mitigated

**Description:**  
`ARCore Session` is a native resource. If `Session.close()` is not called when `ScanScreen` is permanently left (e.g., back navigation after scan), native memory leaks occur. If the session is not paused on Activity `onPause`, the camera pipeline may conflict.

**Mitigation:**  
`ArSessionManager` registers a `DefaultLifecycleObserver` on the provided `LifecycleOwner` (TD-08):  
- `onPause` → `session.pause()`  
- `onResume` → `session.resume()`  
- `onDestroy` → `session.close()`  
`ScanViewModel.onCleared()` also calls `arRepository.destroySession()` as a safety net.

---

### R-14 — Camera2Interop Breaks on Specific OEM Firmware
**Likelihood:** Medium (OEM Camera2 implementations are inconsistent)  
**Impact:** Low (falls back to autofocus; scan still works)  
**Priority:** 2 — Monitor

**Description:**  
Some OEM Camera2 HAL implementations do not handle `Camera2Interop` extensions gracefully and may throw undocumented exceptions or silently ignore settings.

**Mitigation:**  
All `Camera2Interop` calls are wrapped in `try-catch`. Failures are logged; the camera session continues without the locked setting. No user-facing impact — scan proceeds with autofocus/auto-exposure.

---

### R-15 — Firebase Storage Rules Misconfigured
**Likelihood:** Medium (default rules deny all writes)  
**Impact:** High (all uploads fail; WorkManager retries forever; no data reaches storage)  
**Priority:** 6 — **Must resolve before ship**

**Description:**  
Firebase Storage default rules (`allow read, write: if false`) deny all operations. Anonymous-auth uploads require a rule permitting writes by authenticated users to `scans/{uid}/`.

**Required rule:**
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /scans/{userId}/{allPaths=**} {
      allow write: if request.auth != null && request.auth.uid == userId;
      allow read: if false;  // No client-side reads needed
    }
  }
}
```

**Mitigation:**  
Firebase Storage rules must be deployed before M7 integration testing. Rules are stored in `firebase/storage.rules` in the repo and deployed via `firebase deploy --only storage`.

**Owner action:** Deploy and verify rules on Firebase project before M7 exit criteria are evaluated.

---

### R-16 — App Killed Mid-Packaging; Temp Frames Lost
**Likelihood:** Low (packaging is typically fast; < 30 s)  
**Impact:** Medium (scan data lost; user must re-scan)  
**Priority:** 2 — Monitor

**Description:**  
Session temp frames live in `cacheDir`. If the OS evicts `cacheDir` during an active packaging operation (extremely unlikely under normal conditions), frame files may be missing when `ZipBuilder` tries to add them.

**Mitigation:**  
`ZipBuilder.addFile()` throws `IOException` if a source file is missing. `PackageSessionUseCase` propagates this as a packaging failure, shown on `UploadScreen` as `packaging_error_title`. No silent corruption — the failure is surfaced.

---

### R-17 — JPEG Q95 Produces ZIP Too Large for Firebase Upload
**Likelihood:** Low (Firebase Storage limit is 5 TB per object; practical limit is upload time)  
**Impact:** Medium (very long upload times on slow connections; WorkManager timeout)  
**Priority:** 2 — Monitor

**Description:**  
A large ZIP (e.g., 1 GB from a 10-minute scan) may time out on a slow connection. Firebase Storage does not have a practical size limit, but `WorkManager` tasks have a default timeout.

**Mitigation:**  
Firebase Storage `putFile()` is resumable — it uses Firebase Storage's built-in resumable upload protocol. Network interruptions mid-upload do not require restarting from byte 0. If the WorkManager task is killed, the next retry resumes from where the upload left off (Firebase SDK handles this internally via upload session URIs).

---

## 3. Pre-Ship Must-Resolve Summary

| ID | Title | Owner Action |
|----|-------|-------------|
| R-03 | OOM during scan | M9 stress test required; max frame count warning recommended |
| R-05 | ARCore tracking loss | Define minimum scan conditions; confirm partial-measurement handling with COLMAP team |
| R-12 | A-1 / A-2 unresolved | Provide option labels before M5 begins |
| R-15 | Firebase rules | Deploy `storage.rules` before M7 integration testing |

---

*End of Risk Register*  
*End of Phase 6*
