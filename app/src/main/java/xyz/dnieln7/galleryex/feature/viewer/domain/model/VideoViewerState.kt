package xyz.dnieln7.galleryex.feature.viewer.domain.model

import androidx.media3.common.Player

/**
 * Immutable state for the video viewer screen.
 *
 * @property activePage Index of the currently settled pager page.
 * @property isPlaying Whether the player is actively playing.
 * @property showControls Whether the playback controls overlay is visible.
 * @property isScrubbing Whether the user is currently dragging the progress slider.
 * @property scrubSliderValue Normalised slider position in [0, 1] while the user is scrubbing.
 * @property durationTotalMs Total duration of the current video in milliseconds.
 * @property durationCurrentMs Playback position in milliseconds, scrub-aware.
 * @property durationSlider Normalised slider position in [0, 1], scrub-aware.
 * @property isVideoReady Whether the player has rendered the first frame of the current video.
 *   False immediately after a video transition; true once [Player.Listener.onRenderedFirstFrame]
 *   fires. Used by the UI to overlay a thumbnail until the new video surface is ready.
 */
data class VideoViewerState(
    val activePage: Int = 0,
    val isPlaying: Boolean = false,
    val showControls: Boolean = true,
    val isScrubbing: Boolean = false,
    val scrubSliderValue: Float = 0f,
    val durationTotalMs: Long = 0L,
    val durationCurrentMs: Long = 0L,
    val durationSlider: Float = 0f,
    val isVideoReady: Boolean = false,
)
