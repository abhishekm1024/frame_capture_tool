# Product Requirements Document (PRD)
## Android SfM Scanning App

**Version:** 1.0  
**Status:** Draft — Awaiting Phase 1 Approval  
**Date:** 2026-05-24  
**Derived from:** specs/master_spec.md, specs/constraints.md, specs/architecture_preferences.md

---

## 1. Executive Summary

The Android SfM Scanning App enables users to capture structured image sequences of physical objects for offline 3D reconstruction using COLMAP (Structure-from-Motion). The app automates the capture pipeline — collecting calibrated RGB frames, ARCore-derived metric reference measurements, and user-provided metadata — then packages and uploads everything to Firebase Storage as a single ZIP archive.

The product targets research and/or industrial workflows where repeatable, metric-accurate photogrammetry inputs are needed without relying on specialized hardware beyond a modern ARCore-compatible Android device.

---

## 2. Problem Statement

Producing high-quality SfM reconstructions with COLMAP requires:
1. A dense, well-distributed image sequence of the target object.
2. A known metric scale reference to obtain real-world dimensions.
3. Consistent capture settings (focus, exposure) to minimize inter-frame variance.

Current general-purpose camera apps do not enforce capture rate, do not record AR-derived scale references automatically, and do not package data in a format directly consumable by reconstruction pipelines. This app fills that gap.

---

## 3. Goals

| # | Goal | Measure |
|---|------|---------|
| G1 | Capture image frames at exactly 5 FPS at 2K resolution | Frame timestamps confirm ≤ ±20 ms inter-frame jitter; ≥ 90% frames at target resolution |
| G2 | Record two metric AR anchor points automatically without user tapping | measurements.json present and non-null in every completed ZIP |
| G3 | Package all scan data into a single ZIP per session | ZIP structure validated in automated tests |
| G4 | Upload ZIP to Firebase Storage with reliable retry on failure | WorkManager retry observable; no silent data loss |
| G5 | Guide users through the workflow with minimal friction | All mandatory metadata fields validated before scan begins |

---

## 4. Non-Goals (Out of Scope for v1.0)

- On-device 3D reconstruction or COLMAP execution.
- Real-time 3D preview of the reconstruction.
- Multi-user accounts or signed-in (non-anonymous) Firebase Auth.
- Batch scanning of multiple objects in one session.
- Video recording or video export.
- Editing or deleting past scans from within the app.
- Cloud-side processing status or results surfaced back to the app.
- iOS or cross-platform support.

---

## 5. Target Users

**Primary:** Researchers, engineers, or technicians performing repeated structured scans of physical objects for downstream 3D reconstruction. Assumed to have basic familiarity with photogrammetry concepts but not required to be experts.

**Secondary:** QA/integration engineers validating the capture pipeline.

---

## 6. App Overview and User Journey

The app follows a strictly linear, five-stage flow. No back-navigation between stages is required by the spec (see Ambiguity A-6).

```
[Launch]
   │
   ▼
[Stage 1] Splash Screen
   │  animated logo → auto-advance when animation completes
   ▼
[Stage 2] Initial Selection Screen
   │  user selects exactly one option → tap to advance
   ▼
[Stage 3] Metadata Form Screen
   │  dropdown + size + detail + gt (optional) → "Proceed to Scan" unlocks
   ▼
[Stage 4] Scan Screen
   │  camera live preview + ARCore + flash toggle → user initiates/stops scan
   ▼
[Stage 5] Upload / Result Screen
      ZIP packaged → Firebase upload attempted → success or retry-queued feedback
```

---

## 7. Feature Requirements

### 7.1 Splash Screen (FR-SPLASH)

| ID | Requirement |
|----|-------------|
| FR-SPLASH-01 | Display animated app logo on launch. |
| FR-SPLASH-02 | Automatically transition to Initial Selection Screen when animation completes. |
| FR-SPLASH-03 | No user interaction required or accepted during splash. |

---

### 7.2 Initial Selection Screen (FR-SEL)

| ID | Requirement |
|----|-------------|
| FR-SEL-01 | Display a list of selectable options. Exactly one option must be selected before proceeding. |
| FR-SEL-02 | Attempting to advance without a selection must be prevented (button disabled or equivalent). |
| FR-SEL-03 | Selected value is stored and included in `details.json` as `initialSelection`. |

> **Ambiguity A-1:** The spec does not define the content, labels, or count of the Initial Selection options. See Section 9.

---

### 7.3 Metadata Form Screen (FR-FORM)

| ID | Requirement |
|----|-------------|
| FR-FORM-01 | Single dropdown with exactly two options. Selection is mandatory. |
| FR-FORM-02 | Field `size`: integer, required, must be positive (> 0). |
| FR-FORM-03 | Field `detail`: alphanumeric only, max 16 characters, required. |
| FR-FORM-04 | Field `gt`: optional. If provided, must be a comma-separated list of valid floats (e.g., `12.3, 15.0, 0.42`). If empty or absent, stored as `null` in JSON. |
| FR-FORM-05 | "Proceed to Scan" button is disabled until all mandatory fields are valid and `gt` is valid if provided. |
| FR-FORM-06 | All validated values are carried into `details.json`. |

> **Ambiguity A-2:** The two dropdown option labels are placeholders ("option A", "option B") in the spec. See Section 9.

---

### 7.4 Scan Screen (FR-SCAN)

#### 7.4.1 Camera

| ID | Requirement |
|----|-------------|
| FR-SCAN-01 | Live camera preview shown to user during entire scan. |
| FR-SCAN-02 | Capture image frames at 5 FPS target rate (not video). |
| FR-SCAN-03 | Target resolution: 2560×1440 (2K). A fallback strategy is required if the device does not support this resolution. |
| FR-SCAN-04 | Frames saved as JPEG with photogrammetry-friendly settings: fixed focus if device supports it; fixed exposure if device supports it; minimal post-processing. |
| FR-SCAN-05 | Frames stored individually in a `frames/` folder, named `frame_000001.jpg`, `frame_000002.jpg`, … with timestamps recorded. |
| FR-SCAN-06 | Memory pressure management required; OOM must be avoided. |

#### 7.4.2 Flash / Torch

| ID | Requirement |
|----|-------------|
| FR-SCAN-07 | User-accessible toggle for torch: OFF or MAX INTENSITY ON. |
| FR-SCAN-08 | If torch is enabled at scan start, it must remain constant (no auto-dimming) for the duration of the scan. |

#### 7.4.3 ARCore Automatic Measurement

| ID | Requirement |
|----|-------------|
| FR-SCAN-09 | ARCore session initialized before or alongside the scan session. |
| FR-SCAN-10 | At scan start, app captures Point A: center-pixel raycast to obtain a world-space 3D point, preferring depth-based hit testing. No user tap required. |
| FR-SCAN-11 | Camera translation is continuously monitored during scan. |
| FR-SCAN-12 | Once camera translation from the Point A capture pose exceeds a configured threshold, Point B is captured automatically using center-pixel raycast. |
| FR-SCAN-13 | Metric distance between Point A and Point B is computed and stored. |
| FR-SCAN-14 | `measurements.json` records: Point A (XYZ + timestamp), Point B (XYZ + timestamp), distance in meters, camera pose at each capture, AR tracking state, hit confidence/type. |
| FR-SCAN-15 | If ARCore depth is unavailable, a fallback raycast strategy must be used (plane-based or feature-point). Behavior on fully unsupported devices must be specified in technical docs. |

#### 7.4.4 UX Guidance

| ID | Requirement |
|----|-------------|
| FR-SCAN-16 | On-screen instruction to center the object at scan start. |
| FR-SCAN-17 | Visual indication of scan progress (frame count or time). |
| FR-SCAN-18 | Flash/torch state clearly visible in UI. |
| FR-SCAN-19 | Scan completion triggers clean pipeline shutdown before transitioning. |

---

### 7.5 Data Packaging (FR-PKG)

| ID | Requirement |
|----|-------------|
| FR-PKG-01 | ZIP archive created containing: `frames/` folder, `details.json`, `measurements.json`. |
| FR-PKG-02 | ZIP filename format: `{UUID}_{unix_timestamp}.zip`. |
| FR-PKG-03 | `details.json` contains all form metadata, device info, app version, scan timestamp; `gt` is `null` if not provided. |
| FR-PKG-04 | `measurements.json` contains all ARCore measurement data as specified. |

---

### 7.6 Upload & Failure Handling (FR-UPLOAD)

| ID | Requirement |
|----|-------------|
| FR-UPLOAD-01 | App signs in anonymously via Firebase Anonymous Auth before attempting upload. |
| FR-UPLOAD-02 | ZIP is uploaded to Firebase Storage at a path defined in the architecture spec. |
| FR-UPLOAD-03 | On upload failure, ZIP is preserved in app-specific local storage with its original filename. |
| FR-UPLOAD-04 | Upload retry metadata is persisted and a WorkManager retry job is enqueued. |
| FR-UPLOAD-05 | User receives feedback indicating: uploading, success, or failure + retry-pending state. |

---

### 7.7 Permissions (FR-PERM)

| ID | Requirement |
|----|-------------|
| FR-PERM-01 | Camera permission requested at runtime with appropriate rationale UI. |
| FR-PERM-02 | Internet permission declared in manifest. |
| FR-PERM-03 | ARCore required and install-time check performed per ARCore guidelines. |
| FR-PERM-04 | Storage permission handled for target SDK level (scoped storage / app-specific directories). |

---

## 8. Constraints Summary

The following are non-negotiable and must be preserved exactly throughout all downstream documents:

| Domain | Constraint |
|--------|-----------|
| Language | Kotlin only |
| UI | Jetpack Compose only |
| Architecture | MVVM + Clean Architecture + Hilt |
| Camera | CameraX; 5 FPS; 2K preferred; JPEG frames; no video |
| AR | ARCore; automatic; center-pixel raycast; depth-preferred |
| Backend | Firebase Anonymous Auth + Firebase Storage |
| Retry | WorkManager |
| Local DB | Room (if/as needed) |
| Serialization | kotlinx.serialization |
| Packaging | ZIP |
| Code quality | Production-grade; no pseudocode; tests required |
| Modules | Per specs/architecture_preferences.md (refinement requires explicit justification) |

**Minimum SDK:** ARCore requires API 24+; CameraX requires API 21+; effective min SDK is **API 24**. This is derivable from library requirements and is recorded here for confirmation (see Ambiguity A-3).

---

## 9. Open Questions and Ambiguities

The following ambiguities were identified during spec analysis. No assumptions have been made. Each requires owner approval before Phase 2 begins.

| ID | Location | Ambiguity | Options |
|----|----------|-----------|---------|
| **A-1** | Initial Selection Screen | The options on Screen 2 are not named or counted. The spec says "display multiple selectable options" and stores the value as `initialSelection`. | (a) Provide the list of option labels and their stored values. (b) Confirm they are intentionally generic/configurable at build time. |
| **A-2** | Metadata Form Dropdown | "Option A" and "Option B" are placeholders. The real label strings and their stored values are not specified. | Provide the actual option labels and the string value each maps to in `details.json`. |
| **A-3** | Min SDK | Not explicitly stated. Derived as API 24 from ARCore docs. | Confirm API 24, or specify a higher floor if the target device fleet requires it. |
| **A-4** | ARCore translation threshold | The spec says "once camera translation exceeds configured threshold distance" but does not give the value (e.g., 0.1 m, 0.3 m). | Specify the default threshold in meters. Confirm whether it should be user-configurable or hardcoded. |
| **A-5** | Firebase Storage path | Spec defers path to architecture spec. | Provide the desired path template (e.g., `scans/{uid}/{filename}`, `uploads/{filename}`) or confirm it can be decided in Phase 3. |
| **A-6** | Back-navigation between screens | Not specified. Allowing back-navigation from Form → Selection or Scan → Form could corrupt partial scan state. | Confirm: (a) no back-navigation allowed after scan begins, or (b) back is allowed with state-reset behavior defined. |
| **A-7** | Post-upload screen | The spec describes upload behavior and feedback but does not define a dedicated result screen or whether the app resets to start a new scan. | Confirm: (a) show a result/success screen with option to start new scan, (b) auto-reset to Initial Selection, or (c) other. |
| **A-8** | Scan termination trigger | The spec says "on completion, stop acquisition cleanly" but does not define what triggers completion: (a) user taps Stop, (b) a fixed frame count, (c) a time limit, or (d) some combination. | Specify the scan stop mechanism. |

---

## 10. Data Overview (Summary)

Full contracts are deferred to Phase 4. Summary for PRD context:

**details.json keys:** `initialSelection`, `dropdownSelection`, `size` (int), `detail` (string ≤16 alphanumeric), `gt` (float[] or null), `scanTimestamp`, `deviceInfo`, `appVersion`

**measurements.json keys:** `pointA` (x, y, z, timestamp), `pointB` (x, y, z, timestamp), `distanceMeters`, `cameraPoseA`, `cameraPoseB`, `trackingState`, `hitType`

**ZIP structure:**
```
{UUID}_{timestamp}.zip
├── frames/
│   ├── frame_000001.jpg
│   ├── frame_000002.jpg
│   └── ...
├── details.json
└── measurements.json
```

---

## 11. Success Criteria for v1.0 Acceptance

1. Complete scan session produces a valid, openable ZIP containing all three required contents.
2. `measurements.json` is non-null and contains valid metric distance in every scan where ARCore tracking succeeded.
3. Failed uploads are retried by WorkManager without user re-triggering.
4. App does not crash or produce OOM errors on reference device(s) during a full scan session.
5. All unit, ViewModel, repository, serialization, ZIP packaging, and validation tests pass.
6. Firebase Storage rules validated against anonymous-auth upload flow.

---

*End of Phase 1 — PRD*  
*Awaiting approval to proceed to Phase 2: docs/functional_spec.md*
