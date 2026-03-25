package xyz.dnieln7.galleryex.feature.explorer.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.component.VerticalSpacer
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.core.presentation.theme.LargeTileShape
import java.io.File

@Composable
fun VolumeFileTile(modifier: Modifier = Modifier, file: VolumeFile, onClick: () -> Unit) {
    val icon = remember(file) {
        when (file) {
            is VolumeFile.Directory -> Icons.Rounded.Folder
            is VolumeFile.Image -> Icons.Rounded.Image
            is VolumeFile.Video -> Icons.Rounded.Videocam
            is VolumeFile.Other -> Icons.Rounded.QuestionMark
        }
    }
    val isInteractive = file is VolumeFile.Directory || file is VolumeFile.Image || file is VolumeFile.Video
    val isMedia = file is VolumeFile.Image || file is VolumeFile.Video

    Column(
        modifier = modifier.then(
            if (isInteractive) Modifier.clickable { onClick() } else Modifier,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(LargeTileShape)
                .background(
                    when (file) {
                        is VolumeFile.Image,
                        is VolumeFile.Video,
                            -> Color.Transparent

                        is VolumeFile.Other -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (file) {
                is VolumeFile.Image -> {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = file.file.toUri(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(R.drawable.ic_broken_image),
                    )
                }

                is VolumeFile.Video -> {
                    VideoThumbnail(file = file)
                }

                else -> {
                    Icon(
                        modifier = Modifier.fillMaxSize(FILE_TILE_ICON_FILL_FRACTION),
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (file is VolumeFile.Other) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }
        VerticalSpacer(of = 8.dp)
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = file.name,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = if (isMedia || file is VolumeFile.Directory) {
                Color.Unspecified
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
        )
    }
}

@Composable
private fun VideoThumbnail(modifier: Modifier = Modifier, file: VolumeFile.Video) {
    val request = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
        .data(file.file)
        .crossfade(true)
        .videoFrameMillis(0)
        .build()

    SubcomposeAsyncImage(
        modifier = modifier.fillMaxSize(),
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Error -> {
                VideoFallback()
            }

            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent(modifier = Modifier.fillMaxSize())
                VideoPlayBadge()
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.VideoPlayBadge() {
    val iconTint = MaterialTheme.colorScheme.primary
    val borderTint = remember(iconTint) {
        lerp(iconTint, Color.Black, VIDEO_PLAY_BADGE_BORDER_BLEND_FRACTION)
    }

    Box(
        modifier = Modifier
            .padding(8.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Icon(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(borderTint),
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = iconTint,
        )
    }
}

@Composable
private fun VideoFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 2.dp,
        ) {
            Icon(
                modifier = Modifier
                    .padding(14.dp)
                    .size(28.dp),
                imageVector = Icons.Rounded.Videocam,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun VolumeFileTilePreview() {
    GalleryExplorerTheme {
        Surface {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LazyVerticalGrid(columns = GridCells.Fixed(PREVIEW_GRID_COLUMN_COUNT)) {
                    item {
                        VolumeFileTile(
                            modifier = Modifier.padding(4.dp),
                            file = VolumeFile.Directory(
                                file = File("/storage/emulated/0/Pictures"),
                            ),
                            onClick = {},
                        )
                    }
                    item {
                        VolumeFileTile(
                            modifier = Modifier.padding(4.dp),
                            file = VolumeFile.Image(
                                file = File("/Users/dniel/Downloads/20251202_122730.jpg"),
                            ),
                            onClick = {},
                        )
                    }
                    item {
                        VolumeFileTile(
                            modifier = Modifier.padding(4.dp),
                            file = VolumeFile.Video(
                                file = File("/storage/emulated/0/Movies/clip.mp4"),
                            ),
                            onClick = {},
                        )
                    }
                    item {
                        VolumeFileTile(
                            modifier = Modifier.padding(4.dp),
                            file = VolumeFile.Other(
                                file = File("/storage/emulated/0/Pictures/note.txt"),
                            ),
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

private const val FILE_TILE_ICON_FILL_FRACTION = 0.5f
private const val VIDEO_PLAY_BADGE_BORDER_BLEND_FRACTION = 0.5f
private const val PREVIEW_GRID_COLUMN_COUNT = 3
