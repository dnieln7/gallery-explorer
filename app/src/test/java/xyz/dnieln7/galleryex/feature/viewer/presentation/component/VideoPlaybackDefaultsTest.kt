package xyz.dnieln7.galleryex.feature.viewer.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlaybackDefaultsTest {
    @Test
    fun `GIVEN seek backward and forward actions WHEN near bounds THEN position is clamped to 0 duration`() {
        val rewindResult = seekBackwardPosition(currentPositionMs = 4_000L)
        val fastForwardResult = seekForwardPosition(
            currentPositionMs = 57_000L,
            durationMs = 60_000L,
        )

        assertEquals(0L, rewindResult)
        assertEquals(60_000L, fastForwardResult)
    }

    @Test
    fun `GIVEN slider drag values WHEN converted THEN target playback position is correct`() {
        val result = sliderValueToPosition(
            sliderValue = 0.25f,
            durationMs = 120_000L,
        )

        assertEquals(30_000L, result)
    }
}
