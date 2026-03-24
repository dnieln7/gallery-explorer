package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerScreenDestinationReconstructionTest {
    @Test
    fun `GIVEN ordered image paths WHEN rebuilding image models THEN the original order is preserved`() {
        val imagePaths = listOf(
            "/storage/emulated/0/Pictures/1.jpg",
            "/storage/emulated/0/Pictures/2.jpg",
        )

        val images = imagesFromPaths(imagePaths)

        assertEquals(imagePaths, images.map { it.file.absolutePath })
    }

    @Test
    fun `GIVEN ordered video paths WHEN rebuilding video models THEN the original order is preserved`() {
        val videoPaths = listOf(
            "/storage/emulated/0/Movies/a.mp4",
            "/storage/emulated/0/Movies/b.mp4",
        )

        val videos = videosFromPaths(videoPaths)

        assertEquals(videoPaths, videos.map { it.file.absolutePath })
    }
}
