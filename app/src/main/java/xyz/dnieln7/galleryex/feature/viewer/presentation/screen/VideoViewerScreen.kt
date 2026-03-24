package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.ControlsAutoHideDelayMs
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoPlaybackControls
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoSurface
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.positionToSliderValue
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.seekBackwardPosition
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.seekForwardPosition
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.sliderValueToPosition
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

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
    val context = LocalContext.current

    val initialPage = selectedIndex.coerceIn(0, videos.lastIndex)
    val pagerState = rememberPagerState(pageCount = { videos.size }, initialPage = initialPage)

    val activePage by remember(pagerState) { derivedStateOf { pagerState.settledPage } }
    val activeVideo by remember(videos, activePage) { derivedStateOf { videos[activePage] } }
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    var isScreenActive by remember { mutableStateOf(true) }
    var shouldPlay by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubSliderValue by remember { mutableFloatStateOf(0f) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isScreenActive = true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        isScreenActive = false
    }

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
            player.release()
        }
    }

    LaunchedEffect(activePage) {
        val video = videos[activePage]

        player.setMediaItem(MediaItem.fromUri(video.file.toUri()))
        player.prepare()
        player.seekTo(0L)

        shouldPlay = true
        showControls = true
        isScrubbing = false
        scrubSliderValue = 0f
        currentPositionMs = 0L
        durationMs = 0L
    }

    LaunchedEffect(isScreenActive, shouldPlay) {
        if (isScreenActive && shouldPlay) {
            player.play()
        } else {
            player.pause()
        }
    }

    LaunchedEffect(player, isScrubbing) {
        while (true) {
            if (!isScrubbing) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.takeIf { it > 0L } ?: 0L
                isPlaying = player.isPlaying
            }

            delay(250L)
        }
    }

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
                title = activeVideo.name,
                isVisible = showControls,
                isPlaying = isPlaying,
                currentPositionMs = displayedPositionMs,
                durationMs = durationMs,
                sliderValue = sliderValue,
                onBackClick = navigateBack,
                onPlayPauseClick = {
                    shouldPlay = !shouldPlay
                    showControls = true
                },
                onSeekBackClick = {
                    val targetPosition = seekBackwardPosition(currentPositionMs)

                    player.seekTo(targetPosition)
                    currentPositionMs = targetPosition
                    showControls = true
                },
                onSeekForwardClick = {
                    val targetPosition = seekForwardPosition(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                    )

                    player.seekTo(targetPosition)
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

                    player.seekTo(targetPosition)
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

internal fun videosFromPaths(videoPaths: List<String>): List<VolumeFile.Video> {
    return videoPaths.map { path ->
        VolumeFile.Video(file = File(path))
    }
}
