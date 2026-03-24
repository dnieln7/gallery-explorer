package xyz.dnieln7.galleryex.feature.viewer.presentation.component

import android.graphics.Color
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import java.io.File

@OptIn(UnstableApi::class)
@Composable
internal fun VideoSurface(
    modifier: Modifier = Modifier,
    video: VolumeFile.Video,
    player: Player?,
    isActive: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.scrim),
    ) {
        if (isActive && player != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        controllerAutoShow = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(Color.BLACK)
                        setBackgroundColor(Color.BLACK)
                        setKeepContentOnPlayerReset(true)
                        this.player = player
                    }
                },
                update = { playerView ->
                    playerView.player = player
                },
            )
        } else {
            VideoPoster(
                modifier = Modifier.fillMaxSize(),
                video = video,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onTap) {
                    detectTapGestures(onTap = { onTap() })
                }
        )
    }
}

@Composable
private fun VideoPoster(modifier: Modifier = Modifier, video: VolumeFile.Video) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(video.file.toUri())
        .crossfade(true)
        .videoFrameMillis(0)
        .build()

    AsyncImage(
        modifier = modifier,
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Fit,
    )
}

@Preview
@Composable
private fun VideoPosterPreview() {
    GalleryExplorerTheme {
        Surface {
            VideoPoster(
                modifier = Modifier.fillMaxSize(),
                video = VolumeFile.Video(
                    file = File("/storage/emulated/0/Movies/clip.mp4"),
                ),
            )
        }
    }
}
