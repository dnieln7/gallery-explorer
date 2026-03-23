package xyz.dnieln7.galleryex.core.domain.model

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VolumeFileTest {
    @Test
    fun `GIVEN a directory WHEN fromFile is called THEN it returns a Directory`() {
        val directory = createTempDir(prefix = "volume-file-directory")

        val result = VolumeFile.fromFile(directory)

        assertTrue(result is VolumeFile.Directory)

        directory.deleteRecursively()
    }

    @Test
    fun `GIVEN a jpg file WHEN fromFile is called THEN it returns an Image`() {
        val imageFile = createTempFile(prefix = "volume-file-image", suffix = ".jpg")

        val result = VolumeFile.fromFile(imageFile)

        assertTrue(result is VolumeFile.Image)

        imageFile.delete()
    }

    @Test
    fun `GIVEN a mp4 file WHEN fromFile is called THEN it returns a Video`() {
        val videoFile = createTempFile(prefix = "volume-file-video", suffix = ".mp4")

        val result = VolumeFile.fromFile(videoFile)

        assertTrue(result is VolumeFile.Video)

        videoFile.delete()
    }

    @Test
    fun `GIVEN a txt file WHEN fromFile is called THEN it returns Other`() {
        val otherFile = createTempFile(prefix = "volume-file-other", suffix = ".txt")

        val result = VolumeFile.fromFile(otherFile)

        assertTrue(result is VolumeFile.Other)

        otherFile.delete()
    }

    @Test
    fun `GIVEN a mixed directory WHEN children is accessed THEN it includes video files`() {
        val directory = createTempDir(prefix = "volume-file-children")
        createTempFile(prefix = "child-image", suffix = ".jpg", directory = directory)
        createTempFile(prefix = "child-video", suffix = ".mp4", directory = directory)
        createTempFile(prefix = "child-other", suffix = ".txt", directory = directory)

        val result = VolumeFile.Directory(file = directory).children

        assertTrue(result.any { it is VolumeFile.Image })
        assertTrue(result.any { it is VolumeFile.Video })
        assertTrue(result.any { it is VolumeFile.Other })

        directory.deleteRecursively()
    }

    private fun createTempFile(prefix: String, suffix: String, directory: File? = null): File {
        return kotlin.io.path.createTempFile(
            directory = directory?.toPath(),
            prefix = prefix,
            suffix = suffix,
        ).toFile()
    }

    private fun createTempDir(prefix: String): File {
        return kotlin.io.path.createTempDirectory(prefix).toFile()
    }
}
