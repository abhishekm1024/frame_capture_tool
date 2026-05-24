# Screen Specifications
## Android SfM Scanning App

**Version:** 1.0  
**Status:** Draft — Awaiting Phase 5 Approval  
**Date:** 2026-05-24  
**Depends on:** docs/functional_spec.md, docs/data_contracts.md  
**Derived from:** specs/master_spec.md

---

## 0. Conventions

| Notation | Meaning |
|----------|---------|
| `[TBD-Ax]` | Unresolved ambiguity; pending owner input |
| `theme.colorScheme.*` | Reference to MaterialTheme token; not a hardcoded color |
| `theme.typography.*` | Reference to MaterialTheme typography token |
| `SP-nn` | Screen spec requirement ID |
| `[copy]` | Exact UI string literal |

All screens use Jetpack Compose. All dimensions are expressed as `dp` unless noted. All text sizes are expressed as `sp`.

---

## 1. Global Conventions

### 1.1 Theme Tokens Referenced

| Token | Role |
|-------|------|
| `colorScheme.background` | Screen background |
| `colorScheme.surface` | Card / panel backgrounds |
| `colorScheme.primary` | Primary action buttons, active selection highlight |
| `colorScheme.onPrimary` | Text/icon on primary color |
| `colorScheme.error` | Inline field error text and icons |
| `colorScheme.onBackground` | Body text |
| `colorScheme.onSurface` | Secondary text |
| `colorScheme.outline` | Inactive field borders, dividers |
| `typography.headlineMedium` | Screen titles |
| `typography.bodyLarge` | Body / field labels |
| `typography.bodyMedium` | Secondary text, hints |
| `typography.labelLarge` | Button text |
| `typography.labelSmall` | Field error messages, captions |

Exact color values are defined in `core-ui` theme setup (Phase 6). No hex values are specified here.

### 1.2 Edge-to-Edge

All screens use `WindowCompat.setDecorFitsSystemWindows(window, false)`. System bar insets are consumed via `Modifier.systemBarsPadding()` or equivalent.

### 1.3 Navigation Animations

| Transition | Animation |
|------------|-----------|
| Any forward navigation | Slide in from right (standard Compose Navigation transition) |
| SplashScreen → SelectionScreen | Fade (no slide; splash is not part of normal back stack) |
| PackagingScreen → UploadScreen | Fade (transient screen exit) |

---

## 2. SplashScreen

### 2.1 Purpose
Brand moment on cold launch; leads automatically to SelectionScreen.

### 2.2 Layout

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│          ┌─────────────┐            │
│          │             │            │
│          │  APP LOGO   │            │
│          │  (animated) │            │
│          │             │            │
│          └─────────────┘            │
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

### 2.3 Components

| ID | Component | Spec |
|----|-----------|------|
| SP-SPLASH-01 | Screen background | `colorScheme.background`; full-screen |
| SP-SPLASH-02 | Logo container | Centered horizontally and vertically; size 160×160 dp |
| SP-SPLASH-03 | Logo animation | Lottie animation asset (asset path TBD Phase 6); plays once; `repeatCount = 0` |

### 2.4 Behavior

| Event | Response |
|-------|---------|
| Animation completes (`onAnimationEnd`) | Navigate → SelectionScreen (`popUpTo("splash") { inclusive = true }`) |
| System back pressed | No-op |
| Screen tap | No-op |

### 2.5 Accessibility
- Logo container has `contentDescription = "App logo"`.
- Screen is non-interactive; no focus management required.

---

## 3. SelectionScreen

### 3.1 Purpose
User selects exactly one item from a list to identify the object type being scanned.

### 3.2 Layout

```
┌─────────────────────────────────────┐
│  ← (back to Splash disabled)        │  ← system bar
├─────────────────────────────────────┤
│                                     │
│  [Screen Title]              24 sp  │  ← headlineMedium, paddingTop 32dp
│                                     │
│  ┌─────────────────────────────┐    │
│  │  Option A          ◉ / ○   │    │  ← SelectionOptionCard
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │  Option B          ◉ / ○   │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │  Option C          ◉ / ○   │    │
│  └─────────────────────────────┘    │
│    [TBD-A1: additional options]      │
│                                     │
│                           ┌───────┐ │
│                           │ Next  │ │  ← PrimaryButton; disabled if no selection
│                           └───────┘ │
└─────────────────────────────────────┘
```

### 3.3 Strings

| Key | Copy | Notes |
|-----|------|-------|
| `screen_title` | `[TBD-A1]` | Screen heading label |
| `button_next` | `"Continue"` | Primary action button |
| `option_labels` | `[TBD-A1]` | One label per `SelectionOption.displayLabel` |

### 3.4 Components

| ID | Component | Spec |
|----|-----------|------|
| SP-SEL-01 | Screen title | `typography.headlineMedium`; `colorScheme.onBackground`; horizontal padding 24 dp; top padding 32 dp |
| SP-SEL-02 | Option list | `LazyColumn` (even if count is small; safe for variable list); vertical padding 16 dp; item spacing 8 dp |
| SP-SEL-03 | `SelectionOptionCard` | `Card` with `shape = RoundedCornerShape(12.dp)`; `colorScheme.surface` background; full width minus 24 dp horizontal padding; height 64 dp |
| SP-SEL-04 | Option label inside card | `typography.bodyLarge`; `colorScheme.onSurface`; start padding 16 dp |
| SP-SEL-05 | Selection indicator | `RadioButton` trailing; selected color `colorScheme.primary`; entire card is tappable (not just the radio button) |
| SP-SEL-06 | Selected card border | `BorderStroke(2.dp, colorScheme.primary)` when selected; `BorderStroke(1.dp, colorScheme.outline)` when unselected |
| SP-SEL-07 | Continue button | `PrimaryButton` (from `core-ui`); full width minus 24 dp padding; bottom 24 dp; `enabled = selectedOption != null` |

### 3.5 States

| State | Visual |
|-------|--------|
| No selection | All cards show outline border; Continue button disabled (alpha 0.38) |
| Option selected | Selected card shows primary border + filled radio; Continue enabled |

### 3.6 Behavior

| Event | Response |
|-------|---------|
| Tap option card | Update `selectedOption`; deselect previous |
| Tap Continue (enabled) | Navigate → FormScreen with `selectedOption.id` |
| System back | Navigate back to Splash (back stack behavior; Splash is popped so app exits) |

### 3.7 Accessibility
- Each `SelectionOptionCard` has `role = Role.RadioButton`.
- `contentDescription` = `"${option.displayLabel}, ${if selected "selected" else "not selected"}"`.
- Continue button `contentDescription` = `"Continue to metadata form"`.

---

## 4. FormScreen

### 4.1 Purpose
User enters scan metadata before proceeding to the camera.

### 4.2 Layout

```
┌─────────────────────────────────────┐
│  ←                                  │  ← back arrow to SelectionScreen
├─────────────────────────────────────┤
│                                     │
│  Scan Details                       │  ← headlineMedium, pad 24dp
│                                     │
│  Type                               │  ← field label, bodyMedium
│  ┌─────────────────────────────┐    │
│  │ Select type...          ▼  │    │  ← ExposedDropdownMenu
│  └─────────────────────────────┘    │
│                                     │
│  Size                               │
│  ┌─────────────────────────────┐    │
│  │ Enter size...               │    │  ← OutlinedTextField
│  └─────────────────────────────┘    │
│  ⚠ Must be a positive integer       │  ← error text (conditional)
│                                     │
│  Detail                             │
│  ┌─────────────────────────────┐    │
│  │ Enter detail...             │    │
│  └─────────────────────────────┘    │
│  ⚠ Alphanumeric only, max 16 chars  │  ← error text (conditional)
│                                     │
│  Ground Truth (optional)            │
│  ┌─────────────────────────────┐    │
│  │ e.g. 12.3, 15.0, 0.42      │    │
│  └─────────────────────────────┘    │
│  ⚠ Enter comma-separated numbers    │  ← error text (conditional)
│                                     │
│  ┌─────────────────────────────┐    │
│  │      Proceed to Scan        │    │  ← PrimaryButton
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### 4.3 Strings

| Key | Copy |
|-----|------|
| `screen_title` | `"Scan Details"` |
| `label_dropdown` | `"Type"` |
| `placeholder_dropdown` | `"Select type..."` |
| `option_a_label` | `[TBD-A2]` |
| `option_b_label` | `[TBD-A2]` |
| `label_size` | `"Size"` |
| `placeholder_size` | `"Enter size..."` |
| `error_size` | `"Must be a positive integer"` |
| `label_detail` | `"Detail"` |
| `placeholder_detail` | `"Enter detail..."` |
| `error_detail` | `"Alphanumeric only, max 16 characters"` |
| `label_gt` | `"Ground Truth (optional)"` |
| `placeholder_gt` | `"e.g. 12.3, 15.0, 0.42"` |
| `error_gt` | `"Enter comma-separated numbers (e.g. 1.0, 2.5)"` |
| `button_proceed` | `"Proceed to Scan"` |

### 4.4 Components

| ID | Component | Spec |
|----|-----------|------|
| SP-FORM-01 | Back arrow | `TopAppBar` leading icon; navigates to SelectionScreen |
| SP-FORM-02 | Screen title | `typography.headlineMedium`; `TopAppBar` title |
| SP-FORM-03 | Scroll container | `Column` inside `verticalScroll` — handles small screens and soft keyboard push-up |
| SP-FORM-04 | Dropdown | `ExposedDropdownMenuBox` + `OutlinedTextField` (read-only trigger) + `ExposedDropdownMenu`; full width; 2 items `[TBD-A2]` |
| SP-FORM-05 | `size` field | `OutlinedTextField`; `keyboardType = KeyboardType.Number`; single line |
| SP-FORM-06 | `detail` field | `OutlinedTextField`; `keyboardType = KeyboardType.Ascii`; single line; `maxLength` enforced via `VisualTransformation` or input filter |
| SP-FORM-07 | `gt` field | `OutlinedTextField`; `keyboardType = KeyboardType.Ascii`; single line; hint text shown when empty |
| SP-FORM-08 | Inline error text | `typography.labelSmall`; `colorScheme.error`; shown below field when error is non-null; prefixed with `⚠` character |
| SP-FORM-09 | Proceed button | `PrimaryButton`; full width; `enabled = proceedEnabled`; bottom padding 24 dp |
| SP-FORM-10 | Field spacing | 16 dp vertical gap between fields |
| SP-FORM-11 | Horizontal padding | 24 dp on all sides |

### 4.5 Field Interaction Details

#### Dropdown
- Tapping the field opens a dropdown menu anchored below.
- Menu items show `[TBD-A2]` labels.
- Selected item text appears in the trigger field.
- No default selection; placeholder shown until user selects.

#### `size` field
- Errors evaluated on focus-lost (not on every keystroke while first editing, to avoid premature red state).
- Once a field has been touched and lost focus, validation runs live on every subsequent change.

#### `detail` field
- Character counter `"N / 16"` shown as trailing text inside the field when `detailInput.length > 10` (approaching limit).
- Error shown immediately if non-alphanumeric character is typed.

#### `gt` field
- Error shown on focus-lost only (comma-separated float parsing is disruptive if shown mid-entry).
- If field is empty when focus is lost: no error (optional field).

### 4.6 Keyboard Behavior
- `ImeAction.Next` on dropdown → moves focus to `size`.
- `ImeAction.Next` on `size` → moves focus to `detail`.
- `ImeAction.Next` on `detail` → moves focus to `gt`.
- `ImeAction.Done` on `gt` → dismisses keyboard; triggers `gt` validation.

### 4.7 Accessibility
- All `OutlinedTextField` components have `label` set (serves as both floating label and accessibility label).
- Error messages are associated with their field via `semantics { error(...) }`.
- Proceed button `contentDescription = "Proceed to scan screen"`.

---

## 5. ScanScreen

### 5.1 Purpose
Camera viewfinder with AR measurement pipeline and scan control.

### 5.2 Layout

```
┌─────────────────────────────────────┐  ← status bar (translucent)
│                                     │
│  ┌─ AR STATUS ──────────────────┐   │  ← AR status chip, top-start, 16dp margin
│  │  ● Tracking                  │   │
│  └──────────────────────────────┘   │
│                                     │
│                                     │
│                                     │
│         CAMERA PREVIEW              │  ← full-screen CameraX PreviewView
│         (full bleed)                │
│                                     │
│  ╔═══════════════════════════════╗  │
│  ║  Point camera at the center  ║  │  ← instruction overlay (READY state only)
│  ║  of the object to begin      ║  │
│  ╚═══════════════════════════════╝  │
│                                     │
│  ┌──────────────────────────────┐   │
│  │  Frames: 042    🔦  [  STOP ]│   │  ← bottom control bar
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘  ← nav bar (translucent)
```

**READY state** (before scan start):

```
┌─────────────────────────────────────┐
│                                     │
│  ┌─ AR STATUS ──────────────────┐   │
│  │  ○ Initializing...           │   │
│  └──────────────────────────────┘   │
│                                     │
│         CAMERA PREVIEW              │
│                                     │
│  ╔═══════════════════════════════╗  │
│  ║  Point camera at the center  ║  │
│  ║  of the object, then tap     ║  │
│  ║  Start Scan                  ║  │
│  ╚═══════════════════════════════╝  │
│                                     │
│  ┌──────────────────────────────┐   │
│  │  🔦          [  START SCAN ] │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 5.3 Strings

| Key | Copy |
|-----|------|
| `ar_state_initializing` | `"Initializing AR..."` |
| `ar_state_tracking` | `"Tracking"` |
| `ar_state_point_a` | `"Reference A captured"` |
| `ar_state_complete` | `"Measurement complete"` |
| `ar_state_lost` | `"Tracking lost"` |
| `ar_state_unsupported` | `"AR unavailable"` |
| `instruction_ready` | `"Point camera at the center of the object, then tap Start Scan"` |
| `instruction_scanning` | `"Slowly move around the object"` |
| `label_frames` | `"Frames: %d"` |
| `button_start` | `"Start Scan"` |
| `button_stop` | `"Stop"` |
| `torch_off_description` | `"Torch off"` |
| `torch_on_description` | `"Torch on"` |
| `error_camera_unavailable` | `"Camera unavailable. Please check permissions."` |
| `error_ar_unsupported` | `"This device does not support AR measurement. Scanning is unavailable."` |
| `error_permission_camera` | `"Camera permission is required to scan."` |
| `button_open_settings` | `"Open Settings"` |

### 5.4 Components

| ID | Component | Spec |
|----|-----------|------|
| SP-SCAN-01 | Camera preview | `AndroidView { PreviewView }` full-screen; `scaleType = FIT_CENTER`; sits behind all overlay components |
| SP-SCAN-02 | AR status chip | `Surface` with rounded corners (50% radius); `colorScheme.surface.copy(alpha=0.80)`; top-start 16 dp margin; `Row` with status dot + text |
| SP-SCAN-03 | AR status dot | 8×8 dp `Box`; color per state (see §5.5) |
| SP-SCAN-04 | AR status text | `typography.labelSmall`; `colorScheme.onSurface` |
| SP-SCAN-05 | Instruction banner | Semi-transparent `Surface` (`colorScheme.surface.copy(alpha=0.85)`); rounded corners 8 dp; center-horizontal; above bottom bar; padding 12 dp; hidden in INITIALIZING state |
| SP-SCAN-06 | Bottom control bar | Full-width `Row`; `colorScheme.surface.copy(alpha=0.90)`; height 64 dp; `Arrangement.SpaceBetween`; horizontal padding 16 dp; bottom system bar inset |
| SP-SCAN-07 | Frame counter | `typography.bodyMedium`; `colorScheme.onSurface`; start-aligned within bar; visible during SCANNING only |
| SP-SCAN-08 | Torch toggle | `IconButton` (flashlight icon); active state uses `colorScheme.primary`; inactive uses `colorScheme.onSurface`; always visible |
| SP-SCAN-09 | Start button | `PrimaryButton`; visible in READY state |
| SP-SCAN-10 | Stop button | Outlined button (`ButtonDefaults.outlinedButtonColors`); `colorScheme.error` border + text; visible during SCANNING state |
| SP-SCAN-11 | Permission error | Full-screen overlay `Column` with error message + "Open Settings" button; shown when camera permission denied |
| SP-SCAN-12 | AR unsupported error | Full-screen overlay with `error_ar_unsupported` message; non-dismissable |

### 5.5 AR Status Chip Colors

| State | Dot Color | Text |
|-------|-----------|------|
| `INITIALIZING` | `colorScheme.outline` (grey) | `ar_state_initializing` |
| `TRACKING` | Green (`Color(0xFF4CAF50)`) | `ar_state_tracking` |
| `POINT_A_CAPTURED` | `colorScheme.primary` | `ar_state_point_a` |
| `MEASUREMENT_COMPLETE` | Green | `ar_state_complete` |
| `TRACKING_LOST` | `colorScheme.error` | `ar_state_lost` |
| `UNSUPPORTED` | `colorScheme.error` | `ar_state_unsupported` |

### 5.6 Screen States

| `ScanUiState` | Camera Preview | Instruction | Frame Counter | Start Button | Stop Button |
|---------------|---------------|-------------|---------------|--------------|-------------|
| `Initializing` | Visible | Hidden | Hidden | Hidden | Hidden |
| `Ready` | Visible | Shown (`instruction_ready`) | Hidden | Visible | Hidden |
| `Scanning` | Visible | Shown (`instruction_scanning`) | Visible | Hidden | Visible |
| `Stopping` | Visible (frozen) | Hidden | Visible (last count) | Hidden | Disabled |
| `Complete` | Visible | Hidden | Visible (final count) | Hidden | Hidden |
| `Error` | Hidden | Hidden | Hidden | Hidden | Hidden; error overlay shown |

### 5.7 Torch Toggle Behavior

| Condition | Toggle Available |
|-----------|-----------------|
| `Initializing` | Yes (user can pre-set torch state) |
| `Ready` | Yes |
| `Scanning` | Yes (visual state change only; torch held constant per TD-15 if it was ON at scan start) |
| `Stopping` | No (disabled) |

> Note from functional_spec.md §5.4: if torch is ON when scan starts, it remains ON for the full scan. The toggle button during SCANNING reflects the current UI toggle state but does not change torch hardware if torch was locked ON. This behavior should be surfaced to the user (e.g., brief snackbar: "Torch locked on until scan ends") `[confirm behavior]`.

### 5.8 Accessibility
- Camera preview: `contentDescription = "Camera viewfinder"`.
- Torch toggle: `contentDescription` = `torch_off_description` or `torch_on_description` per current state.
- Start/Stop buttons have visible labels; no icon-only controls.
- AR status chip: `semantics { contentDescription = <ar state text> }`.

---

## 6. PackagingScreen

### 6.1 Purpose
Transient, non-interactive progress screen shown while ZIP is being built.

### 6.2 Layout

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│          ┌─────────────┐            │
│          │   (spinner) │            │
│          └─────────────┘            │
│                                     │
│      Packaging scan data...         │  ← bodyLarge, centered
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

### 6.3 Strings

| Key | Copy |
|-----|------|
| `packaging_label` | `"Packaging scan data..."` |

### 6.4 Components

| ID | Component | Spec |
|----|-----------|------|
| SP-PKG-01 | Background | `colorScheme.background`; full-screen |
| SP-PKG-02 | Spinner | `CircularProgressIndicator`; size 48 dp; `colorScheme.primary`; centered |
| SP-PKG-03 | Label | `typography.bodyLarge`; `colorScheme.onBackground`; 24 dp below spinner; centered |

### 6.5 Behavior
- No user interaction accepted.
- System back: no-op.
- Auto-navigates → UploadScreen when ZIP is ready.
- If packaging fails (e.g., I/O error): navigate to an error state on UploadScreen rather than a separate screen.

### 6.6 Accessibility
- `CircularProgressIndicator` has `contentDescription = "Packaging scan data, please wait"`.

---

## 7. UploadScreen

### 7.1 Purpose
Shows upload progress and final result; provides next-action control.

### 7.2 Layout — AUTHENTICATING / UPLOADING

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│          ┌─────────────┐            │
│          │   (spinner  │            │
│          │   or bar)   │            │
│          └─────────────┘            │
│                                     │
│      Uploading scan...              │  ← bodyLarge, centered
│      filename.zip                   │  ← labelSmall, onSurface, centered
│                                     │
│      ████████░░░░░░░  64%           │  ← LinearProgressIndicator (UPLOADING)
│                                     │
│                                     │
└─────────────────────────────────────┘
```

### 7.3 Layout — SUCCESS

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│          ┌─────────────┐            │
│          │      ✓      │            │  ← success icon, primary color
│          └─────────────┘            │
│                                     │
│      Upload complete                │  ← headlineMedium, centered
│                                     │
│      Your scan has been             │
│      uploaded successfully.         │  ← bodyMedium, onSurface, centered
│                                     │
│  ┌──────────────────────────────┐   │
│  │       Start New Scan         │   │  ← PrimaryButton
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 7.4 Layout — FAILED / RETRY QUEUED

```
┌─────────────────────────────────────┐
│                                     │
│          ┌─────────────┐            │
│          │      ✕      │            │  ← error icon, error color
│          └─────────────┘            │
│                                     │
│      Upload failed                  │  ← headlineMedium
│                                     │
│      Will retry automatically       │  ← bodyMedium, onSurface
│      when connected.                │
│                                     │
│  ┌──────────────────────────────┐   │
│  │       Start New Scan         │   │  ← PrimaryButton
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 7.5 Strings

| Key | Copy |
|-----|------|
| `authenticating_label` | `"Preparing upload..."` |
| `uploading_label` | `"Uploading scan..."` |
| `success_title` | `"Upload complete"` |
| `success_body` | `"Your scan has been uploaded successfully."` |
| `failed_title` | `"Upload failed"` |
| `failed_body` | `"Will retry automatically when connected."` |
| `retry_queued_note` | `"Upload has been queued and will retry automatically."` |
| `button_new_scan` | `"Start New Scan"` |
| `packaging_error_title` | `"Packaging failed"` |
| `packaging_error_body` | `"Could not create scan archive. Please try again."` |
| `button_retry_now` | `"Retry"` |

### 7.6 Components

| ID | Component | Spec |
|----|-----------|------|
| SP-UPLOAD-01 | Background | `colorScheme.background`; full-screen; center-aligned `Column` |
| SP-UPLOAD-02 | Status icon | 64×64 dp; spinner (`CircularProgressIndicator`) for AUTHENTICATING; `LinearProgressIndicator` for UPLOADING; checkmark icon for SUCCESS; ✕ icon for FAILED |
| SP-UPLOAD-03 | Progress bar | `LinearProgressIndicator` full width minus 48 dp padding; visible in UPLOADING state only; progress = `percent / 100f` |
| SP-UPLOAD-04 | Progress percent label | `typography.labelSmall`; trailing the progress bar; `"${percent}%"` |
| SP-UPLOAD-05 | Filename label | `typography.labelSmall`; `colorScheme.onSurface`; shown below main label; truncated with ellipsis if > 40 chars |
| SP-UPLOAD-06 | Title text | `typography.headlineMedium`; `colorScheme.onBackground`; shown in SUCCESS and FAILED states |
| SP-UPLOAD-07 | Body text | `typography.bodyMedium`; `colorScheme.onSurface`; shown in SUCCESS and FAILED states |
| SP-UPLOAD-08 | Start New Scan button | `PrimaryButton`; full width minus 24 dp padding; shown in SUCCESS and FAILED/RETRY_QUEUED states |
| SP-UPLOAD-09 | Retry now button | `PrimaryButton`; shown only in FAILED state as secondary action `[optional — confirm with owner]` |

### 7.7 Screen States

| `UploadUiState` | Icon | Progress Bar | Title | Body | Button |
|-----------------|------|-------------|-------|------|--------|
| `Authenticating` | Spinner | Hidden | `authenticating_label` | filename | Hidden |
| `Uploading(n)` | None | Visible, n% | `uploading_label` | filename + percent | Hidden |
| `Success` | ✓ green | Hidden | `success_title` | `success_body` | `button_new_scan` |
| `Failed` | ✕ red | Hidden | `failed_title` | `failed_body` | `button_new_scan` |
| `RetryQueued` | ✕ red | Hidden | `failed_title` | `retry_queued_note` | `button_new_scan` |

### 7.8 Navigation from UploadScreen

| Action | Destination |
|--------|------------|
| Tap "Start New Scan" (any terminal state) | `popUpTo("selection") { inclusive = false }` → SelectionScreen `[TBD-A7 default applied]` |
| System back | No-op (back disabled; upload in progress or complete) |

### 7.9 Accessibility
- Status icon: `contentDescription` set per state (e.g., `"Upload successful"`, `"Upload failed"`).
- Progress indicator: `semantics { progressBarRangeInfo = ProgressBarRangeInfo(current, 0f..1f) }`.
- "Start New Scan" button: `contentDescription = "Start a new scan session"`.

---

## 8. Screen Summary

| Screen | Back Allowed | User Actions | Auto-advance |
|--------|-------------|-------------|-------------|
| Splash | No | None | Yes (animation end) |
| Selection | Yes (exits app) | Select option, tap Continue | No |
| Form | Yes (→ Selection) | Fill fields, tap Proceed | No |
| Scan | No (system back blocked) | Torch toggle, Start, Stop | No |
| Packaging | No | None | Yes (ZIP ready) |
| Upload | No | Start New Scan | No |

---

## 9. Carried-Forward Ambiguities Affecting Screens

| ID | Screens Affected | Blocker? |
|----|-----------------|---------|
| A-1 | SelectionScreen — option labels, screen title | Screen renders with placeholders; final copy required before release |
| A-2 | FormScreen — dropdown option labels | Same as above |
| A-7 | UploadScreen — "Start New Scan" default applied | Confirm reset destination |

---

*End of Phase 5 — Screen Specifications*  
*Awaiting approval to proceed to Phase 6: docs/implementation_plan.md + docs/risk_register.md*
