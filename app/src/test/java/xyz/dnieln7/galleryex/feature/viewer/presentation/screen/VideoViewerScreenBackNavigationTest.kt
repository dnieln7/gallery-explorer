package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackSessionState
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackController

class VideoViewerScreenBackNavigationTest {
    @Test
    fun `GIVEN the viewer back action WHEN invoked THEN playback stops before navigation`() {
        val events = mutableListOf<String>()
        val videoPlaybackController = FakeVideoPlaybackController(
            onStopPlayback = {
                events += "stop"
            },
        )
        val navigateBack = {
            events += "navigate"
        }

        val action = stopPlaybackAndNavigateBack(
            videoPlaybackController = videoPlaybackController,
            navigateBack = navigateBack,
        )

        action()

        assertEquals(
            listOf("stop", "navigate"),
            events,
        )
    }
}

private class FakeVideoPlaybackController(
    private val onStopPlayback: () -> Unit,
) : VideoPlaybackController {
    override val player = MutableStateFlow<Player?>(null)
    override val sessionState = MutableStateFlow(VideoPlaybackSessionState())

    override fun connect() = Unit

    override fun openPlaylist(videoPaths: List<String>, selectedIndex: Int) = Unit

    override fun selectVideo(index: Int) = Unit

    override fun stopPlayback() {
        onStopPlayback()
    }
}
