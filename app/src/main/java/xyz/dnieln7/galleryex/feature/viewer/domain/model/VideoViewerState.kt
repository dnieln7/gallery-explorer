package xyz.dnieln7.galleryex.feature.viewer.domain.model

/**
 * Immutable state for the video viewer screen.
 *
 * @property videoPaths Ordered absolute paths of the active playlist.
 * @property selectedIndex Index of the currently active video, or `-1` when nothing is loaded.
 * @property currentVideoTitle Display title for the active video when provided by Media3 metadata.
 */
data class VideoViewerState(
    val videoPaths: List<String> = emptyList(),
    val selectedIndex: Int = -1,
    val currentVideoTitle: String? = null,
)
