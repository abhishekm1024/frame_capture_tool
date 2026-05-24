# Code

```text
# Android SfM Scanning App — Spec-Driven Development Master Prompt

I want you to act as a senior Android architect and implement this project using **spec-driven development**.

Do NOT jump directly into coding.

Follow this exact workflow:

1. First generate:
   - Product Requirements Document (PRD)
   - Functional specification
   - Technical architecture specification
   - Data model specification
   - API/storage specification
   - UI/UX screen specification
   - Error handling specification
   - Testing specification
   - Build/deployment specification
   - Security/privacy specification

2. After I approve specs, generate implementation incrementally module by module.

3. Tech stack constraints:
   - Android native
   - Kotlin
   - Jetpack Compose UI
   - MVVM + Clean Architecture
   - Dependency injection with Hilt
   - CameraX for camera pipeline
   - ARCore for AR measurements
   - Firebase Anonymous Auth
   - Firebase Storage for uploads
   - Kotlin coroutines + Flow
   - Room for local metadata persistence if needed
   - WorkManager for retry uploads
   - kotlinx.serialization for JSON
   - ZIP archive generation
   - min SDK should be chosen appropriately for ARCore + CameraX support

4. Produce production-grade code only.
   No pseudo-code unless explicitly requested.

---

## APP OVERVIEW

Build an Android app for structured object scanning for later Structure-from-Motion reconstruction using COLMAP.

The app captures:
- 2K RGB image frames at 5 FPS
- ARCore metric reference measurements
- user metadata
- packages everything into ZIP
- uploads to Firebase Storage

The app must be modular, production-ready, and spec-driven.

---

## APP FLOW

### Screen 1 — Splash Screen

Requirements:
- animated logo
- shown on app launch
- auto transition after animation completes
- no user interaction required

---

### Screen 2 — Initial Selection Screen

Requirements:
- display multiple selectable options
- exactly one option must be selected
- selection required before proceeding

Behavior:
- after selecting one option, proceed to next screen

Store selected value for packaging later.

---

### Screen 3 — Metadata Form Screen

Requirements:

#### Dropdown
Single dropdown with exactly 2 options:
- option A
- option B

Selection mandatory.

---

#### Input 1
Field name:
`size`

Constraints:
- integer only
- required
- positive values only

---

#### Input 2
Field name:
`detail`

Constraints:
- alphanumeric only
- max length 16
- required

---

#### Input 3
Field name:
`gt`

Constraints:
- optional
- comma-separated float list if provided
- format:

```text
12.3, 15.0, 0.42
```

Parsing rules:
- if provided, all values must parse as float
- if empty / no input, store value as `None`

---

#### Proceed Button
Disabled until:
- dropdown selected
- size valid
- detail valid
- gt valid if provided

Then activates:
`Proceed to Scan`

---

## SCAN SCREEN

### Camera Requirements

Capture:
- 2K resolution
- 5 FPS exact target
- raw frames stored individually
- no video compression pipeline
- save actual image frames

Recommended resolution target:
2560x1440 if device supports
fallback strategy required

Use CameraX.

---

### Flash Control

User toggle:
- OFF
- MAX INTENSITY ON

Requirements:
- torch mode
- maintain constant torch if enabled during scan

---

### ARCore Measurement Pipeline

Use ARCore.

Goal:
collect automatic metric constraints for later scaling COLMAP reconstruction.

Mechanism:

1. User is instructed to begin scan facing object center.
2. App captures first AR measurement point:
   - center pixel raycast
   - obtain world-space 3D point from ARCore
   - preferably using depth-based hit testing
3. During scan:
   continuously monitor camera pose.
4. Once camera translation exceeds configured threshold distance:
   capture second AR measurement point:
   - again using center pixel raycast
   - obtain second world-space 3D point
5. Compute:
   - metric distance between points

Store:
- point A XYZ
- point B XYZ
- metric distance
- timestamps
- AR tracking state
- hit confidence/type
- camera poses at capture moments

Do NOT require manual tapping.

This must be automatic.

---

### Scan Completion

On completion:
stop acquisition cleanly.

---

## DATA PACKAGING

Create ZIP archive containing:

### 1. Raw Frames

Folder:
`frames/`

Contents:
individual captured images

Naming:
```text
frame_000001.jpg
frame_000002.jpg
...
```

Store timestamps.

JPEG acceptable.

Need photogrammetry-friendly settings:
- fixed focus if possible
- fixed exposure if feasible
- minimal processing

---

### 2. details.json

Filename:
`details.json`

Contents:

```json
{
  "initialSelection": "...",
  "dropdownSelection": "...",
  "size": 123,
  "detail": "ABC123",
  "gt": [1.2, 3.4, 5.6],
  "scanTimestamp": "...",
  "deviceInfo": "...",
  "appVersion": "..."
}
```

If `gt` is omitted:

```json
{
  "gt": null
}
```

---

### 3. measurements.json

Filename:
`measurements.json`

Contents example:

```json
{
  "pointA": {
    "x": 0.0,
    "y": 0.0,
    "z": 0.0,
    "timestamp": 123456
  },
  "pointB": {
    "x": 1.0,
    "y": 0.0,
    "z": 0.0,
    "timestamp": 123999
  },
  "distanceMeters": 1.0,
  "cameraPoseA": {},
  "cameraPoseB": {},
  "trackingState": "...",
  "hitType": "DEPTH"
}
```

---

## FILE NAMING

ZIP filename format:

```text
UUID_timestamp.zip
```

Example:

```text
550e8400-e29b-41d4-a716-446655440000_1712345678.zip
```

---

## CLOUD STORAGE

Use:
Firebase Anonymous Authentication

Flow:
- anonymous login at upload time if needed
- upload ZIP to Firebase Storage

Upload path should be specified in architecture spec.

---

## FAILURE HANDLING

If upload fails:
- store ZIP locally
- use app-specific storage
- preserve same UUID_timestamp.zip filename
- persist upload retry metadata
- use WorkManager retry strategy

---

## PERMISSIONS

Handle:
- camera
- storage if needed
- internet
- ARCore requirements

Runtime permission UX required.

---

## ARCHITECTURE REQUIREMENTS

Use clean modular architecture.

Suggested modules:
- app
- core-ui
- core-common
- core-storage
- feature-splash
- feature-selection
- feature-form
- feature-scan
- feature-upload
- data-camera
- data-ar
- data-firebase

Refine if needed.

Layers:
- presentation
- domain
- data

---

## CAMERA REQUIREMENTS

Need discussion/specification for:
- CameraX image analysis
- exact FPS throttling
- frame dropping policy
- resolution negotiation
- JPEG encoding strategy
- memory pressure management

Must avoid OOM.

---

## AR REQUIREMENTS

Need discussion/specification for:
- ARCore session lifecycle
- depth availability fallback
- unsupported device behavior
- pose confidence handling
- raycast strategy
- tracking loss recovery

---

## UX REQUIREMENTS

Need scan guidance:
- instruct user to center object
- indicate scan progress
- show flash state
- loading/upload state
- upload success/failure feedback

---

## TESTING REQUIREMENTS

Include:
- unit tests
- ViewModel tests
- repository tests
- JSON serialization tests
- ZIP packaging tests
- validation tests
- instrumentation tests
- camera integration strategy
- AR abstraction testability

---

## SECURITY

Need explicit handling for:
- anonymous auth risks
- local file protection
- Firebase rules
- metadata privacy
- crash-safe persistence

---

## DELIVERABLE ORDER

Deliver in this order:

Phase 1:
PRD

Phase 2:
functional spec

Phase 3:
technical architecture

Phase 4:
data contracts

Phase 5:
screen specs

Phase 6:
implementation plan

Then coding.

Do NOT skip directly to implementation.

Ask clarifying questions only if absolutely necessary.

