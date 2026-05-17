package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerAction
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerEvent
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerState
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.positionToSliderValue
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.sliderValueToPosition
import javax.inject.Inject

/**
 * ViewModel for [VideoViewerScreenDestination].
 *
 * Owns the [ExoPlayer] instance so it survives configuration changes such as device rotation.
 * All playback commands and state mutations flow through [onAction], keeping the UI fully
 * stateless and testable.
 *
 * When the active page changes, [preloadAdjacentThumbnails] enqueues first-frame thumbnail
 * requests via [imageLoader] for the immediately adjacent videos, warming Coil's memory cache
 * before the user initiates a scroll gesture.
 *
 * @property player The ExoPlayer instance used for video playback. Exposed so that the
 *   stateless screen can bind a [androidx.media3.ui.PlayerView] to it directly.
 * @property imageLoader Coil [ImageLoader] used to proactively warm the thumbnail cache for
 *   adjacent videos.
 * @property context Application context used to build [ImageRequest] instances for thumbnail
 *   preloading.
 */
@HiltViewModel
class VideoViewerViewModel @Inject constructor(
    val player: ExoPlayer,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _events = Channel<VideoViewerEvent>()
    val events = _events.receiveAsFlow()

    private val _uiState = MutableStateFlow(VideoViewerState())
    val uiState = _uiState.asStateFlow()

    private var isInitialized = false
    private var videos: List<VolumeFile.Video> = emptyList()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            _uiState.update { state ->
                state.copy(
                    durationTotalMs = player.duration.takeIf { it > 0L } ?: 0L,
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onRenderedFirstFrame() {
            _uiState.update { it.copy(isVideoReady = true) }
        }
    }

    init {
        player.addListener(playerListener)
        startPositionPolling()
        startControlsAutoHide()
    }

    /**
     * Single entry point for all user intents and lifecycle events.
     */
    fun onAction(action: VideoViewerAction) {
        when (action) {
            is VideoViewerAction.Initialize -> onInitialize(action.videos, action.selectedIndex)
            is VideoViewerAction.OnPageChange -> onPageChange(action.index)
            VideoViewerAction.OnPlayPauseClick -> onPlayPause()
            VideoViewerAction.OnSeekBack -> onSeekBack()
            VideoViewerAction.OnSeekForward -> onSeekForward()
            is VideoViewerAction.OnSliderValueChange -> onSliderValueChange(action.value)
            VideoViewerAction.OnSliderValueChangeFinished -> onSliderValueChangeFinished()
            VideoViewerAction.OnTap -> onTap()
            VideoViewerAction.OnResume -> player.play()
            VideoViewerAction.OnPause -> player.pause()
        }
    }

    private fun onInitialize(videos: List<VolumeFile.Video>, selectedIndex: Int) {
        if (isInitialized) return
        isInitialized = true
        this.videos = videos
        loadVideo(selectedIndex)
    }

    private fun onPageChange(index: Int) {
        if (!isInitialized || index == _uiState.value.activePage) return
        loadVideo(index)
    }

    private fun onPlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }

        _uiState.update { it.copy(showControls = true) }
    }

    private fun onSeekBack() {
        player.seekBack()

        _uiState.update { it.copy(showControls = true) }
    }

    private fun onSeekForward() {
        player.seekForward()

        _uiState.update { it.copy(showControls = true) }
    }

    private fun onSliderValueChange(value: Float) {
        _uiState.update { state ->
            state.copy(
                isScrubbing = true,
                scrubSliderValue = value,
                showControls = true,
                durationCurrentMs = sliderValueToPosition(
                    sliderValue = value,
                    durationMs = state.durationTotalMs,
                ),
                durationSlider = value,
            )
        }
    }

    private fun onSliderValueChangeFinished() {
        val state = _uiState.value

        val targetPosition = sliderValueToPosition(
            sliderValue = state.scrubSliderValue,
            durationMs = state.durationTotalMs,
        )

        player.seekTo(targetPosition)

        _uiState.update { it.copy(isScrubbing = false, showControls = true) }
    }

    private fun onTap() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    private fun loadVideo(index: Int) {
        _uiState.update { it.copy(isVideoReady = false) }

        player.stop()

        val video = videos.getOrNull(index) ?: return

        player.setMediaItem(MediaItem.fromUri(video.file.toUri()))
        player.prepare()
        player.seekTo(0L)
        player.play()

        _uiState.update {
            it.copy(
                activePage = index,
                showControls = true,
                isScrubbing = false,
                scrubSliderValue = 0f,
                durationCurrentMs = 0L,
                durationSlider = 0f,
                durationTotalMs = 0L,
            )
        }

        preloadAdjacentThumbnails(index)
    }

    /**
     * Enqueues Coil [ImageRequest]s for the videos immediately before and after [index],
     * warming the memory cache with their first-frame thumbnails before the user begins
     * scrolling. Out-of-bounds indices are safely ignored via [List.getOrNull].
     *
     * @param index The index of the currently active video.
     */
    private fun preloadAdjacentThumbnails(index: Int) {
        listOfNotNull(
            videos.getOrNull(index - 1),
            videos.getOrNull(index + 1),
        ).forEach { video ->
            val request = ImageRequest.Builder(context)
                .data(video.file.toUri())
                .build()
            imageLoader.enqueue(request)
        }
    }

    private fun startPositionPolling() {
        viewModelScope.launch {
            _uiState
                .map { it.isPlaying && !it.isScrubbing }
                .distinctUntilChanged()
                .collectLatest { isPlaying ->
                    if (isPlaying) {
                        while (true) {
                            val position = player.currentPosition.coerceAtLeast(0L)

                            _uiState.update { state ->
                                state.copy(
                                    durationCurrentMs = position,
                                    durationSlider = positionToSliderValue(
                                        positionMs = position,
                                        durationMs = state.durationTotalMs,
                                    ),
                                )
                            }

                            delay(POSITION_POLLING_INTERVAL_MS)
                        }
                    }
                }
        }
    }

    private fun startControlsAutoHide() {
        viewModelScope.launch {
            _uiState.collectLatest { state ->
                if (state.showControls && state.isPlaying && !state.isScrubbing) {
                    delay(CONTROLS_AUTO_HIDE_DELAY_MS)
                    _uiState.update { it.copy(showControls = false) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        player.removeListener(playerListener)
        player.release()
    }
}

private const val CONTROLS_AUTO_HIDE_DELAY_MS: Long = 2_500L
private const val POSITION_POLLING_INTERVAL_MS: Long = 500L
