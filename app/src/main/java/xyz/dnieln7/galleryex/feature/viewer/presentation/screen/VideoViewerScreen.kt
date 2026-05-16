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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaScreenTarget
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.media.LocalExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerAction
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerState
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoPlaybackControls
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.VideoSurface
import java.io.File

/**
 * Voyager destination for the video viewer.
 *
 * Acts as the MVI orchestrator: obtains [VideoViewerViewModel], collects [VideoViewerState],
 * maps lifecycle events to actions, and registers the current video path with the external
 * media redirect coordinator. Navigation lambdas are defined here and never exposed to the
 * stateless [VideoViewerScreen].
 *
 * @property videoPaths Absolute paths of the videos available in the current folder, in folder order.
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
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val videos = remember(videoPaths) { videosFromPaths(videoPaths) }

        LaunchedEffect(videoPaths, selectedIndex) {
            viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex))
        }

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            viewModel.onAction(VideoViewerAction.OnResume)
        }

        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
            viewModel.onAction(VideoViewerAction.OnPause)
        }

        VideoViewerScreen(
            videos = videos,
            initialPage = selectedIndex,
            removableVolumeRootPath = removableVolumeRootPath,
            removableVolumeName = removableVolumeName,
            uiState = uiState,
            player = viewModel.player,
            onAction = viewModel::onAction,
            navigateBack = { navigator.pop() },
        )
    }
}

@Composable
private fun VideoViewerScreen(
    videos: List<VolumeFile.Video>,
    initialPage: Int,
    removableVolumeRootPath: String?,
    removableVolumeName: String?,
    uiState: VideoViewerState,
    player: ExoPlayer,
    onAction: (VideoViewerAction) -> Unit,
    navigateBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { videos.size }, initialPage = initialPage)
    val activePage by remember { derivedStateOf { pagerState.settledPage } }
    val currentVideo by remember { derivedStateOf { videos[activePage] } }

    LaunchedEffect(activePage) {
        onAction(VideoViewerAction.OnPageChange(activePage))
    }

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
                userScrollEnabled = !uiState.isScrubbing,
            ) { index ->
                VideoSurface(
                    modifier = Modifier.fillMaxSize(),
                    player = player,
                    isActive = activePage == index,
                    isVideoReady = uiState.isVideoReady,
                    onTap = { onAction(VideoViewerAction.OnTap) },
                )
            }

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = uiState.showControls,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                VideoPlaybackControls(
                    isPlaying = uiState.isPlaying,
                    durationCurrentMs = uiState.durationCurrentMs,
                    durationTotalMs = uiState.durationTotalMs,
                    durationSlider = uiState.durationSlider,
                    onPlayPauseClick = { onAction(VideoViewerAction.OnPlayPauseClick) },
                    onSeekBackClick = { onAction(VideoViewerAction.OnSeekBack) },
                    onSeekForwardClick = { onAction(VideoViewerAction.OnSeekForward) },
                    onSliderValueChange = { onAction(VideoViewerAction.OnSliderValueChange(it)) },
                    onSliderValueChangeFinished = { onAction(VideoViewerAction.OnSliderValueChangeFinished) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun VideoViewerScreenPreview() {
    val context = LocalContext.current
    GalleryExplorerTheme {
        Surface {
            VideoViewerScreen(
                videos = listOf(
                    VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip-1.mp4")),
                    VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip-2.mp4")),
                ),
                initialPage = 0,
                removableVolumeRootPath = null,
                removableVolumeName = null,
                uiState = VideoViewerState(),
                player = remember { ExoPlayer.Builder(context).build() },
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
