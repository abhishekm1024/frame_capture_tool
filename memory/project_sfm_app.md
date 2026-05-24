---
name: project-sfm-app
description: Android SfM Scanning App — implementation progress, module structure, key decisions
metadata:
  type: project
---

Multi-module Android app (MVVM + Clean Architecture) for Structure-from-Motion (SfM) photogrammetry scanning.

## Implementation Status

**M0** (Project Setup): COMPLETE — all 11 modules scaffolded, version catalog in `gradle/libs.versions.toml`, minSdk=24, targetSdk=35.

**M1** (Core Modules): COMPLETE — implemented 2026-05-24.
- `core-common`: `Result<T>`, `AppDispatchers`, `Logger`/`AndroidLogger`, `AppDispatchersModule`, `LoggerModule`, `StringExt`, `CollectionExt`
- `core-storage`: `StorageConstants`, `AppFileProvider`, `SessionDirectoryManager`, `ZipBuilder`, `StorageModule`
- `core-ui`: `ScanAppTheme`, `PrimaryButton`, `LabeledTextField`, `LoadingOverlay`, `ErrorBanner` + full test coverage
- Modified `core/core-storage/build.gradle.kts` to add Hilt + KSP (was missing from M0 scaffold)

**M2** (data-camera): PENDING — depends on M1
**M3** (data-ar): PENDING — depends on M1
**M4** (data-firebase): PENDING — depends on M1, needs `google-services.json`
**M5** (feature-splash/selection/form): PENDING — blocked by TBD-A1, TBD-A2, Lottie asset
**M6** (feature-scan): PENDING
**M7** (feature-upload): PENDING
**M8** (app): PENDING
**M9** (Testing): PENDING

## Module Paths
- Core: `core/core-common`, `core/core-ui`, `core/core-storage`
- Data: `data/data-camera`, `data/data-ar`, `data/data-firebase`
- Features: `features/feature-splash`, `features/feature-selection`, `features/feature-form`, `features/feature-scan`, `features/feature-upload`
- App: `app/`

## Key Architecture Decisions (from docs)
- Package root: `com.sfm.scanner`
- `Result<T>` is project-defined sealed class (not Kotlin stdlib) — in `core.common`
- `ZipBuilder` is unscoped Hilt (one instance per injection) — mutable state per ZIP operation
- `SessionDirectoryManager.createSessionDir(uuid)` returns the **frames** subdirectory
- `StorageConstants.FRAME_FILENAME_FORMAT = "frame_%06d.jpg"`, `ZIP_FILENAME_FORMAT = "%s_%d.zip"`
- ZIP written atomically: temp file → `ZipOutputStream.close()` → rename
- Forbidden: `core-*` → `data-*`, `core-*` → `feature-*`, `data-*` → `feature-*`, cross-feature deps

## Unresolved Blockers (TBDs)
- A-1: `SelectionOption` instances/values (blocks M5-B)
- A-2: `dropdownSelection` valid values (blocks M5-C)
- A-4: AR translation threshold (default 0.30m) — needs owner confirmation
- A-5: Firebase Storage base path (default "scans") — needs owner confirmation
- Lottie animation asset: blocks M5-A (feature-splash)
- `google-services.json`: blocks M4 (data-firebase) and M0 completion

**Why:** Implementation is module-by-module bottom-up per docs/implementation_plan.md. Stop on ambiguity rule applies — M5 features are blocked until TBDs resolved.
**How to apply:** Before starting any M2+ milestone, check which TBDs affect it and whether they are resolved.
