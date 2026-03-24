package xyz.dnieln7.galleryex.feature.viewer.presentation.component

import kotlin.math.roundToLong

internal const val SeekOffsetMs = 10_000L
internal const val ControlsAutoHideDelayMs = 2_500L

internal fun seekBackwardPosition(currentPositionMs: Long): Long {
    return (currentPositionMs - SeekOffsetMs).coerceAtLeast(0L)
}

internal fun seekForwardPosition(currentPositionMs: Long, durationMs: Long): Long {
    if (durationMs <= 0L) {
        return currentPositionMs.coerceAtLeast(0L)
    }

    return (currentPositionMs + SeekOffsetMs).coerceAtMost(durationMs)
}

internal fun sliderValueToPosition(sliderValue: Float, durationMs: Long): Long {
    if (durationMs <= 0L) {
        return 0L
    }

    return (sliderValue.coerceIn(0f, 1f) * durationMs).roundToLong()
}

internal fun positionToSliderValue(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) {
        return 0f
    }

    return positionMs.coerceIn(0L, durationMs).toFloat() / durationMs.toFloat()
}

internal fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1_000L).toInt()
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
