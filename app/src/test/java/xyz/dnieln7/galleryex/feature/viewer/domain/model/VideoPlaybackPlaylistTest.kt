package xyz.dnieln7.galleryex.feature.viewer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class VideoPlaybackPlaylistTest {
    @Test
    fun `GIVEN invalid paths around the selected index WHEN creating a playlist THEN missing files are skipped and the closest valid selection is kept`() {
        val firstVideo = createTempVideoFile("first.mp4")
        val thirdVideo = createTempVideoFile("third.mp4")

        val playlist = createVideoPlaybackPlaylist(
            videoPaths = listOf(
                firstVideo.absolutePath,
                "/missing/second.mp4",
                thirdVideo.absolutePath,
            ),
            selectedIndex = 1,
        )

        assertEquals(
            listOf(firstVideo.absolutePath, thirdVideo.absolutePath),
            playlist?.videoPaths,
        )
        assertEquals(1, playlist?.selectedIndex)
    }

    @Test
    fun `GIVEN no valid files WHEN creating a playlist THEN null is returned`() {
        val playlist = createVideoPlaybackPlaylist(
            videoPaths = listOf(
                "/missing/first.mp4",
                "/missing/second.mp4",
            ),
            selectedIndex = 0,
        )

        assertNull(playlist)
    }

    private fun createTempVideoFile(fileName: String): File {
        return File.createTempFile(fileName.substringBefore('.'), ".mp4").apply {
            deleteOnExit()
        }
    }
}
