# Feature Specification: TikTok-Style Video Scroll with First-Frame Preview

**Feature Branch**: `001-tiktok-video-scroll`

**Created**: 2026-05-17

**Status**: Draft

**Input**: User description: "Implement a tiktok scrolling behavior on VideoViewerScreen, the current behavior play the current video correctly, however when the user is mid-scrolling to see what the next or previous video is all they can see is a progress indicator. The new behavior must show the first frame of the next/previous video when the user is mid-scrolling."

## Clarifications

### Session 2026-05-17

- Q: When should adjacent video previews be loaded? → A: Load first-frame previews for the immediately adjacent videos (previous and next) as soon as a video becomes the active page — before the user begins scrolling (proactive loading).
- Q: Should first-frame previews be retained and reused during a session? → A: Retain loaded previews in memory for the entire duration of the video viewer screen session; discard only when the screen is closed.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Preview Adjacent Video While Scrolling (Priority: P1)

A user is watching a video and starts scrolling vertically to see what the next or previous video is. Instead of seeing only a black screen with a spinning progress indicator, they immediately see the first frame (a static image preview) of the adjacent video as it comes into view.

**Why this priority**: This is the core feature request. It directly addresses the poor user experience during scrolling transitions and is the minimum deliverable. Without it, the feature is not implemented at all.

**Independent Test**: Can be fully tested by opening a folder with multiple videos, playing one, then swiping to the next — the visible portion of the incoming video must show a recognizable first frame rather than a spinner.

**Acceptance Scenarios**:

1. **Given** the user is on the video viewer with at least two videos, **When** the user begins scrolling down toward the next video, **Then** the first frame of the next video is visible in the portion of the screen occupied by the incoming page.
2. **Given** the user is on the video viewer with at least two videos, **When** the user begins scrolling up toward the previous video, **Then** the first frame of the previous video is visible in the portion of the screen occupied by the incoming page.
3. **Given** the user has scrolled halfway between two videos, **When** both pages are partially visible, **Then** the current video's playback surface is visible on the outgoing portion and the first frame of the adjacent video is visible on the incoming portion.
4. **Given** the user releases the scroll midway and the pager snaps back to the current video, **Then** the current video resumes normal playback without interruption.

---

### User Story 2 - Smooth Transition to Newly Selected Video (Priority: P2)

After the user completes a scroll gesture and the pager settles on a new video, the first-frame preview transitions seamlessly into the fully loaded and playing video without a visible flash or additional loading spinner.

**Why this priority**: Completing the scroll must feel polished. If the first-frame preview disappears and is replaced by a spinner before playback starts, the UX degrades again at the final step.

**Independent Test**: Can be tested by scrolling fully to the next video and observing that the transition from the first-frame preview to active playback is smooth, with no black screen or spinner appearing between them.

**Acceptance Scenarios**:

1. **Given** the user fully scrolls to the next video and the pager settles, **When** the video begins loading and playing, **Then** the first-frame image is shown until the video is ready to play and then disappears — no black intermediate state or spinner is shown.
2. **Given** the video fails to load (e.g., file is unreadable), **When** the pager settles on the video, **Then** a clear error indicator or fallback state is shown instead of an indefinite spinner.

---

### Edge Cases

- What happens when a video file is corrupt or has no decodable first frame?
- What happens when the folder contains only one video (no adjacent videos to preview)?
- What happens when the user scrolls very quickly past multiple videos?
- How does the system behave if the first-frame preview for a very large video file takes too long to load?
- What happens when the device has limited memory and cannot cache multiple first-frame images?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST display the first frame (a static image) of the next video when the user scrolls downward on the video viewer screen.
- **FR-002**: The system MUST display the first frame (a static image) of the previous video when the user scrolls upward on the video viewer screen.
- **FR-003**: The first-frame preview MUST be visible during any partial scroll that brings an adjacent video page into view, not only after the scroll completes.
- **FR-004**: The currently playing video MUST continue its playback state uninterrupted while the user is mid-scrolling.
- **FR-005**: When the pager settles on a new video, the first-frame preview MUST transition to active video playback without showing a black screen or spinner between them.
- **FR-006**: The system MUST load first-frame previews asynchronously so that the UI remains responsive during loading.
- **FR-007**: If a first-frame preview cannot be loaded (e.g., corrupt file, unsupported format), the system MUST show a neutral fallback (e.g., a dark placeholder) rather than a spinner or crash.
- **FR-008**: First-frame previews MUST only be loaded for immediately adjacent videos (the one directly before and the one directly after the current video) to avoid unnecessary resource usage.
- **FR-009**: The system MUST begin loading the first-frame preview for the immediately adjacent videos (previous and next) as soon as a video becomes the active page, before the user initiates a scroll gesture, so that previews are available instantly when scrolling begins.
- **FR-010**: Once loaded, a first-frame preview MUST be retained in memory and reused for the remainder of the video viewer screen session; it MUST NOT be reloaded from the source each time the user scrolls back to that position.

### Key Entities

- **Video First Frame**: A static image representing the first decoded frame of a video file, used as a visual preview during scroll transitions. Identified by the video file's path and loaded on demand.
- **Pager Scroll State**: The current scroll offset and settled page of the vertical pager, used to determine which adjacent videos are currently visible to the user.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When a user initiates a scroll gesture on the video viewer, the first frame of the adjacent video becomes visible within 500 milliseconds of the adjacent page entering the screen.
- **SC-002**: The currently playing video experiences zero interruption (no pause, seek, or restart) while the user is actively scrolling between videos.
- **SC-003**: 100% of scroll transitions from a first-frame preview to active playback occur without displaying a black screen or progress indicator between the two states.
- **SC-004**: The first-frame preview load failure rate results in a neutral fallback state in 100% of cases — no crash or indefinite spinner.

## Assumptions

- Users interact with the video viewer screen in a folder that contains at least two video files; the single-video case is handled gracefully (no adjacent previews needed).
- First-frame preview images are loaded using the existing image loading infrastructure already present in the project (the project already includes a video-capable image loading library).
- The first-frame preview for each video is derived from the video file itself and does not require a separate server call or pre-generated thumbnail asset.
- The previews are only needed for immediately adjacent videos (previous and next); videos further away in the list do not require preloading.
- The scroll behavior is vertical, matching the existing vertical pager already implemented in the video viewer.
- The existing video playback engine (used for the current video) is not replaced or altered by this feature.
- Memory and performance impact of loading two adjacent first-frame images at a time is acceptable on the target device range.
- First-frame preview images are held in memory for the duration of the screen session; the total memory footprint is bounded by the number of unique videos visited during the session, not by the total video count in the folder.
