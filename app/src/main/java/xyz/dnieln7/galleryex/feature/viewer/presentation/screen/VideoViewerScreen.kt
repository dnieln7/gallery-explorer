@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaScreenTarget
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.media.LocalExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoPlaybackControls
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoSurface
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.positionToSliderValue
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

        val videos = remember(videoPaths) { videosFromPaths(videoPaths) }

        VideoViewerScreen(
            videos = videos,
            selectedIndex = selectedIndex,
            removableVolumeRootPath = removableVolumeRootPath,
            removableVolumeName = removableVolumeName,
            navigateBack = { navigator.pop() },
        )
    }
}

@Composable
private fun VideoViewerScreen(
    videos: List<VolumeFile.Video>,
    selectedIndex: Int,
    removableVolumeRootPath: String?,
    removableVolumeName: String?,
    navigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Pager
    val pagerState = rememberPagerState(pageCount = { videos.size }, initialPage = selectedIndex)
    val activePage by remember {
        derivedStateOf { pagerState.settledPage }
    }
    val currentVideo by remember {
        derivedStateOf { videos[activePage] }
    }
    // Pager

    // External Media Redirect Coordinator
    val externalMediaRedirectCoordinator = LocalExternalMediaRedirectCoordinator.current

    val currentVideoPath = currentVideo.file.absolutePath
    val screenTarget by remember(currentVideoPath, removableVolumeRootPath, removableVolumeName) {
        derivedStateOf {
            ExternalMediaScreenTarget(
                path = currentVideoPath,
                removableVolumeRootPath = removableVolumeRootPath,
                removableVolumeName = removableVolumeName,
            )
        }
    }

    LaunchedEffect(screenTarget) {
        externalMediaRedirectCoordinator.registerTarget(screenTarget)
    }

    DisposableEffect(currentVideoPath) {
        onDispose {
            coroutineScope.launch {
                externalMediaRedirectCoordinator.clearPath(currentVideoPath)
            }
        }
    }
    // External Media Redirect Coordinator

    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubSliderValue by remember { mutableFloatStateOf(0f) }

    var durationTotalMs by remember { mutableLongStateOf(0L) }
    var durationCurrentRawMs by remember { mutableLongStateOf(0L) }
    val durationCurrentMs by remember {
        derivedStateOf {
            if (isScrubbing) {
                sliderValueToPosition(
                    sliderValue = scrubSliderValue,
                    durationMs = durationTotalMs,
                )
            } else {
                durationCurrentRawMs
            }
        }
    }
    val durationSlider by remember {
        derivedStateOf {
            if (isScrubbing) {
                scrubSliderValue
            } else {
                positionToSliderValue(
                    positionMs = durationCurrentRawMs,
                    durationMs = durationTotalMs,
                )
            }
        }
    }

    // ExoPlayer
    val player = remember {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(PLAYER_SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(PLAYER_SEEK_INCREMENT_MS)
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_ONE }
    }

    // Listener for Exoplayer events
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                isPlaying = player.isPlaying
                durationTotalMs = player.duration.takeIf { it > 0L } ?: 0L
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Change playing video and reset playback state when the active page changes.
    LaunchedEffect(activePage) {
        val video = videos[activePage]

        player.stop()
        player.setMediaItem(MediaItem.fromUri(video.file.toUri()))
        player.prepare()
        player.seekTo(0L)
        player.play()

        showControls = true
        isScrubbing = false
        scrubSliderValue = 0f
        durationCurrentRawMs = 0L
        durationTotalMs = 0L
    }

    // Play when screen is active
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        player.play()
    }

    // Pause when screen is inactive
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        player.pause()
    }

    // Poll current position and update the slider value.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            durationCurrentRawMs = player.currentPosition.coerceAtLeast(0L)
            delay(POSITION_POLLING_INTERVAL_MS)
        }
    }

    // Auto-hides controls after a short delay while a video is actively playing.
    LaunchedEffect(showControls, isPlaying, isScrubbing, activePage) {
        if (showControls && isPlaying && !isScrubbing) {
            delay(CONTROLS_AUTO_HIDE_DELAY_MS)
            showControls = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Text(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        text = currentVideo.name,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        },
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
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

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                VideoPlaybackControls(
                    isPlaying = isPlaying,
                    durationCurrentMs = durationCurrentMs,
                    durationTotalMs = durationTotalMs,
                    durationSlider = durationSlider,
                    onPlayPauseClick = {
//                        shouldPlay = !shouldPlay
                        if (isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                        showControls = true
                    },
                    onSeekBackClick = {
                        player.seekBack()
                        showControls = true
                    },
                    onSeekForwardClick = {
                        player.seekForward()
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
                            durationMs = durationTotalMs,
                        )

                        player.seekTo(targetPosition)

                        isScrubbing = false
                        showControls = true
                    },
                )
            }
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
                    VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip-1.mp4")),
                    VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip-2.mp4")),
                ),
                selectedIndex = 0,
                removableVolumeRootPath = null,
                removableVolumeName = null,
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

private const val PLAYER_SEEK_INCREMENT_MS = 10_000L
private const val CONTROLS_AUTO_HIDE_DELAY_MS: Long = 2_500L
private const val POSITION_POLLING_INTERVAL_MS: Long = 500L
