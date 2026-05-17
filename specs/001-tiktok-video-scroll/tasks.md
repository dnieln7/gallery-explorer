# Tasks: TikTok-Style Video Scroll with First-Frame Preview

**Input**: Design documents from `specs/001-tiktok-video-scroll/`

**Prerequisites**: [plan.md](plan.md) · [spec.md](spec.md) · [research.md](research.md) · [data-model.md](data-model.md)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other [P] tasks in the same phase (different files, no shared dependencies)
- **[Story]**: Which user story this task belongs to (US1 = P1 scroll preview, US2 = P2 active transition)

## Path Conventions

All source paths are relative to:
`app/src/main/java/xyz/dnieln7/galleryex/`

All test paths are relative to:
`app/src/test/java/xyz/dnieln7/galleryex/`

---

## Phase 1: Foundational (Blocking Prerequisite)

**Purpose**: Provide `ImageLoader` via Hilt so the ViewModel can inject it. This single task MUST be done before any US1 work begins; nothing else is blocked.

**⚠️ CRITICAL**: `VideoViewerViewModel` injection changes cannot compile until this provision exists.

- [ ] T001 Add `@Singleton` `ImageLoader` binding to `di/SingletonModule.kt` using `context.imageLoader` (Coil's singleton extension on `@ApplicationContext Context`)

**Checkpoint**: `./gradlew :app:assembleDebug` compiles — `ImageLoader` is now injectable.

---

## Phase 2: User Story 1 — Preview Adjacent Video While Scrolling (Priority: P1) 🎯 MVP

**Goal**: Replace the `CircularProgressIndicator` on inactive pager pages with a real first-frame thumbnail loaded via Coil, and preload adjacent thumbnails proactively in the ViewModel so the preview is available before the user starts scrolling.

**Independent Test**: Open a folder with 3+ videos → play the first video → swipe down slowly → the incoming page MUST show a recognizable first frame of the next video, not a spinner.

### Implementation for User Story 1

- [ ] T002 [P] [US1] Add `imageLoader: ImageLoader` and `@ApplicationContext context: Context` constructor parameters to `VideoViewerViewModel` in `feature/viewer/presentation/screen/VideoViewerViewModel.kt`; update KDoc
- [ ] T003 [P] [US1] Add `videoFile: File` parameter (before `onTap`) to `VideoSurface` in `feature/viewer/presentation/component/VideoSurface.kt`; replace the `!isActive` branch's `CircularProgressIndicator` with `AsyncImage(model = videoFile.toUri(), contentScale = ContentScale.Fit)`; update KDoc
- [ ] T004 [US1] Add private `preloadAdjacentThumbnails(index: Int)` function to `VideoViewerViewModel` that enqueues `ImageRequest` via `imageLoader` for `videos.getOrNull(index - 1)` and `videos.getOrNull(index + 1)`; call it at the end of `loadVideo()` in `feature/viewer/presentation/screen/VideoViewerViewModel.kt`
- [ ] T005 [US1] Update `VideoViewerScreen` pager block in `feature/viewer/presentation/screen/VideoViewerScreen.kt` to pass `videoFile = videos[index].file` to `VideoSurface` (depends on T003)
- [ ] T006 [US1] Update `feature/viewer/presentation/screen/VideoViewerViewModelTest.kt`: add `imageLoader = relaxedMockk<ImageLoader>()` and `context = relaxedMockk<Context>()` fields; update all `VideoViewerViewModel(player)` constructor calls to `VideoViewerViewModel(player, imageLoader, context)`; add four new tests:
  - `` `GIVEN videos and a selected index WHEN Initialize is called THEN imageLoader enqueues thumbnails for the previous and next videos` ``
  - `` `GIVEN initialized viewmodel WHEN OnPageChange is called with a new page THEN imageLoader enqueues thumbnails for the adjacent videos of the new page` ``
  - `` `GIVEN the first video is selected WHEN Initialize is called THEN imageLoader enqueues only the next thumbnail` ``
  - `` `GIVEN the last video is selected WHEN Initialize is called THEN imageLoader enqueues only the previous thumbnail` ``

**Checkpoint**: `./gradlew :app:testDebugUnitTest` passes all 4 new tests. Manually swipe to an adjacent video — the incoming page shows its first frame instead of a spinner.

---

## Phase 3: User Story 2 — Smooth Transition to Newly Selected Video (Priority: P2)

**Goal**: After the pager settles on a new video, show the video's cached first-frame thumbnail (already in Coil's memory from proactive loading) instead of a black screen + spinner while ExoPlayer renders the first real frame.

**Independent Test**: Swipe fully to the next video and let the pager settle → NO black screen or `CircularProgressIndicator` must appear between the thumbnail and live playback. The first-frame image must remain visible until `onRenderedFirstFrame` fires and `isVideoReady` becomes `true`.

### Implementation for User Story 2

- [ ] T007 [US2] Update `VideoSurface` in `feature/viewer/presentation/component/VideoSurface.kt`: replace the `isActive && !isVideoReady` branch's black `Box` + `CircularProgressIndicator` overlay with `AsyncImage(model = videoFile.toUri(), contentScale = ContentScale.Fit)` (depends on T003 which added the `videoFile` param)

**Checkpoint**: Complete a swipe to the next video → thumbnail remains visible, video starts playing, no black flash.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Validate quality gates before merge.

- [ ] T008 [P] Run Detekt: `./gradlew :app:detekt` — confirm zero new violations; if any are reported, fix them (do NOT modify `app/detekt/baseline.xml`)
- [ ] T009 [P] Run full unit test suite: `./gradlew :app:testDebugUnitTest` — all tests must pass including the 4 new preload tests from T006
- [ ] T010 Verify KDoc on all modified public/internal functions and classes: `VideoViewerViewModel`, `VideoSurface`, `SingletonModule.provideImageLoader` — each must have a complete KDoc block with `@property`/`@param`/`@return` tags as applicable

**Checkpoint**: All gates pass — branch is ready for review.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — start immediately
- **User Story 1 (Phase 2)**: Depends on Phase 1 (T001) completion — BLOCKS T002, T003, T004
- **User Story 2 (Phase 3)**: Depends on T003 (VideoSurface `videoFile` param must exist) — can start once T003 is done, without waiting for full US1 completion
- **Polish (Phase 4)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Depends only on T001 (Foundational); no dependency on US2
- **US2 (P2)**: Depends only on T003 (VideoSurface signature change from US1); independently testable

### Within User Story 1

```
T001 (Foundational)
  ↓
T002 [P]  T003 [P]   ← can run in parallel (different files)
  ↓           ↓
T004        T005     ← T004 depends on T002; T005 depends on T003
  ↓
T006 (tests depend on T002 + T004 being complete)
```

### Within User Story 2

```
T003 (from US1, VideoSurface param)
  ↓
T007
```

### Parallel Opportunities

- T002 and T003 can run in parallel (different files: ViewModel vs VideoSurface)
- T008 and T009 can run in parallel in the polish phase

---

## Parallel Example: User Story 1

```
# T002 and T003 can be launched together:
Task: "Add ImageLoader + Context injection to VideoViewerViewModel"
Task: "Add videoFile param to VideoSurface and replace inactive spinner with AsyncImage"

# After both complete:
Task: "Add preloadAdjacentThumbnails() to VideoViewerViewModel"   ← T004
Task: "Update VideoViewerScreen to pass videoFile to VideoSurface" ← T005

# After T004 is complete:
Task: "Add 4 ViewModel unit tests for preloading"  ← T006
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: T001 — add `ImageLoader` provision
2. Complete Phase 2 (US1): T002 → T003 (parallel) → T004, T005 → T006
3. **STOP and VALIDATE**: Swipe between videos — first-frame preview visible during scroll
4. This is already a shippable improvement; US2 can follow separately

### Incremental Delivery

1. T001 → T002 + T003 → T004 + T005 → T006 → **US1 done and testable**
2. T007 → **US2 done and testable**
3. T008 + T009 + T010 → **Ready to merge**

---

## Notes

- **No new files** are created by this feature — all changes are modifications to existing files
- **No new dependencies** — Coil 2.7.0 with `coil-video` is already declared in `gradle/libs.versions.toml`
- `baseline.xml` MUST NOT be touched; fix any Detekt findings in-place
- `isVideoReady` must remain gated on `onRenderedFirstFrame()` — do NOT set it to `true` before that callback fires
- The `AsyncImage` uses `videoFile.toUri()` as its model — `VideoFrameDecoder` (auto-registered by `coil-video`) handles first-frame extraction
