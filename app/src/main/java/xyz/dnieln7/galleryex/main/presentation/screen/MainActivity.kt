package xyz.dnieln7.galleryex.main.presentation.screen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.home.presentation.screen.HomeScreenDestination
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackRestoreRequest
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.DefaultVideoPlaybackController
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.LocalVideoPlaybackController
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackRestoreIntent
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackSessionStore
import xyz.dnieln7.galleryex.feature.viewer.presentation.screen.VideoViewerScreenDestination
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    internal lateinit var videoPlaybackController: DefaultVideoPlaybackController

    @Inject
    internal lateinit var videoPlaybackSessionStore: VideoPlaybackSessionStore

    private val restoreRequests = MutableSharedFlow<VideoPlaybackRestoreRequest>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialRestoreRequest = consumeRestoreRequest(intent)
        installSplashScreen().apply {
            setKeepOnScreenCondition { false }
        }

        setContent {
            GalleryExplorerTheme {
                CompositionLocalProvider(
                    LocalVideoPlaybackController provides videoPlaybackController,
                ) {
                    Surface {
                        MainContent(
                            initialRestoreRequest = initialRestoreRequest,
                            restoreRequests = restoreRequests,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        consumeRestoreRequest(intent)?.let(restoreRequests::tryEmit)
    }

    private fun consumeRestoreRequest(intent: Intent?): VideoPlaybackRestoreRequest? {
        val restoreRequest = VideoPlaybackRestoreIntent.consumeRestoreRequest(
            intent = intent,
            sessionStore = videoPlaybackSessionStore,
        )

        setIntent(VideoPlaybackRestoreIntent.clearFromIntent(intent))

        return restoreRequest
    }
}

@Composable
private fun MainContent(
    initialRestoreRequest: VideoPlaybackRestoreRequest?,
    restoreRequests: Flow<VideoPlaybackRestoreRequest>,
) {
    Navigator(screen = initialRestoreRequest.toScreenDestination()) { navigator ->
        LaunchedEffect(navigator, restoreRequests) {
            restoreRequests.collect { restoreRequest ->
                navigator.replaceAll(restoreRequest.toScreenDestination())
            }
        }

        CurrentScreen()
    }
}

@Preview
@Composable
private fun MainContentPreview() {
    MainContent(
        initialRestoreRequest = null,
        restoreRequests = MutableSharedFlow(),
    )
}

private fun VideoPlaybackRestoreRequest?.toScreenDestination() = this?.let {
    VideoViewerScreenDestination(
        videoPaths = videoPaths,
        selectedIndex = selectedIndex,
    )
} ?: HomeScreenDestination()
