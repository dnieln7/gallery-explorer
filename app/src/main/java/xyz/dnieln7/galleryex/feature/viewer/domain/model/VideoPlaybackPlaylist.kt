package xyz.dnieln7.galleryex.feature.viewer.domain.model

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File

/**
 * Sanitized snapshot of the playlist state used to drive the video viewer.
 *
 * This is intentionally small: it stores only the ordered playlist and the index that should be
 * focused when the viewer is shown. The actual `Player` lives in the playback service.
 *
 * @property videoPaths Absolute file paths for the active folder-scoped playlist.
 * @property selectedIndex Index of the item that should be shown as the current video.
 */
data class VideoPlaybackPlaylist(
    val videoPaths: List<String>,
    val selectedIndex: Int,
)

/**
 * Builds a playlist from a raw path list coming from the UI or from stored playback state.
 *
 * The helper removes entries that no longer exist on disk and remaps the selected index to the
 * nearest valid item so the viewer never tries to open a deleted file.
 *
 * @param videoPaths Raw playlist in folder order.
 * @param selectedIndex Preferred index within [videoPaths].
 * @return A sanitized playlist, or `null` when no playable files remain.
 */
internal fun createVideoPlaybackPlaylist(
    videoPaths: List<String>,
    selectedIndex: Int,
): VideoPlaybackPlaylist? {
    val indexedVideoPaths = videoPaths.mapIndexedNotNull { index, videoPath ->
        if (File(videoPath).isFile) {
            IndexedVideoPath(
                originalIndex = index,
                videoPath = videoPath,
            )
        } else {
            null
        }
    }

    if (indexedVideoPaths.isEmpty()) {
        return null
    }

    val requestedIndex = selectedIndex.coerceIn(0, videoPaths.lastIndex)
    val selectedVideo = indexedVideoPaths.firstOrNull { it.originalIndex >= requestedIndex }
        ?: indexedVideoPaths.last()

    return VideoPlaybackPlaylist(
        videoPaths = indexedVideoPaths.map { it.videoPath },
        selectedIndex = indexedVideoPaths.indexOf(selectedVideo),
    )
}

/**
 * Converts the playlist into Media3 items for the playback service/controller layer.
 *
 * The file path is used as the `mediaId` so the service can later rebuild session state without
 * needing any extra mapping table.
 *
 * @return Media3 items preserving the playlist order.
 */
internal fun VideoPlaybackPlaylist.toMediaItems(): List<MediaItem> {
    return videoPaths.map { videoPath ->
        MediaItem.Builder()
            .setMediaId(videoPath)
            .setUri(File(videoPath).toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(File(videoPath).name)
                    .build(),
            )
            .build()
    }
}

/**
 * Temporary helper that keeps the original raw index while invalid files are filtered out.
 */
private data class IndexedVideoPath(
    val originalIndex: Int,
    val videoPath: String,
)
