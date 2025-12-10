package xyz.dnieln7.galleryex.feature.gallery.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.traceEventEnd
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.component.VerticalSpacer
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import java.io.File

@Composable
fun VolumeFileTile(modifier: Modifier = Modifier, file: VolumeFile, onClick: () -> Unit) {
    val icon = remember(file) {
        when (file) {
            is VolumeFile.Directory -> Icons.Rounded.Folder
            is VolumeFile.Image -> Icons.Rounded.Image
            is VolumeFile.Other -> Icons.AutoMirrored.Rounded.InsertDriveFile
        }
    }

    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.clickable { onClick() }) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (file is VolumeFile.Image) {
                    AsyncImage(
                        modifier = Modifier.size(maxWidth),
                    model = file.file.toUri(),
//                        model = "https://static.wikia.nocookie.net/fateanother/images/b/b8/RiderAstolfoStage2.jpg/revision/latest?cb=20190708225924",
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        error = painterResource(R.drawable.ic_broken_image),
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(maxWidth),
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 4.dp),
                text = file.name,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
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
                LazyVerticalGrid(columns = GridCells.Fixed(3)) {
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
