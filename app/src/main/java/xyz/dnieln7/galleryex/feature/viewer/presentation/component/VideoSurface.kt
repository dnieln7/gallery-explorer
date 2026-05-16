package xyz.dnieln7.galleryex.feature.viewer.presentation.component

import android.graphics.Color
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
internal fun VideoSurface(
    modifier: Modifier = Modifier,
    player: ExoPlayer,
    isActive: Boolean,
    isVideoReady: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.scrim)
            .pointerInput(onTap) {
                detectTapGestures(onTap = { onTap() })
            },
    ) {
        if (isActive) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        controllerAutoShow = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        setKeepContentOnPlayerReset(false)
                        setShutterBackgroundColor(Color.BLACK)
                        setBackgroundColor(Color.BLACK)

                        this.player = player
                    }
                },
            )

            if (!isVideoReady) {
                // Cover the PlayerView with the incoming video's thumbnail until the first
                // decoded frame is on the surface, preventing a flash of the previous video.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
