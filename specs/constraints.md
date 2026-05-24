# Non-Negotiable Constraints

## Platform
- Android native only
- Kotlin only
- min SDK must support ARCore + CameraX

## UI
- Jetpack Compose only

## Architecture
- MVVM
- Clean Architecture
- Hilt dependency injection

## Camera
- CameraX
- exact 5 FPS target
- 2K resolution target (2560x1440 preferred)
- fallback resolution strategy required
- capture image frames, NOT video
- JPEG acceptable
- fixed focus if feasible
- fixed exposure if feasible
- avoid OOM

## AR
- ARCore
- automatic measurement capture
- no manual tapping
- center-pixel raycast
- depth-preferred fallback strategy required

## Backend
- Firebase Anonymous Auth
- Firebase Storage
- WorkManager retry uploads

## Persistence
- Room if needed
- app-specific storage for failed uploads

## Serialization
- kotlinx.serialization JSON

## Packaging
- ZIP archive output

## Quality
- production-grade code only
- no pseudocode
- tests required