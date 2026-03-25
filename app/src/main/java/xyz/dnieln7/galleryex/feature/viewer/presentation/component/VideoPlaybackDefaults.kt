package xyz.dnieln7.galleryex.feature.viewer.presentation.component

import kotlin.math.roundToLong

internal fun seekBackwardPosition(currentPositionMs: Long): Long {
    return (currentPositionMs - SEEK_OFFSET_MS).coerceAtLeast(0L)
}

internal fun seekForwardPosition(currentPositionMs: Long, durationMs: Long): Long {
    if (durationMs <= 0L) {
        return currentPositionMs.coerceAtLeast(0L)
    }

    return (currentPositionMs + SEEK_OFFSET_MS).coerceAtMost(durationMs)
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
    val totalSeconds = (positionMs.coerceAtLeast(0L) / MILLISECONDS_PER_SECOND).toInt()
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private const val MILLISECONDS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3_600
private const val SEEK_OFFSET_MS = 10_000L
internal const val CONTROLS_AUTO_HIDE_DELAY_MS: Long = 2_500L
