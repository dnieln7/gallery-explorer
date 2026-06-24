package xyz.dnieln7.galleryex.main.presentation.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaRedirectEvent
import xyz.dnieln7.galleryex.core.framework.extension.toastLong
import xyz.dnieln7.galleryex.core.presentation.media.LocalExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.presentation.media.NoOpExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.core.presentation.util.CollectEventsWithLifeCycle
import xyz.dnieln7.galleryex.feature.home.presentation.screen.HomeScreenDestination
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    internal lateinit var externalMediaRedirectCoordinator: ExternalMediaRedirectCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen().apply {
            setKeepOnScreenCondition { false }
        }

        setContent {
            GalleryExplorerTheme {
                CompositionLocalProvider(
                    LocalExternalMediaRedirectCoordinator provides externalMediaRedirectCoordinator,
                ) {
                    Surface {
                        MainContent(
                            externalMediaRedirectCoordinator = externalMediaRedirectCoordinator,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top-level Voyager host for external-media redirects.
 *
 * The screen tree below this composable can be any Home, Explorer, or Viewer destination. This host
 * is responsible for reacting to global storage changes, because only it can safely replace the full
 * navigation stack when removable storage disappears.
 */
@Composable
private fun MainContent(
    externalMediaRedirectCoordinator: ExternalMediaRedirectCoordinator = NoOpExternalMediaRedirectCoordinator,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Navigator(screen = HomeScreenDestination()) { navigator ->
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            coroutineScope.launch {
                externalMediaRedirectCoordinator.refreshAndVerify()
            }
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
        externalMediaRedirectCoordinator = NoOpExternalMediaRedirectCoordinator,
    )
}
