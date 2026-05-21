# Research: TikTok-Style Video Scroll with First-Frame Preview

## Decision 1: Thumbnail extraction mechanism

**Decision**: Use the existing `coil-video` library (already a project dependency at `2.7.0`) to extract first-frame thumbnails from video files via `VideoFrameDecoder`.

**Rationale**: `coil-video` is already on the classpath. `AsyncImage(model = videoFile.toUri())` automatically routes through `VideoFrameDecoder`, which decodes the first frame of a video file as a `Bitmap`. No additional library is required. The decoded bitmap is stored in Coil's `LruCache`-backed memory cache, so subsequent renders of the same URI hit the cache instantly — satisfying FR-010 (session-level caching).

**Alternatives considered**:
- `MediaMetadataRetriever.getFrameAtTime()` — requires manual bitmap management, no caching, more boilerplate.
- `ThumbnailUtils.createVideoThumbnail()` — deprecated in API 29+, no caching.
- Custom decoder with ExoPlayer frame grab — unnecessary complexity given Coil already handles this.

---

## Decision 2: Thumbnail preloading strategy (FR-009)

**Decision**: Inject Coil's `ImageLoader` singleton and `@ApplicationContext Context` into `VideoViewerViewModel`. Call `imageLoader.enqueue(ImageRequest)` for the adjacent video files (index-1, index+1) inside `loadVideo()` each time the active page changes.

**Rationale**: Proactive enqueuing warms the Coil memory cache before the user starts scrolling. When `VideoSurface` then renders an `AsyncImage` for an inactive (adjacent) page, the bitmap is already in cache and appears without noticeable delay. This directly satisfies FR-009 (load before scroll starts) and SC-001 (visible within 500ms — in practice, near-instant from cache).

`ImageLoader` injection also keeps the preloading logic testable: the mock can verify `enqueue()` calls without touching the file system.

**Alternatives considered**:
- Preloading in the Composable via `LaunchedEffect` — harder to test, fires after recomposition (may miss the "before scroll" window).
- Storing `Bitmap`/`ImageBitmap` objects in `VideoViewerState` — breaks state immutability best practices, increases state size, complicates serialisation.

---

## Decision 3: VideoSurface inactive-page rendering

**Decision**: Replace the `CircularProgressIndicator` shown for inactive pages with `AsyncImage(model = videoFile.toUri(), contentScale = ContentScale.Fit)`. Extend `VideoSurface` with a `videoFile: File` parameter.

**Rationale**: The inactive-page path (`isActive = false`) currently shows only a spinner. Swapping in `AsyncImage` with the video URI gives the TikTok-style visual: a real first-frame preview fills the incoming page as soon as the user starts scrolling.

The same `AsyncImage` approach is also used for the **active page when `!isVideoReady`** (FR-005): instead of a black box + spinner, the current video's thumbnail is shown until `onRenderedFirstFrame()` fires. Because the thumbnail was preloaded at the previous step, it is already in cache when `isActive` becomes `true`, so the transition is seamless.

**Alternatives considered**:
- Showing the thumbnail only for inactive pages, keeping the black overlay for the active page — fails FR-005 (still shows a loading spinner during the transition).
- Using a separate `Painter` preloaded via `rememberAsyncImagePainter` — functionally equivalent but less idiomatic with Coil's Compose integration.

---

## Decision 4: `ImageLoader` Hilt provision

**Decision**: Provide Coil's singleton `ImageLoader` from `SingletonModule` (`@InstallIn(SingletonComponent::class)`) using `context.imageLoader` (Coil's extension property on `Context`).

**Rationale**: `SingletonComponent` gives the `ImageLoader` the same lifetime as the application, matching Coil's own singleton semantics. The `VideoViewerModule` is `ViewModelComponent`-scoped and cannot provide singletons. `SingletonModule` already provides other application-scoped dependencies (`Explorer`, `ExternalMediaRedirectCoordinator`).

**Alternatives considered**:
- Adding the provision to `VideoViewerModule` — wrong scope; `ImageLoader` should be app-scoped.
- Accessing `LocalContext.current.imageLoader` in the Composable and passing it to the ViewModel — leaks a Compose/Android dependency into the ViewModel constructor, harder to mock.

---

## Coil 2.7.0 API notes

- `VideoFrameDecoder` is registered automatically when `coil-video` is on the classpath.
- `ImageLoader.enqueue(request: ImageRequest): Disposable` starts loading asynchronously; the returned `Disposable` can be ignored when the goal is only cache warming.
- Memory cache key for a video file URI: defaults to the URI string. Identical URIs in `AsyncImage` and in `enqueue()` share the same cache entry.
- The Coil memory cache is a global `LruCache`. The default size is 20% of available RAM. For two adjacent first-frame thumbnails at typical video resolutions (≤ 2 MB each), the impact is negligible.

---

## Constitution compliance notes

- `ImageLoader` is injected — no `context.imageLoader` called inside ViewModel body (avoids Android-context leak risk).
- `@ApplicationContext Context` injection into a ViewModel is standard Hilt practice — not a leak.
- No new third-party libraries added; Coil and its video plugin are already declared in `gradle/libs.versions.toml`.
- `VideoFrameDecoder` use is implicit (auto-registered) — no direct Coil internal API calls.
- All new public functions will have KDoc.
- Named constants added for any new numeric literals (none expected for this feature beyond what already exists).
