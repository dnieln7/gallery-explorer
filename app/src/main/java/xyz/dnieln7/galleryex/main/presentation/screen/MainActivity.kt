package xyz.dnieln7.galleryex.main.presentation.screen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import xyz.dnieln7.galleryex.core.framework.extension.toastLong
import xyz.dnieln7.galleryex.core.presentation.util.CollectEventsWithLifeCycle
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.home.presentation.screen.HomeScreenDestination
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackRestoreRequest
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.DefaultVideoPlaybackController
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.LocalVideoPlaybackController
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackRestoreIntent
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackSessionStore
import xyz.dnieln7.galleryex.feature.viewer.presentation.screen.VideoViewerScreenDestination
import xyz.dnieln7.galleryex.main.framework.ExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.main.framework.ExternalMediaRedirectEvent
import xyz.dnieln7.galleryex.main.framework.LocalExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.main.framework.NoOpExternalMediaRedirectCoordinator
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    internal lateinit var videoPlaybackController: DefaultVideoPlaybackController

    @Inject
    internal lateinit var videoPlaybackSessionStore: VideoPlaybackSessionStore

    @Inject
    internal lateinit var externalMediaRedirectCoordinator: ExternalMediaRedirectCoordinator

    private val restoreRequests = MutableSharedFlow<VideoPlaybackRestoreRequest>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen().apply {
            setKeepOnScreenCondition { false }
        }

        val initialRestoreRequest = consumeRestoreRequest(intent)

        setContent {
            GalleryExplorerTheme {
                CompositionLocalProvider(
                    LocalVideoPlaybackController provides videoPlaybackController,
                    LocalExternalMediaRedirectCoordinator provides externalMediaRedirectCoordinator,
                ) {
                    Surface {
                        MainContent(
                            initialRestoreRequest = initialRestoreRequest,
                            restoreRequests = restoreRequests,
                            externalMediaRedirectCoordinator = externalMediaRedirectCoordinator,
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

/**
 * Top-level Voyager host for restore handling and external-media redirects.
 *
 * The screen tree below this composable can be any Home, Explorer, or Viewer destination. This host
 * is responsible for reacting to global storage changes, because only it can safely replace the full
 * navigation stack when removable storage disappears.
 */
@Composable
private fun MainContent(
    initialRestoreRequest: VideoPlaybackRestoreRequest?,
    restoreRequests: Flow<VideoPlaybackRestoreRequest>,
    externalMediaRedirectCoordinator: ExternalMediaRedirectCoordinator = NoOpExternalMediaRedirectCoordinator,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Navigator(screen = initialRestoreRequest.toScreenDestination()) { navigator ->
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            coroutineScope.launch {
                externalMediaRedirectCoordinator.refreshAndVerify()
            }
        }

        CollectEventsWithLifeCycle(events = restoreRequests) { restoreRequest ->
            navigator.replaceAll(restoreRequest.toScreenDestination())
        }

        CollectEventsWithLifeCycle(events = externalMediaRedirectCoordinator.events) { event ->
            when (event) {
                is ExternalMediaRedirectEvent.Redirect -> {
                    navigator.replaceAll(HomeScreenDestination())
                    context.toastLong(event.message.asString(context))
                }
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
        externalMediaRedirectCoordinator = NoOpExternalMediaRedirectCoordinator,
    )
}

private fun VideoPlaybackRestoreRequest?.toScreenDestination() = this?.let {
    VideoViewerScreenDestination(
        videoPaths = videoPaths,
        selectedIndex = selectedIndex,
        removableVolumeRootPath = null,
        removableVolumeName = null,
    )
} ?: HomeScreenDestination()
