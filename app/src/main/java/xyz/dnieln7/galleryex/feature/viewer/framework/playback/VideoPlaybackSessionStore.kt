package xyz.dnieln7.galleryex.feature.viewer.framework.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackPlaylist
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackSessionState
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared in-memory source of truth for the current video playback session.
 *
 * The service writes into this store whenever the playlist or active item changes, and the UI reads
 * from it to keep the viewer synchronized with background playback. The same state is also used to
 * rebuild the viewer when the user taps the media notification.
 */
@Singleton
class VideoPlaybackSessionStore @Inject constructor() {
    private val _sessionState = MutableStateFlow(VideoPlaybackSessionState())

    /**
     * Live playback snapshot observed by the viewer and activity.
     */
    val sessionState: StateFlow<VideoPlaybackSessionState> = _sessionState.asStateFlow()

    /**
     * Updates the store from a playlist issued by the UI/controller layer.
     *
     * This lets the app know which playlist is intended to be active even before Media3 has emitted
     * a player callback for the new media items.
     *
     * @param playlist Sanitized playlist that should become the active viewer session.
     */
    fun updateRequestedPlayback(playlist: VideoPlaybackPlaylist) {
        _sessionState.update {
            it.copy(
                videoPaths = playlist.videoPaths,
                selectedIndex = playlist.selectedIndex,
                currentVideoPath = playlist.videoPaths.getOrNull(playlist.selectedIndex),
                currentVideoTitle = playlist.videoPaths
                    .getOrNull(playlist.selectedIndex)
                    ?.let(::File)
                    ?.name,
            )
        }
    }

    /**
     * Rebuilds session state from the current Media3 player.
     *
     * The service calls this from its player listener so UI and notification re-entry always reflect
     * the latest playlist ordering, selected item, and playback flag reported by Media3.
     *
     * @param player Active service-owned player.
     */
    fun updateFromPlayer(player: Player) {
        val videoPaths = (0 until player.mediaItemCount).mapNotNull { index ->
            player.getMediaItemAt(index).absolutePathOrNull()
        }
        val selectedIndex = player.currentMediaItemIndex.takeIf { it in videoPaths.indices } ?: -1
        val currentVideoPath = videoPaths.getOrNull(selectedIndex)
        val currentVideoTitle = player.currentMediaItem
            ?.mediaMetadata
            ?.title
            ?.toString()
            ?: player.currentMediaItem
                ?.mediaMetadata
                ?.displayTitle
                ?.toString()
            ?: currentVideoPath?.let(::File)?.name

        _sessionState.value = VideoPlaybackSessionState(
            videoPaths = videoPaths,
            selectedIndex = selectedIndex,
            currentVideoPath = currentVideoPath,
            currentVideoTitle = currentVideoTitle,
            isPlaying = player.isPlaying,
        )
    }

    /**
     * Clears the in-memory session snapshot when the playback service is torn down.
     */
    fun clear() {
        _sessionState.value = VideoPlaybackSessionState()
    }
}

/**
 * Extracts the absolute file path backing a Media3 item.
 *
 * The service prefers `mediaId` because it is explicitly set to the original file path when media
 * items are created. URI fallbacks are kept for defensive compatibility.
 *
 * @return The absolute file path represented by this media item, or `null` when it cannot be resolved.
 */
private fun MediaItem.absolutePathOrNull(): String? {
    return mediaId.takeIf { it.isNotBlank() }
        ?: localConfiguration?.uri?.path
        ?: requestMetadata.mediaUri?.path
}
