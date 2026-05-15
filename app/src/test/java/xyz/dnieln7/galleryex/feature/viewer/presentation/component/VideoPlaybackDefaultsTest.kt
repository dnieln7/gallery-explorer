package xyz.dnieln7.galleryex.feature.viewer.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlaybackDefaultsTest {
    @Test
    fun `GIVEN slider drag values WHEN converted THEN target playback position is correct`() {
        val result = sliderValueToPosition(
            sliderValue = 0.25f,
            durationMs = 120_000L,
        )

        assertEquals(30_000L, result)
    }
}
