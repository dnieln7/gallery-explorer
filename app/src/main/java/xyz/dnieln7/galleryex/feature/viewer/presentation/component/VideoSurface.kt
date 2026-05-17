package xyz.dnieln7.galleryex.feature.viewer.presentation.component

import android.graphics.Color
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import java.io.File

/**
 * Renders the video surface for a single pager page.
 *
 * When [isActive] is `true`, the [PlayerView] is shown with the given [player] attached.
 * While the player has not yet rendered its first frame ([isVideoReady] is `false`), a
 * first-frame thumbnail loaded from [videoFile] via Coil overlays the player surface,
 * preventing a black flash between video transitions.
 *
 * When [isActive] is `false`, only the first-frame thumbnail of [videoFile] is shown,
 * giving the user a preview of the adjacent video during mid-scroll.
 *
 * @param modifier Modifier applied to the root container.
 * @param player The [ExoPlayer] instance bound to the active page's [PlayerView].
 * @param isActive Whether this surface is the currently settled pager page.
 * @param isVideoReady Whether [Player.Listener.onRenderedFirstFrame] has fired for the current video.
 * @param videoFile The video file for this page, used to load the first-frame thumbnail via Coil.
 * @param onTap Callback invoked when the user taps the surface.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun VideoSurface(
    modifier: Modifier = Modifier,
    player: ExoPlayer,
    isActive: Boolean,
    isVideoReady: Boolean,
    videoFile: File,
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
                // Cover the PlayerView with the cached first-frame thumbnail until the player
                // renders its first real frame, preventing a black flash during transitions.
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = videoFile.toUri(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = videoFile.toUri(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }
}
