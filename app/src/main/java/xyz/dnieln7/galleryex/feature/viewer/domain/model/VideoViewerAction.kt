package xyz.dnieln7.galleryex.feature.viewer.domain.model

import xyz.dnieln7.galleryex.core.domain.model.VolumeFile

/**
 * User intents and lifecycle events for the video viewer screen.
 */
sealed interface VideoViewerAction {
    /**
     * Initialises the viewer with a list of videos and the index to start at.
     *
     * Calling this more than once on the same ViewModel instance is a no-op; the guard
     * ensures that device rotation does not restart playback from the beginning.
     *
     * @property videos The full list of videos available in the current folder.
     * @property selectedIndex Index of the video that should begin playing.
     */
    data class Initialize(
        val videos: List<VolumeFile.Video>,
        val selectedIndex: Int,
    ) : VideoViewerAction

    /**
     * The pager settled on a new page.
     *
     * @property index The settled page index.
     */
    data class OnPageChange(val index: Int) : VideoViewerAction

    /** User tapped the play/pause button. */
    data object OnPlayPauseClick : VideoViewerAction

    /** User tapped the seek-back button. */
    data object OnSeekBack : VideoViewerAction

    /** User tapped the seek-forward button. */
    data object OnSeekForward : VideoViewerAction

    /**
     * User dragged the progress slider.
     *
     * @property value Normalised slider value in [0, 1].
     */
    data class OnSliderValueChange(val value: Float) : VideoViewerAction

    /** User released the progress slider after scrubbing. */
    data object OnSliderValueChangeFinished : VideoViewerAction

    /** User tapped the video surface to toggle the controls overlay. */
    data object OnTap : VideoViewerAction

    /** The screen lifecycle moved to ON_RESUME. */
    data object OnResume : VideoViewerAction

    /** The screen lifecycle moved to ON_PAUSE. */
    data object OnPause : VideoViewerAction
}
