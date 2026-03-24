package xyz.dnieln7.galleryex.feature.explorer.presentation.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class ExplorerScreenDestinationTest {
    @Test
    fun `GIVEN a directory path WHEN rebuilding a directory model THEN the original path is preserved`() {
        val path = "/storage/emulated/0/Pictures"

        val directory = directoryFromPath(path)

        assertEquals(path, directory.file.absolutePath)
    }
}
