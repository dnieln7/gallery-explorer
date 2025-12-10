package xyz.dnieln7.galleryex.main.presentation.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cafe.adriel.voyager.navigator.Navigator
import dagger.hilt.android.AndroidEntryPoint
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.home.presentation.screen.HomeScreenDestination

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        installSplashScreen().apply {
            setKeepOnScreenCondition { false }
        }

        setContent {
            GalleryExplorerTheme {
                Surface {
                    MainContent()
                }
            }
        }
    }
}

@Composable
private fun MainContent() {
    Navigator(
        screen = HomeScreenDestination(),
    )
}

@Preview
@Composable
private fun MainContentPreview() {
    MainContent()
}
