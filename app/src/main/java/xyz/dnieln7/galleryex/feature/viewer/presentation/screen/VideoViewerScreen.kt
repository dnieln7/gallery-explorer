package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackSessionState
import xyz.dnieln7.galleryex.feature.viewer.domain.model.currentVideoTitleOrFileName
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.LocalVideoPlaybackController
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackController
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.ControlsAutoHideDelayMs
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoPlaybackControls
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoSurface
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.positionToSliderValue
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.seekBackwardPosition
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.seekForwardPosition
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.sliderValueToPosition
import java.io.File
import androidx.media3.common.Player

/**
 * Voyager destination that shows a vertically swipeable video viewer for a folder-scoped list of videos.
 *
 * @property videoPaths Absolute paths of the videos available in the current folder, preserved in folder order.
 * @property selectedIndex Index of the tapped video that should start playback.
 */
class VideoViewerScreenDestination(
    val videoPaths: List<String>,
    val selectedIndex: Int,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val videos = remember(videoPaths) { videosFromPaths(videoPaths) }

        VideoViewerScreen(
            videos = videos,
            selectedIndex = selectedIndex,
            navigateBack = { navigator.pop() },
        )
    }
}

@Composable
private fun VideoViewerScreen(
    videos: List<VolumeFile.Video>,
    selectedIndex: Int,
    navigateBack: () -> Unit,
) {
    if (videos.isEmpty()) {
        return
    }

    val videoPlaybackController = LocalVideoPlaybackController.current

    val initialPage = selectedIndex.coerceIn(0, videos.lastIndex)
    val pagerState = rememberPagerState(pageCount = { videos.size }, initialPage = initialPage)
    val player by videoPlaybackController.player.collectAsStateWithLifecycle()
    val sessionState by videoPlaybackController.sessionState.collectAsStateWithLifecycle()
    val activePage by remember(sessionState.selectedIndex, pagerState, videos) {
        derivedStateOf {
            sessionState.selectedIndex.takeIf { it in videos.indices } ?: pagerState.settledPage
        }
    }
    val activeVideo by remember(videos, activePage) { derivedStateOf { videos[activePage] } }

    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubSliderValue by remember { mutableFloatStateOf(0f) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // Keeps the local Compose state in sync with the current Player instance exposed by the
    // controller. This effect is restarted whenever the service connection swaps in a new Player.
    DisposableEffect(player) {
        val activePlayer = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                isPlaying = player.isPlaying
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.takeIf { it > 0L } ?: 0L
            }
        }

        activePlayer.addListener(listener)

        onDispose {
            activePlayer.removeListener(listener)
        }
    }

    // Pushes the folder-scoped playlist into the shared playback layer when the viewer is opened or
    // reconstructed with a different selection. This is what tells the background service which
    // videos belong to the current swipe session.
    LaunchedEffect(videoPlaybackController, videos, selectedIndex) {
        videoPlaybackController.openPlaylist(
            videoPaths = videos.map { it.file.absolutePath },
            selectedIndex = selectedIndex,
        )
    }

    // Applies player-driven selection changes back into the pager. This covers service-side changes
    // such as notification re-entry or controller commands that move playback to another item.
    LaunchedEffect(sessionState.selectedIndex) {
        val targetPage = sessionState.selectedIndex.takeIf { it in videos.indices } ?: return@LaunchedEffect

        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    // Applies pager-driven selection changes back into the playback session. Once a swipe settles on
    // another page, the controller updates the background player so in-app UI and notification stay
    // aligned on the same active video.
    LaunchedEffect(pagerState.settledPage) {
        val settledPage = pagerState.settledPage

        if (settledPage in videos.indices && sessionState.selectedIndex != settledPage) {
            videoPlaybackController.selectVideo(settledPage)
        }
    }

    // Resets transient overlay state whenever the active page changes so each newly selected video
    // starts with visible controls and a fresh non-scrubbing state.
    LaunchedEffect(activePage) {
        showControls = true
        isScrubbing = false
        scrubSliderValue = 0f
    }

    // Polls playback position while the viewer is active. This is intentionally paused during manual
    // scrubbing so the slider thumb does not fight the user's drag gesture.
    LaunchedEffect(player, isScrubbing) {
        while (true) {
            val activePlayer = player

            if (activePlayer != null && !isScrubbing) {
                currentPositionMs = activePlayer.currentPosition.coerceAtLeast(0L)
                durationMs = activePlayer.duration.takeIf { it > 0L } ?: 0L
                isPlaying = activePlayer.isPlaying
            }

            delay(250L)
        }
    }

    // Auto-hides controls after a short delay while a video is actively playing. Any change that
    // makes the overlay invalid for auto-hide, such as pausing or starting a scrub, cancels and
    // restarts this effect with the new state.
    LaunchedEffect(showControls, isPlaying, isScrubbing, activePage) {
        if (showControls && isPlaying && !isScrubbing) {
            delay(ControlsAutoHideDelayMs)
            showControls = false
        }
    }

    val sliderValue = if (isScrubbing) {
        scrubSliderValue
    } else {
        positionToSliderValue(
            positionMs = currentPositionMs,
            durationMs = durationMs,
        )
    }
    val displayedPositionMs = if (isScrubbing) {
        sliderValueToPosition(
            sliderValue = scrubSliderValue,
            durationMs = durationMs,
        )
    } else {
        currentPositionMs
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VerticalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                flingBehavior = PagerDefaults.flingBehavior(pagerState),
                userScrollEnabled = !isScrubbing,
            ) { index ->
                VideoSurface(
                    modifier = Modifier.fillMaxSize(),
                    video = videos[index],
                    player = player,
                    isActive = activePage == index,
                    onTap = {
                        showControls = !showControls
                    },
                )
            }

            VideoPlaybackControls(
                modifier = Modifier.fillMaxSize(),
                title = sessionState.currentVideoTitleOrFileName().ifBlank { activeVideo.name },
                isVisible = showControls,
                isPlaying = isPlaying,
                currentPositionMs = displayedPositionMs,
                durationMs = durationMs,
                sliderValue = sliderValue,
                onBackClick = navigateBack,
                onPlayPauseClick = {
                    val activePlayer = player

                    if (activePlayer?.isPlaying == true) {
                        activePlayer.pause()
                    } else {
                        activePlayer?.play()
                    }

                    showControls = true
                },
                onSeekBackClick = {
                    val targetPosition = seekBackwardPosition(currentPositionMs)

                    player?.seekTo(targetPosition)
                    currentPositionMs = targetPosition
                    showControls = true
                },
                onSeekForwardClick = {
                    val targetPosition = seekForwardPosition(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                    )

                    player?.seekTo(targetPosition)
                    currentPositionMs = targetPosition
                    showControls = true
                },
                onSliderValueChange = { nextValue ->
                    isScrubbing = true
                    scrubSliderValue = nextValue
                    showControls = true
                },
                onSliderValueChangeFinished = {
                    val targetPosition = sliderValueToPosition(
                        sliderValue = scrubSliderValue,
                        durationMs = durationMs,
                    )

                    player?.seekTo(targetPosition)
                    currentPositionMs = targetPosition
                    isScrubbing = false
                    showControls = true
                },
            )
        }
    }
}

@Preview
@Composable
private fun VideoViewerScreenPreview() {
    GalleryExplorerTheme {
        CompositionLocalProvider(
            LocalVideoPlaybackController provides PreviewVideoPlaybackController(),
        ) {
            Surface {
                VideoViewerScreen(
                    videos = listOf(
                        VolumeFile.Video(
                            file = File("/storage/emulated/0/Movies/clip-1.mp4"),
                        ),
                        VolumeFile.Video(
                            file = File("/storage/emulated/0/Movies/clip-2.mp4"),
                        ),
                    ),
                    selectedIndex = 0,
                    navigateBack = {},
                )
            }
        }
    }
}

internal fun videosFromPaths(videoPaths: List<String>): List<VolumeFile.Video> {
    return videoPaths.map { path ->
        VolumeFile.Video(file = File(path))
    }
}

private class PreviewVideoPlaybackController : VideoPlaybackController {
    override val player = MutableStateFlow<Player?>(null)
    override val sessionState = MutableStateFlow(
        VideoPlaybackSessionState(
            videoPaths = listOf(
                "/storage/emulated/0/Movies/clip-1.mp4",
                "/storage/emulated/0/Movies/clip-2.mp4",
            ),
            selectedIndex = 0,
            currentVideoPath = "/storage/emulated/0/Movies/clip-1.mp4",
            currentVideoTitle = "clip-1.mp4",
        )
    )

    override fun connect() = Unit

    override fun openPlaylist(videoPaths: List<String>, selectedIndex: Int) = Unit

    override fun selectVideo(index: Int) = Unit
}
