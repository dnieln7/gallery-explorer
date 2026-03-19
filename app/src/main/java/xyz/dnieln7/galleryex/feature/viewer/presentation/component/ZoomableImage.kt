package xyz.dnieln7.galleryex.feature.viewer.presentation.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import coil.compose.AsyncImage
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import kotlin.math.abs

@Composable
fun ZoomableImage(
    image: VolumeFile.Image,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isFocused) {
        if (!isFocused) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val state = remember(constraints) {
            ZoomableState(constraints.maxWidth, constraints.maxHeight)
        }

        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2f

                                val centerX = state.width / 2f
                                val centerY = state.height / 2f

                                val newOffsetX = (centerX - tapOffset.x)
                                val newOffsetY = (centerY - tapOffset.y)

                                val maxX = (state.width * (scale - 1)) / 2f
                                val maxY = (state.height * (scale - 1)) / 2f

                                offset = Offset(
                                    x = newOffsetX.coerceIn(-maxX, maxX),
                                    y = newOffsetY.coerceIn(-maxY, maxY)
                                )
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)

                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop
                        var lockedToPanZoom = false
                        var pan = Offset.Zero
                        var zoom = 1f

                        do {
                            val event: PointerEvent = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }

                            if (!canceled) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()

                                if (!pastTouchSlop) {
                                    zoom *= zoomChange
                                    pan += panChange

                                    val centroidSize = event.calculateCentroidSize(
                                        useCurrent = false,
                                    )
                                    val zoomMotion = abs(1 - zoom) * centroidSize
                                    val panMotion = pan.getDistance()

                                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                        pastTouchSlop = true
                                        lockedToPanZoom = true
                                    }
                                }

                                if (pastTouchSlop) {
                                    val effectivePan =
                                        if (lockedToPanZoom) panChange else Offset.Zero
                                    val effectiveZoom = if (lockedToPanZoom) zoomChange else 1f

                                    if (effectiveZoom != 1f || effectivePan != Offset.Zero) {
                                        val targetScale = (scale * effectiveZoom).coerceIn(1f, 4f)
                                        val isZooming = effectiveZoom != 1f

                                        val maxX = (state.width * (targetScale - 1)) / 2f
                                        val maxY = (state.height * (targetScale - 1)) / 2f

                                        val newPanChange = effectivePan * targetScale
                                        val targetOffsetX = offset.x + newPanChange.x
                                        val targetOffsetY = offset.y + newPanChange.y

                                        val atLeftEdge = offset.x >= maxX
                                        val atRightEdge = offset.x <= -maxX

                                        val movingFingerRight = effectivePan.x > 0
                                        val movingFingerLeft = effectivePan.x < 0

                                        val pushingPastLeftEdge = atLeftEdge && movingFingerRight
                                        val pushingPastRightEdge = atRightEdge && movingFingerLeft

                                        val consume =
                                            isZooming || (scale > 1f && !(pushingPastLeftEdge || pushingPastRightEdge))

                                        if (consume) {
                                            event.changes.forEach {
                                                if (it.positionChanged()) it.consume()
                                            }
                                        }

                                        scale = targetScale
                                        offset = Offset(
                                            x = targetOffsetX.coerceIn(-maxX, maxX),
                                            y = targetOffsetY.coerceIn(-maxY, maxY)
                                        )
                                    }
                                }
                            }
                        } while (!canceled && event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            model = image.file.toUri(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_broken_image),
        )
    }
}

private class ZoomableState(val width: Int, val height: Int)
