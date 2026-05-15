package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.dnieln7.galleryex.di.IO
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackPlaylist
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerAction
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerEvent
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerState
import xyz.dnieln7.galleryex.feature.viewer.domain.model.createVideoPlaybackPlaylist
import xyz.dnieln7.galleryex.feature.viewer.domain.model.toMediaItems
import java.io.File
import javax.inject.Inject

/**
 * ViewModel that owns the [ExoPlayer] instance for the video viewer screen.
 *
 * The player is created eagerly so it is immediately available when the composable first renders.
 * Its lifetime matches the Voyager screen: created on first entry, released in [onCleared] when the
 * user navigates away. Configuration changes (rotation) are handled transparently by Voyager's
 * ViewModel retention.
 *
 * Audio focus and audio-becoming-noisy handling (headphone disconnect) are delegated to ExoPlayer
 * via [setAudioAttributes] with `handleAudioFocus = true`, removing the need for a manual
 * BroadcastReceiver.
 *
 * @property context Application context used to build the ExoPlayer instance.
 * @property dispatcher Dispatcher for the file-existence checks in playlist sanitization.
 */
@HiltViewModel
class VideoViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IO private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoViewerState())
    val uiState: StateFlow<VideoViewerState> = _uiState.asStateFlow()

    private val _events = Channel<VideoViewerEvent>()
    val events: Flow<VideoViewerEvent> = _events.receiveAsFlow()

    private var resumeOnForeground = false

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _uiState.update {
                it.copy(
                    selectedIndex = player.currentMediaItemIndex,
                    currentVideoTitle = mediaItem?.mediaMetadata?.title?.toString(),
                )
            }
        }
    }

    val player: Player = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(PLAYER_SEEK_INCREMENT_MS)
        .setSeekForwardIncrementMs(PLAYER_SEEK_INCREMENT_MS)
        .build()
        .apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            addListener(playerListener)
        }

    /**
     * Single entry point for all user and lifecycle-driven interactions with the video viewer.
     */
    fun onAction(action: VideoViewerAction) {
        when (action) {
            is VideoViewerAction.OpenPlaylist -> openPlaylist(action.videoPaths, action.selectedIndex)

            is VideoViewerAction.SelectVideo -> selectVideo(action.index)

            VideoViewerAction.TogglePlayPause -> togglePlayPause()

            is VideoViewerAction.SeekTo -> player.seekTo(action.positionMs)

            VideoViewerAction.SeekBack -> player.seekBack()

            VideoViewerAction.SeekForward -> player.seekForward()

            VideoViewerAction.PauseForBackground -> {
                resumeOnForeground = player.playWhenReady
                player.pause()
            }

            VideoViewerAction.ResumeFromBackground -> {
                if (resumeOnForeground) {
                    player.play()
                }
                resumeOnForeground = false
            }

            VideoViewerAction.StopPlayback -> player.stop()
        }
    }

    override fun onCleared() {
        player.removeListener(playerListener)
        player.release()
        super.onCleared()
    }

    /**
     * Sanitizes the raw path list on the IO dispatcher (file existence is disk I/O), then loads the
     * resulting playlist into ExoPlayer. Reuses the existing media items when the playlist matches.
     */
    private fun openPlaylist(videoPaths: List<String>, selectedIndex: Int) {
        viewModelScope.launch {
            val playlist = withContext(dispatcher) {
                createVideoPlaybackPlaylist(videoPaths, selectedIndex)
            } ?: return@launch

            _uiState.update {
                it.copy(
                    videoPaths = playlist.videoPaths,
                    selectedIndex = playlist.selectedIndex,
                    currentVideoTitle = playlist.videoPaths
                        .getOrNull(playlist.selectedIndex)
                        ?.let(::File)
                        ?.name,
                )
            }

            applyPlaylist(playlist)
        }
    }

    /**
     * Switches the active item within the already-loaded playlist when the user swipes to another page.
     */
    private fun selectVideo(index: Int) {
        if (index !in 0 until player.mediaItemCount) {
            return
        }

        if (player.currentMediaItemIndex != index) {
            player.seekToDefaultPosition(index)
            player.play()
        }

        _uiState.update {
            it.copy(
                selectedIndex = index,
                currentVideoTitle = player.getMediaItemAt(index)
                    .mediaMetadata
                    .title
                    ?.toString(),
            )
        }
    }

    private fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    /**
     * Loads [playlist] into ExoPlayer, reusing the existing items when the playlist is unchanged
     * to avoid an unnecessary seek-to-start.
     */
    private fun applyPlaylist(playlist: VideoPlaybackPlaylist) {
        val currentPaths = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }

        if (currentPaths == playlist.videoPaths) {
            if (player.currentMediaItemIndex != playlist.selectedIndex &&
                playlist.selectedIndex in 0 until player.mediaItemCount
            ) {
                player.seekToDefaultPosition(playlist.selectedIndex)
                player.play()
            }

            if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) {
                player.prepare()
            }

            return
        }

        player.setMediaItems(playlist.toMediaItems(), playlist.selectedIndex, 0L)
        player.prepare()
        player.play()
    }
}

private const val PLAYER_SEEK_INCREMENT_MS = 10_000L
