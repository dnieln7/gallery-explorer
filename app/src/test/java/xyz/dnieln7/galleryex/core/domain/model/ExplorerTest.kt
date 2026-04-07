package xyz.dnieln7.galleryex.core.framework.explorer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.dnieln7.galleryex.core.domain.model.Volume
import java.io.File

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ExplorerTest {
    @Test
    fun `GIVEN a refreshed explorer WHEN a new collector starts THEN the latest volume snapshot is replayed`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val explorer = Explorer(context)
        val latestVolumes = explorer.refreshVolumes()

        explorer.volumes.test {
            assertEquals(latestVolumes, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN nested paths WHEN resolving a volume THEN the matching root and removable flag are returned`() {
        val internalRoot = createTempDir(prefix = "internal-volume")
        val removableRoot = createTempDir(prefix = "removable-volume")
        val internalVolume = Volume(
            name = "Internal",
            file = internalRoot,
            isRemovable = false,
        )
        val removableVolume = Volume(
            name = "USB drive",
            file = removableRoot,
            isRemovable = true,
        )

        val internalResult = listOf(internalVolume, removableVolume).resolveVolumeForPath(
            File(internalRoot, "Pictures/photo.jpg").absolutePath,
        )
        val removableResult = listOf(internalVolume, removableVolume).resolveVolumeForPath(
            File(removableRoot, "Movies/clip.mp4").absolutePath,
        )

        assertEquals(internalVolume, internalResult)
        assertEquals(false, internalResult?.isRemovable)
        assertEquals(removableVolume, removableResult)
        assertEquals(true, removableResult?.isRemovable)
    }

    private fun createTempDir(prefix: String): File {
        return kotlin.io.path.createTempDirectory(prefix).toFile()
    }
}
