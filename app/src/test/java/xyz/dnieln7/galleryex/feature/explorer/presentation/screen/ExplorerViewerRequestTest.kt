package xyz.dnieln7.galleryex.feature.explorer.presentation.screen

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import java.io.File

class ExplorerViewerRequestTest {
    @Test
    fun `GIVEN mixed folder content WHEN a video is selected THEN only videos are passed and selectedIndex matches the tapped video`() {
        val selectedVideo = VolumeFile.Video(file = File("/storage/emulated/0/Movies/second.mp4"))
        val files = listOf(
            VolumeFile.Image(file = File("/storage/emulated/0/Pictures/1.jpg")),
            VolumeFile.Video(file = File("/storage/emulated/0/Movies/first.mp4")),
            VolumeFile.Other(file = File("/storage/emulated/0/Documents/readme.txt")),
            selectedVideo,
        )

        val request = createVideoViewerRequest(
            files = files,
            selectedVideo = selectedVideo,
        )

        assertEquals(2, request.videoPaths.size)
        assertEquals(
            listOf(
                "/storage/emulated/0/Movies/first.mp4",
                "/storage/emulated/0/Movies/second.mp4",
            ),
            request.videoPaths,
        )
        assertEquals(1, request.selectedIndex)
    }

    @Test
    fun `GIVEN ordered videos in a folder WHEN navigating THEN pager order matches folder order`() {
        val firstVideo = VolumeFile.Video(file = File("/storage/emulated/0/Movies/a.mp4"))
        val secondVideo = VolumeFile.Video(file = File("/storage/emulated/0/Movies/b.mp4"))
        val thirdVideo = VolumeFile.Video(file = File("/storage/emulated/0/Movies/c.mp4"))
        val files = listOf(
            VolumeFile.Image(file = File("/storage/emulated/0/Pictures/cover.jpg")),
            firstVideo,
            secondVideo,
            thirdVideo,
        )

        val request = createVideoViewerRequest(
            files = files,
            selectedVideo = secondVideo,
        )

        assertEquals(
            listOf(
                "/storage/emulated/0/Movies/a.mp4",
                "/storage/emulated/0/Movies/b.mp4",
                "/storage/emulated/0/Movies/c.mp4",
            ),
            request.videoPaths,
        )
        assertEquals(1, request.selectedIndex)
    }

    @Test
    fun `GIVEN mixed folder content WHEN an image is selected THEN only image paths are passed and selectedIndex matches the tapped image`() {
        val selectedImage = VolumeFile.Image(file = File("/storage/emulated/0/Pictures/2.jpg"))
        val files = listOf(
            VolumeFile.Video(file = File("/storage/emulated/0/Movies/clip.mp4")),
            VolumeFile.Image(file = File("/storage/emulated/0/Pictures/1.jpg")),
            VolumeFile.Other(file = File("/storage/emulated/0/Documents/readme.txt")),
            selectedImage,
        )

        val request = createImageViewerRequest(
            files = files,
            selectedImage = selectedImage,
        )

        assertEquals(
            listOf(
                "/storage/emulated/0/Pictures/1.jpg",
                "/storage/emulated/0/Pictures/2.jpg",
            ),
            request.imagePaths,
        )
        assertEquals(1, request.selectedIndex)
    }
}
