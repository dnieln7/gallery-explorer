package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaScreenTarget
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.media.LocalExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.presentation.media.NoOpExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.home.presentation.screen.HomeScreenDestination
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerAction
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerState
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.CONTROLS_AUTO_HIDE_DELAY_MS
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoPlaybackControls
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoSurface
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.positionToSliderValue
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.seekBackwardPosition
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.seekForwardPosition
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.sliderValueToPosition
import java.io.File

/**
 * Voyager destination that shows a vertically swipeable video viewer for a folder-scoped list of videos.
 *
 * @property videoPaths Absolute paths of the videos available in the current folder, preserved in folder order.
 * @property selectedIndex Index of the tapped video that should start playback.
 * @property removableVolumeRootPath Removable volume root captured when the destination was created.
 * @property removableVolumeName Removable volume label used if the destination must be redirected home.
 */
class VideoViewerScreenDestination(
    val videoPaths: List<String>,
    val selectedIndex: Int,
    val removableVolumeRootPath: String? = null,
    val removableVolumeName: String? = null,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = getViewModel<VideoViewerViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        val externalMediaRedirectCoordinator = LocalExternalMediaRedirectCoordinator.current

        LaunchedEffect(videoPaths, selectedIndex) {
            viewModel.onAction(VideoViewerAction.OpenPlaylist(videoPaths, selectedIndex))
        }

        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            viewModel.onAction(VideoViewerAction.PauseForBackground)
        }

        LifecycleEventEffect(Lifecycle.Event.ON_START) {
            viewModel.onAction(VideoViewerAction.ResumeFromBackground)
        }

        VideoViewerScreen(
            videos = remember(videoPaths) { videosFromPaths(videoPaths) },
            state = state,
            player = viewModel.player,
            removableVolumeRootPath = removableVolumeRootPath,
            removableVolumeName = removableVolumeName,
            externalMediaRedirectCoordinator = externalMediaRedirectCoordinator,
            onAction = viewModel::onAction,
            navigateBack = {
                if (navigator.canPop) {
                    navigator.pop()
                } else {
                    navigator.replaceAll(HomeScreenDestination())
                }
            },
        )
    }
}

@Composable
private fun VideoViewerScreen(
    videos: List<VolumeFile.Video>,
    state: VideoViewerState,
    player: Player,
    removableVolumeRootPath: String?,
    removableVolumeName: String?,
    externalMediaRedirectCoordinator: ExternalMediaRedirectCoordinator,
    onAction: (VideoViewerAction) -> Unit,
    navigateBack: () -> Unit,
) {
    if (videos.isEmpty()) {
        return
    }

    val initialPage = state.selectedIndex.coerceIn(0, videos.lastIndex).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(pageCount = { videos.size }, initialPage = initialPage)
    val activePage by remember(state.selectedIndex, pagerState, videos) {
        derivedStateOf {
            state.selectedIndex.takeIf { it in videos.indices } ?: pagerState.settledPage
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val activeVideo by remember(videos, activePage) { derivedStateOf { videos[activePage] } }
    val activeVideoPath = activeVideo.file.absolutePath
    val screenTarget by remember(activeVideoPath, removableVolumeRootPath, removableVolumeName) {
        derivedStateOf {
            ExternalMediaScreenTarget(
                path = activeVideoPath,
                removableVolumeRootPath = removableVolumeRootPath,
                removableVolumeName = removableVolumeName,
            )
        }
    }

    LaunchedEffect(screenTarget) {
        externalMediaRedirectCoordinator.registerTarget(screenTarget)
    }

    DisposableEffect(activeVideoPath) {
        onDispose {
            coroutineScope.launch {
                externalMediaRedirectCoordinator.clearPath(activeVideoPath)
            }
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubSliderValue by remember { mutableFloatStateOf(0f) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        onAction(VideoViewerAction.StopPlayback)
        navigateBack()
    }

    // Keeps the local Compose state in sync with the Player instance. Runs on the non-nullable
    // player directly, removing the null-guard required by the old StateFlow<Player?> approach.
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                isPlaying = player.isPlaying
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.takeIf { it > 0L } ?: 0L
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
        }
    }

    // Applies player-driven selection changes back into the pager.
    LaunchedEffect(state.selectedIndex) {
        val targetPage = state.selectedIndex.takeIf { it in videos.indices } ?: return@LaunchedEffect

        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    // Applies pager-driven selection changes back into the ViewModel.
    LaunchedEffect(pagerState.settledPage) {
        val settledPage = pagerState.settledPage

        if (settledPage in videos.indices && state.selectedIndex != settledPage) {
            onAction(VideoViewerAction.SelectVideo(settledPage))
        }
    }

    // Resets transient overlay state whenever the active page changes.
    LaunchedEffect(activePage) {
        showControls = true
        isScrubbing = false
        scrubSliderValue = 0f
    }

    // Polls playback position while the viewer is active.
    LaunchedEffect(player, isScrubbing) {
        while (true) {
            if (!isScrubbing) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.takeIf { it > 0L } ?: 0L
                isPlaying = player.isPlaying
            }

            delay(PLAYBACK_POSITION_POLL_INTERVAL_MS)
        }
    }

    // Auto-hides controls after a short delay while a video is actively playing.
    LaunchedEffect(showControls, isPlaying, isScrubbing, activePage) {
        if (showControls && isPlaying && !isScrubbing) {
            delay(CONTROLS_AUTO_HIDE_DELAY_MS)
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
                title = state.currentVideoTitle.orEmpty().ifBlank { activeVideo.name },
                isVisible = showControls,
                isPlaying = isPlaying,
                currentPositionMs = displayedPositionMs,
                durationMs = durationMs,
                sliderValue = sliderValue,
                onBackClick = {
                    onAction(VideoViewerAction.StopPlayback)
                    navigateBack()
                },
                onPlayPauseClick = {
                    onAction(VideoViewerAction.TogglePlayPause)
                    showControls = true
                },
                onSeekBackClick = {
                    val targetPosition = seekBackwardPosition(currentPositionMs)
                    onAction(VideoViewerAction.SeekTo(targetPosition))
                    currentPositionMs = targetPosition
                    showControls = true
                },
                onSeekForwardClick = {
                    val targetPosition = seekForwardPosition(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                    )
                    onAction(VideoViewerAction.SeekTo(targetPosition))
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
                    onAction(VideoViewerAction.SeekTo(targetPosition))
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember { androidx.media3.exoplayer.ExoPlayer.Builder(context).build() }

    GalleryExplorerTheme {
        Surface {
            VideoViewerScreen(
                videos = listOf(
                    VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip-1.mp4")),
                    VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip-2.mp4")),
                ),
                state = VideoViewerState(
                    videoPaths = listOf(
                        "/storage/emulated/0/Movies/clip-1.mp4",
                        "/storage/emulated/0/Movies/clip-2.mp4",
                    ),
                    selectedIndex = 0,
                    currentVideoTitle = "clip-1.mp4",
                ),
                player = player,
                removableVolumeRootPath = null,
                removableVolumeName = null,
                externalMediaRedirectCoordinator = NoOpExternalMediaRedirectCoordinator,
                onAction = {},
                navigateBack = {},
            )
        }
    }
}

internal fun videosFromPaths(videoPaths: List<String>): List<VolumeFile.Video> {
    return videoPaths.map { path ->
        VolumeFile.Video(file = File(path))
    }
}

private const val PLAYBACK_POSITION_POLL_INTERVAL_MS = 250L
