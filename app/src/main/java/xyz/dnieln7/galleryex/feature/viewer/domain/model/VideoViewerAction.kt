package xyz.dnieln7.galleryex.feature.viewer.domain.model

/**
 * User intents and lifecycle-driven commands for the video viewer.
 */
sealed interface VideoViewerAction {
    /** Loads and sanitizes a folder-scoped playlist, then starts playback at [selectedIndex]. */
    data class OpenPlaylist(val videoPaths: List<String>, val selectedIndex: Int) : VideoViewerAction

    /** Switches the active item within the already-loaded playlist. */
    data class SelectVideo(val index: Int) : VideoViewerAction

    /** Toggles between play and pause. */
    data object TogglePlayPause : VideoViewerAction

    /** Seeks to an absolute position in the active video. */
    data class SeekTo(val positionMs: Long) : VideoViewerAction

    /** Seeks back by the configured increment. */
    data object SeekBack : VideoViewerAction

    /** Seeks forward by the configured increment. */
    data object SeekForward : VideoViewerAction

    /** Pauses playback when the app leaves the foreground. */
    data object PauseForBackground : VideoViewerAction

    /** Resumes playback when the app returns to the foreground, only if it was playing before. */
    data object ResumeFromBackground : VideoViewerAction

    /** Stops playback in response to an explicit user exit. */
    data object StopPlayback : VideoViewerAction
}
