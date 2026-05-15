@file:OptIn(ExperimentalCoroutinesApi::class)

package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import androidx.media3.exoplayer.ExoPlayer
import app.cash.turbine.test
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoViewerAction
import xyz.dnieln7.galleryex.testutil.MainDispatcherRule
import xyz.dnieln7.galleryex.testutil.relaxedMockk
import xyz.dnieln7.galleryex.testutil.verifyOnce
import java.io.File

@RunWith(RobolectricTestRunner::class)
class VideoViewerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val player = relaxedMockk<ExoPlayer>()

    private lateinit var viewModel: VideoViewerViewModel

    private val videos = listOf(
        VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip-1.mp4")),
        VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip-2.mp4")),
        VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip-3.mp4")),
    )

    @Test
    fun `GIVEN videos and a selected index WHEN Initialize is called THEN player loads the selected video and state reflects the active page`() = runTest {
        viewModel = VideoViewerViewModel(player)

        viewModel.uiState.test {
            awaitItem() // Initial default state

            viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))

            awaitItem().let { state ->
                state.activePage.shouldBeEqualTo(1)
                state.showControls.shouldBeTrue()
                state.isScrubbing.shouldBeFalse()
                state.durationCurrentMs.shouldBeEqualTo(0L)
                state.durationTotalMs.shouldBeEqualTo(0L)
            }

            cancelAndIgnoreRemainingEvents()
        }

        verifyOnce { player.setMediaItem(any()) }
        verifyOnce { player.prepare() }
        verifyOnce { player.seekTo(0L) }
        verifyOnce { player.play() }
    }

    @Test
    fun `GIVEN already initialized viewmodel WHEN Initialize is called again THEN player is not reset`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))

        viewModel.uiState.test {
            awaitItem() // State after first Initialize

            viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))

            // No new emission because Initialize is a no-op the second time
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        // setMediaItem was called exactly once by the first Initialize
        verify(exactly = 1) { player.setMediaItem(any()) }
    }

    @Test
    fun `GIVEN initialized viewmodel WHEN OnPageChange is called with a new page THEN player loads the new video and state resets`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))
        clearMocks(player, answers = false)

        viewModel.uiState.test {
            awaitItem() // State after Initialize (activePage = 1)

            viewModel.onAction(VideoViewerAction.OnPageChange(2))

            awaitItem().let { state ->
                state.activePage.shouldBeEqualTo(2)
                state.showControls.shouldBeTrue()
                state.isScrubbing.shouldBeFalse()
                state.durationCurrentMs.shouldBeEqualTo(0L)
                state.durationTotalMs.shouldBeEqualTo(0L)
            }

            cancelAndIgnoreRemainingEvents()
        }

        verifyOnce { player.setMediaItem(any()) }
        verifyOnce { player.prepare() }
    }

    @Test
    fun `GIVEN initialized viewmodel WHEN OnPageChange is called with the same page THEN player is not reset`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))
        clearMocks(player, answers = false)

        viewModel.onAction(VideoViewerAction.OnPageChange(1))

        verify(exactly = 0) { player.setMediaItem(any()) }
    }

    @Test
    fun `GIVEN playing video WHEN OnPlayPauseClick is called THEN player pauses`() = runTest {
        every { player.isPlaying } returns true

        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))

        viewModel.onAction(VideoViewerAction.OnPlayPauseClick)

        verifyOnce { player.pause() }
    }

    @Test
    fun `GIVEN paused video WHEN OnPlayPauseClick is called THEN player plays`() = runTest {
        every { player.isPlaying } returns false

        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))
        clearMocks(player, answers = false)

        viewModel.onAction(VideoViewerAction.OnPlayPauseClick)

        verifyOnce { player.play() }
    }

    @Test
    fun `GIVEN active video WHEN OnSeekBack is called THEN player seeks back and controls are shown`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))
        viewModel.onAction(VideoViewerAction.OnTap) // Hide controls so the next showControls = true is a real state change

        viewModel.uiState.test {
            awaitItem() // State with showControls = false

            viewModel.onAction(VideoViewerAction.OnSeekBack)

            awaitItem().showControls.shouldBeTrue()
            cancelAndIgnoreRemainingEvents()
        }

        verifyOnce { player.seekBack() }
    }

    @Test
    fun `GIVEN active video WHEN OnSeekForward is called THEN player seeks forward and controls are shown`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))
        viewModel.onAction(VideoViewerAction.OnTap) // Hide controls so the next showControls = true is a real state change

        viewModel.uiState.test {
            awaitItem() // State with showControls = false

            viewModel.onAction(VideoViewerAction.OnSeekForward)

            awaitItem().showControls.shouldBeTrue()
            cancelAndIgnoreRemainingEvents()
        }

        verifyOnce { player.seekForward() }
    }

    @Test
    fun `GIVEN active video WHEN OnSliderValueChange is called THEN state reflects scrubbing`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))

        viewModel.uiState.test {
            awaitItem() // State after Initialize

            viewModel.onAction(VideoViewerAction.OnSliderValueChange(0.5f))

            awaitItem().let { state ->
                state.isScrubbing.shouldBeTrue()
                state.scrubSliderValue.shouldBeEqualTo(0.5f)
                state.durationSlider.shouldBeEqualTo(0.5f)
                state.showControls.shouldBeTrue()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN playing video WHEN OnSliderValueChange is called THEN position polling does not overwrite the scrub position`() = runTest {
        // Simulate the player reporting it is playing so the polling condition would normally be true
        every { player.isPlaying } returns true
        // Simulate a non-zero playback position that polling would otherwise write to state
        every { player.currentPosition } returns 60_000L

        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))

        viewModel.uiState.test {
            awaitItem() // State after Initialize

            viewModel.onAction(VideoViewerAction.OnSliderValueChange(0.25f))

            awaitItem().let { state ->
                // Polling must have been suspended — scrub values must be intact
                state.isScrubbing.shouldBeTrue()
                state.scrubSliderValue.shouldBeEqualTo(0.25f)
                state.durationSlider.shouldBeEqualTo(0.25f)
            }

            // No further emissions means polling did not fire and overwrite durationSlider
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN scrubbing state WHEN OnSliderValueChangeFinished is called THEN player seeks to the target position and scrubbing ends`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))
        viewModel.onAction(VideoViewerAction.OnSliderValueChange(0.25f))
        clearMocks(player, answers = false)

        viewModel.uiState.test {
            awaitItem() // Scrubbing state

            viewModel.onAction(VideoViewerAction.OnSliderValueChangeFinished)

            awaitItem().let { state ->
                state.isScrubbing.shouldBeFalse()
                state.showControls.shouldBeTrue()
            }

            cancelAndIgnoreRemainingEvents()
        }

        verifyOnce { player.seekTo(any()) }
    }

    @Test
    fun `GIVEN controls visible WHEN OnTap is called THEN controls are hidden`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))

        viewModel.uiState.test {
            awaitItem() // State after Initialize (showControls = true)

            viewModel.onAction(VideoViewerAction.OnTap)

            awaitItem().showControls.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN controls hidden WHEN OnTap is called THEN controls are shown`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))
        viewModel.onAction(VideoViewerAction.OnTap) // Hide controls

        viewModel.uiState.test {
            awaitItem() // State with showControls = false

            viewModel.onAction(VideoViewerAction.OnTap)

            awaitItem().showControls.shouldBeTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN screen resuming WHEN OnResume is called THEN player plays`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))
        clearMocks(player, answers = false)

        viewModel.onAction(VideoViewerAction.OnResume)

        verifyOnce { player.play() }
    }

    @Test
    fun `GIVEN screen going to background WHEN OnPause is called THEN player pauses`() = runTest {
        viewModel = VideoViewerViewModel(player)
        viewModel.onAction(VideoViewerAction.Initialize(videos, selectedIndex = 1))

        viewModel.onAction(VideoViewerAction.OnPause)

        verifyOnce { player.pause() }
    }
}
