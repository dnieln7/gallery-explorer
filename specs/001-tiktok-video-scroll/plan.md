# Implementation Plan: TikTok-Style Video Scroll with First-Frame Preview

**Branch**: `001-tiktok-video-scroll` | **Date**: 2026-05-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-tiktok-video-scroll/spec.md`

## Summary

When a user scrolls vertically between videos in `VideoViewerScreen`, adjacent pages currently show only a `CircularProgressIndicator`. This plan replaces that spinner with the first decoded frame of the adjacent video, loaded proactively via Coil's `VideoFrameDecoder`. No new screens, entities, or third-party libraries are required — all dependencies are already present in the project.

## Technical Context

**Language/Version**: Kotlin 2.0+

**Primary Dependencies**:
- Jetpack Compose (Material 3) — UI
- Voyager 1.1.0-beta03 (Hilt integration) — navigation
- Media3 ExoPlayer — active video playback (unchanged)
- Coil 2.7.0 (`coil-compose` + `coil-video`) — first-frame thumbnail extraction and caching (already present)
- Hilt — dependency injection

**Storage**: N/A — thumbnails are held in Coil's in-process `MemoryCache`; no disk writes

**Testing**: JUnit 4 · Kluent · MockK (relaxed) · Turbine · Coroutines Test · Robolectric (already used in `VideoViewerViewModelTest`)

**Target Platform**: Android

**Project Type**: Mobile app (single Gradle module: `app`)

**Performance Goals**: Preview visible before scroll begins; target ≤ 16ms render latency from cache (Coil memory hit)

**Constraints**:
- `isVideoReady` MUST remain gated on `onRenderedFirstFrame()` — constitution rule
- No new third-party libraries; Coil and ExoPlayer already declared in `gradle/libs.versions.toml`
- Zero new Detekt/KTLint violations; `baseline.xml` MUST NOT be modified

## Constitution Check

| Gate | Status | Notes |
|------|--------|-------|
| Detekt + KTLint: zero new violations | Pass | No structural changes; new code follows existing conventions |
| JVM unit tests pass; GIVEN/WHEN/THEN backtick names | Pass | New tests added in `VideoViewerViewModelTest` |
| KDoc on all new public classes/functions | Pass | `VideoSurface` and `VideoViewerViewModel` updated; KDoc required |
| MVI contract: ScreenDestination + private Screen + domain/model | Pass | No new screen; existing split unchanged |
| State/Action/Event in `[feature]/domain/model` | Pass | No new MVI files required |
| New dependencies in `libs.versions.toml` | Pass | No new dependencies; Coil already declared |
| Video loading gated on `onRenderedFirstFrame` | Pass | Rule is unchanged; thumbnail overlay is removed only after this callback |

## Project Structure

### Documentation (this feature)

```text
specs/001-tiktok-video-scroll/
├── plan.md          ← this file
├── research.md      ← Phase 0 output
├── data-model.md    ← Phase 1 output
└── tasks.md         ← Phase 2 output (created by /speckit-tasks)
```

### Source Code (modified files only)

```text
app/src/main/java/xyz/dnieln7/galleryex/
├── di/
│   └── SingletonModule.kt                         ← add ImageLoader @Singleton provision
└── feature/viewer/
    ├── domain/model/
    │   ├── VideoViewerAction.kt                   ← no changes
    │   ├── VideoViewerEvent.kt                    ← no changes
    │   └── VideoViewerState.kt                    ← no changes
    └── presentation/
        ├── component/
        │   └── VideoSurface.kt                    ← add videoFile param; replace spinner with AsyncImage
        └── screen/
            ├── VideoViewerScreen.kt               ← pass videos[index].file to VideoSurface
            └── VideoViewerViewModel.kt            ← inject ImageLoader + Context; add preloadAdjacentThumbnails()

app/src/test/java/xyz/dnieln7/galleryex/feature/viewer/presentation/screen/
└── VideoViewerViewModelTest.kt                    ← add preloading tests; update constructor call
```

**Structure Decision**: Single-module Android app; all changes are within the existing `feature/viewer` package. No new packages or files are created.

---

## Implementation Details

### 1. `SingletonModule.kt` — provide `ImageLoader`

Add a `@Singleton` binding that returns Coil's own singleton `ImageLoader`. Coil's `Context.imageLoader` extension property returns the lazily-created singleton built by `ImageLoader.Builder` during app startup.

```kotlin
@Provides
@Singleton
fun provideImageLoader(
    @ApplicationContext context: Context,
): ImageLoader {
    return context.imageLoader
}
```

---

### 2. `VideoViewerViewModel.kt` — inject ImageLoader, add proactive preloading

**Constructor changes** (new parameters added; existing `player` parameter unchanged):

```kotlin
@HiltViewModel
class VideoViewerViewModel @Inject constructor(
    val player: ExoPlayer,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context,
) : ViewModel()
```

**New private function** (`preloadAdjacentThumbnails`) called at the end of `loadVideo()`:

```kotlin
private fun preloadAdjacentThumbnails(index: Int) {
    listOfNotNull(
        videos.getOrNull(index - 1),
        videos.getOrNull(index + 1),
    ).forEach { video ->
        val request = ImageRequest.Builder(context)
            .data(video.file.toUri())
            .build()
        imageLoader.enqueue(request)
    }
}
```

Call site in `loadVideo()` — add at the end:
```kotlin
preloadAdjacentThumbnails(index)
```

**Note**: `preloadAdjacentThumbnails` is a private function with real domain logic (boundary checks, filtering nulls, building requests for each neighbour) — not a simple delegation wrapper. This is constitution-compliant.

---

### 3. `VideoSurface.kt` — replace spinner with AsyncImage

**Signature change**: add `videoFile: File` before `onTap`.

**Rendering logic change** (see data-model.md for the full rule table):

- `!isActive` path: replace `CircularProgressIndicator` with:
  ```kotlin
  AsyncImage(
      modifier = Modifier.fillMaxSize(),
      model = videoFile.toUri(),
      contentDescription = null,
      contentScale = ContentScale.Fit,
  )
  ```
- `isActive && !isVideoReady` path: replace the black-box + `CircularProgressIndicator` overlay with the same `AsyncImage` (current video's cached thumbnail covers the `PlayerView` until the first real frame renders).

The `Box` outer background (`MaterialTheme.colorScheme.scrim`) is retained and serves as the visible background for pages before the thumbnail loads.

---

### 4. `VideoViewerScreen.kt` — pass `videoFile` to `VideoSurface`

Inside the `VerticalPager { index -> }` block:

```kotlin
VideoSurface(
    modifier = Modifier.fillMaxSize(),
    player = player,
    isActive = activePage == index,
    isVideoReady = uiState.isVideoReady,
    videoFile = videos[index].file,   // NEW
    onTap = { onAction(VideoViewerAction.OnTap) },
)
```

---

### 5. `VideoViewerViewModelTest.kt` — new test cases

Update constructor:
```kotlin
private val imageLoader = relaxedMockk<ImageLoader>()
private val context = relaxedMockk<Context>()
// ...
viewModel = VideoViewerViewModel(player, imageLoader, context)
```

New test cases (all follow GIVEN/WHEN/THEN backtick naming, use Turbine for state):

1. `GIVEN videos and a selected index WHEN Initialize is called THEN adjacent thumbnails are preloaded via imageLoader`
   - Verify `imageLoader.enqueue(any())` called exactly 2 times (for index-1 and index+1 when applicable)

2. `GIVEN initialized viewmodel WHEN OnPageChange is called with a new page THEN adjacent thumbnails for the new page are preloaded`

3. `GIVEN first video is selected WHEN Initialize is called THEN only the next thumbnail is preloaded (no out-of-bounds access)`
   - Verify `imageLoader.enqueue(any())` called exactly 1 time

4. `GIVEN last video is selected WHEN Initialize is called THEN only the previous thumbnail is preloaded`
   - Verify `imageLoader.enqueue(any())` called exactly 1 time

---

## Verification

1. **Build**: `./gradlew :app:assembleDebug` — zero errors
2. **Detekt**: `./gradlew :app:detekt` — zero new violations
3. **Tests**: `./gradlew :app:testDebugUnitTest` — all pass including 4 new preload tests
4. **Manual — mid-scroll preview**: Open a folder with 3+ videos → play first video → swipe down slowly → adjacent video's first frame must be visible on the incoming page before the pager settles
5. **Manual — smooth active transition**: Complete a swipe to the next video → no black screen or spinner must appear between the thumbnail and the playing video
6. **Manual — back-navigation**: Scroll to video 3, then scroll back to video 1 → thumbnails for video 2 must appear immediately (cached, no reload flash)
7. **Manual — single video folder**: Open a folder with only one video → no crash; no adjacent thumbnail load attempted
8. **Manual — corrupt video**: If a video file is unreadable, the `AsyncImage` must show the scrim background (Coil's error fallback), not crash
