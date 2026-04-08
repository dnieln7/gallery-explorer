package xyz.dnieln7.galleryex.core.framework.media

import androidx.media3.common.Player
import app.cash.turbine.test
import io.mockk.every
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaRedirectEvent
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaScreenTarget
import xyz.dnieln7.galleryex.core.domain.model.Volume
import xyz.dnieln7.galleryex.core.framework.explorer.Explorer
import xyz.dnieln7.galleryex.core.framework.explorer.resolveVolumeForPath
import xyz.dnieln7.galleryex.core.presentation.text.UIText
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackSessionState
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackController
import xyz.dnieln7.galleryex.testutil.relaxedMockk

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalMediaRedirectCoordinatorTest {
    @Test
    fun `GIVEN a removable target WHEN the volume disappears THEN a single redirect event is emitted`() = runTest {
        val removableRoot = createTempDir(prefix = "usb-volume")
        val removableVolume = createVolume(
            name = "USB drive",
            file = removableRoot,
            isRemovable = true,
        )
        val volumesFlow = MutableStateFlow(listOf(removableVolume))
        val explorer = relaxedMockk<Explorer>()
        var refreshedVolumes = volumesFlow.value

        every { explorer.volumes } returns volumesFlow
        every { explorer.resolveVolumeForPath(any()) } answers {
            volumesFlow.value.resolveVolumeForPath(firstArg())
        }
        every { explorer.refreshVolumes() } answers {
            volumesFlow.value = refreshedVolumes
            refreshedVolumes
        }
        val videoPlaybackController = FakeVideoPlaybackController()

        val coordinator = DefaultExternalMediaRedirectCoordinator(
            explorer = explorer,
            videoPlaybackController = videoPlaybackController,
            scope = backgroundScope,
        )
        val targetPath = File(removableRoot, "Movies/clip.mp4").absolutePath

        coordinator.registerTarget(ExternalMediaScreenTarget(path = targetPath))

        coordinator.events.test {
            refreshedVolumes = emptyList()

            coordinator.refreshAndVerify()

            val event = awaitItem()
            val redirect = event as ExternalMediaRedirectEvent.Redirect
            val message = redirect.message as UIText.FromResourceWithArgs

            assertEquals(R.string.external_media_removed_with_name, message.id)
            assertEquals("USB drive", message.args.first())
            assertEquals(1, videoPlaybackController.stopPlaybackCallCount)

            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN an internal target WHEN volumes change THEN no redirect is emitted`() = runTest {
        val internalRoot = createTempDir(prefix = "internal-volume")
        val internalVolume = createVolume(
            name = "Internal storage",
            file = internalRoot,
            isRemovable = false,
        )
        val volumesFlow = MutableStateFlow(listOf(internalVolume))
        val explorer = relaxedMockk<Explorer>()
        var refreshedVolumes = volumesFlow.value

        every { explorer.volumes } returns volumesFlow
        every { explorer.resolveVolumeForPath(any()) } answers {
            volumesFlow.value.resolveVolumeForPath(firstArg())
        }
        every { explorer.refreshVolumes() } answers {
            volumesFlow.value = refreshedVolumes
            refreshedVolumes
        }
        val videoPlaybackController = FakeVideoPlaybackController()

        val coordinator = DefaultExternalMediaRedirectCoordinator(
            explorer = explorer,
            videoPlaybackController = videoPlaybackController,
            scope = backgroundScope,
        )

        coordinator.registerTarget(
            ExternalMediaScreenTarget(
                path = File(internalRoot, "Pictures/photo.jpg").absolutePath,
            ),
        )

        coordinator.events.test {
            refreshedVolumes = emptyList()

            coordinator.refreshAndVerify()

            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0, videoPlaybackController.stopPlaybackCallCount)
    }

    @Test
    fun `GIVEN an unrelated volume change WHEN the active removable target remains mounted THEN no redirect is emitted`() = runTest {
        val targetRoot = createTempDir(prefix = "usb-target")
        val unrelatedRoot = createTempDir(prefix = "usb-unrelated")
        val targetVolume = createVolume(
            name = "Target USB",
            file = targetRoot,
            isRemovable = true,
        )
        val unrelatedVolume = createVolume(
            name = "Other USB",
            file = unrelatedRoot,
            isRemovable = true,
        )
        val volumesFlow = MutableStateFlow(listOf(targetVolume, unrelatedVolume))
        val explorer = relaxedMockk<Explorer>()

        every { explorer.volumes } returns volumesFlow
        every { explorer.resolveVolumeForPath(any()) } answers {
            volumesFlow.value.resolveVolumeForPath(firstArg())
        }
        every { explorer.refreshVolumes() } answers {
            volumesFlow.value
        }
        val videoPlaybackController = FakeVideoPlaybackController()

        val coordinator = DefaultExternalMediaRedirectCoordinator(
            explorer = explorer,
            videoPlaybackController = videoPlaybackController,
            scope = backgroundScope,
        )

        coordinator.registerTarget(
            ExternalMediaScreenTarget(
                path = File(targetRoot, "Movies/clip.mp4").absolutePath,
            ),
        )

        coordinator.events.test {
            volumesFlow.value = listOf(targetVolume)

            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0, videoPlaybackController.stopPlaybackCallCount)
    }

    @Test
    fun `GIVEN a restored removable screen WHEN its volume is already missing THEN playback is cleared and redirect is emitted immediately`() = runTest {
        val explorer = relaxedMockk<Explorer>()
        val volumesFlow = MutableStateFlow<List<Volume>>(emptyList())
        val videoPlaybackController = FakeVideoPlaybackController()

        every { explorer.volumes } returns volumesFlow
        every { explorer.resolveVolumeForPath(any()) } returns null
        every { explorer.refreshVolumes() } returns emptyList()

        val coordinator = DefaultExternalMediaRedirectCoordinator(
            explorer = explorer,
            videoPlaybackController = videoPlaybackController,
            scope = backgroundScope,
        )

        coordinator.events.test {
            coordinator.registerTarget(
                ExternalMediaScreenTarget(
                    path = "/storage/1234-5678/Movies/clip.mp4",
                    removableVolumeRootPath = "/storage/1234-5678",
                    removableVolumeName = "USB drive",
                ),
            )

            val event = awaitItem() as ExternalMediaRedirectEvent.Redirect
            val message = event.message as UIText.FromResourceWithArgs

            assertEquals(R.string.external_media_removed_with_name, message.id)
            assertEquals("USB drive", message.args.first())
            assertEquals(1, videoPlaybackController.stopPlaybackCallCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createVolume(
        name: String,
        file: File,
        isRemovable: Boolean,
    ): Volume {
        return Volume(
            name = name,
            file = file,
            isRemovable = isRemovable,
        )
    }

    private fun createTempDir(prefix: String): File {
        return kotlin.io.path.createTempDirectory(prefix).toFile()
    }
}

private class FakeVideoPlaybackController : VideoPlaybackController {
    override val player = MutableStateFlow<Player?>(null)
    override val sessionState = MutableStateFlow(VideoPlaybackSessionState())
    var stopPlaybackCallCount = 0
        private set

    override fun connect() = Unit

    override fun openPlaylist(videoPaths: List<String>, selectedIndex: Int) = Unit

    override fun selectVideo(index: Int) = Unit

    override fun stopPlayback() {
        stopPlaybackCallCount += 1
    }
}
