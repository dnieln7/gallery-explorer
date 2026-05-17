# Data Model: TikTok-Style Video Scroll with First-Frame Preview

## Overview

This feature introduces **no new persistent entities** and **no changes to `VideoViewerState`**. Thumbnail data is managed entirely by Coil's memory cache, not by application state.

---

## Existing Entities (unchanged)

### `VideoViewerState`
*`feature/viewer/domain/model/VideoViewerState.kt`*

```
VideoViewerState
├── activePage: Int          — index of settled pager page
├── isPlaying: Boolean       — player is actively playing
├── showControls: Boolean    — controls overlay visible
├── isScrubbing: Boolean     — user is dragging progress slider
├── scrubSliderValue: Float  — normalised [0,1] scrub position
├── durationTotalMs: Long    — total video duration (ms)
├── durationCurrentMs: Long  — playback position (ms), scrub-aware
├── durationSlider: Float    — normalised [0,1], scrub-aware
└── isVideoReady: Boolean    — true once onRenderedFirstFrame fires
```

No new fields are added. Thumbnail images are not stored in state.

---

## Ephemeral / Cache-Level Entities

### `VideoFirstFrame` (Coil cache entry, not a Kotlin class)

| Attribute        | Description |
|------------------|-------------|
| Cache key        | `Uri` string of the video file (e.g., `file:///storage/emulated/0/Movies/clip-2.mp4`) |
| Value            | Decoded `Bitmap` of the first video frame |
| Cache layer      | Coil `MemoryCache` (LRU, app-scoped) |
| Lifetime         | Until evicted by LRU pressure or app process death |
| Load trigger     | `ImageLoader.enqueue()` called by `VideoViewerViewModel` when active page changes |
| Render consumer  | `AsyncImage` in `VideoSurface` for inactive pages and for active page when `!isVideoReady` |

---

## Updated Composable Contract

### `VideoSurface` (signature change)
*`feature/viewer/presentation/component/VideoSurface.kt`*

**Before**:
```kotlin
fun VideoSurface(
    modifier: Modifier,
    player: ExoPlayer,
    isActive: Boolean,
    isVideoReady: Boolean,
    onTap: () -> Unit,
)
```

**After**:
```kotlin
fun VideoSurface(
    modifier: Modifier,
    player: ExoPlayer,
    isActive: Boolean,
    isVideoReady: Boolean,
    videoFile: File,          // NEW — used to load the first-frame thumbnail via Coil
    onTap: () -> Unit,
)
```

**Rendering rules**:

| Condition | Render |
|-----------|--------|
| `isActive && isVideoReady` | `PlayerView` only (ExoPlayer surface) |
| `isActive && !isVideoReady` | `PlayerView` + `AsyncImage` overlay (thumbnail for current video) |
| `!isActive` | `AsyncImage` (thumbnail for this page's video) |

---

## Dependency Injection Delta

### `SingletonModule` (new binding)
*`app/src/main/java/xyz/dnieln7/galleryex/di/SingletonModule.kt`*

```
ImageLoader (Singleton)
└── provided via context.imageLoader (Coil singleton)
```

### `VideoViewerViewModel` (new constructor parameters)
*`feature/viewer/presentation/screen/VideoViewerViewModel.kt`*

```
VideoViewerViewModel
├── player: ExoPlayer           (existing)
├── imageLoader: ImageLoader    (NEW — injected singleton)
└── context: Context            (NEW — @ApplicationContext, for ImageRequest.Builder)
```
