package xyz.dnieln7.galleryex.feature.viewer.domain.model

import java.io.File

/**
 * In-memory description of the current video playback session.
 *
 * This is the shared state published by the playback service and observed by the UI. It mirrors the
 * information a screen needs to stay synchronized with the background player.
 *
 * @property videoPaths Current ordered playlist exposed by the service.
 * @property selectedIndex Index of the currently selected media item, or `-1` when nothing is active.
 * @property currentVideoPath Absolute path of the active media item when available.
 * @property currentVideoTitle Best-effort display title for the active media item.
 * @property isPlaying Whether Media3 reports the session as actively playing.
 */
data class VideoPlaybackSessionState(
    val videoPaths: List<String> = emptyList(),
    val selectedIndex: Int = -1,
    val currentVideoPath: String? = null,
    val currentVideoTitle: String? = null,
    val isPlaying: Boolean = false,
)

/**
 * Returns the best title available for UI surfaces such as the in-app viewer controls.
 *
 * If the service did not provide a title, the file name is derived from [currentVideoPath].
 *
 * @return A user-visible title, or an empty string when no active item exists.
 */
internal fun VideoPlaybackSessionState.currentVideoTitleOrFileName(): String {
    return currentVideoTitle ?: currentVideoPath?.let { File(it).name }.orEmpty()
}
