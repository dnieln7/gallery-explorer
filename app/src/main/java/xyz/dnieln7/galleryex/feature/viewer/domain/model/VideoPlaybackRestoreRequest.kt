package xyz.dnieln7.galleryex.feature.viewer.domain.model

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File

/**
 * Serializable snapshot of the playlist state needed to reopen the video viewer.
 *
 * This is intentionally small: it stores only the ordered playlist and the index that should be
 * focused when the viewer is reconstructed. The actual `Player` lives in the playback service.
 *
 * @property videoPaths Absolute file paths for the active folder-scoped playlist.
 * @property selectedIndex Index of the item that should be shown as the current video.
 */
internal data class VideoPlaybackRestoreRequest(
    val videoPaths: List<String>,
    val selectedIndex: Int,
)

/**
 * Builds a restore request from a raw playlist coming from the UI or from stored playback state.
 *
 * The helper removes entries that no longer exist on disk and remaps the selected index to the
 * nearest valid item so notification re-entry never tries to open a deleted file.
 *
 * @param videoPaths Raw playlist in folder order.
 * @param selectedIndex Preferred index within [videoPaths].
 * @return A sanitized restore request, or `null` when no playable files remain.
 */
internal fun createVideoPlaybackRestoreRequest(
    videoPaths: List<String>,
    selectedIndex: Int,
): VideoPlaybackRestoreRequest? {
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

    return VideoPlaybackRestoreRequest(
        videoPaths = indexedVideoPaths.map { it.videoPath },
        selectedIndex = indexedVideoPaths.indexOf(selectedVideo),
    )
}

/**
 * Converts the restore request into Media3 items for the playback service/controller layer.
 *
 * The file path is used as the `mediaId` so the service can later rebuild session state without
 * needing any extra mapping table.
 *
 * @return Media3 items preserving the playlist order from the restore request.
 */
internal fun VideoPlaybackRestoreRequest.toMediaItems(): List<MediaItem> {
    return videoPaths.map { videoPath ->
        MediaItem.Builder()
            .setMediaId(videoPath)
            .setUri(File(videoPath).toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(File(videoPath).name)
                    .build()
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
