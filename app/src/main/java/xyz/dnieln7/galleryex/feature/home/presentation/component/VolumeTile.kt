package xyz.dnieln7.galleryex.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.dnieln7.galleryex.core.domain.model.Volume
import xyz.dnieln7.galleryex.core.presentation.component.HorizontalSpacer
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import java.io.File

@Composable
fun VolumeTile(modifier: Modifier = Modifier, volume: Volume, onClick: () -> Unit) {
    ElevatedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(36.dp),
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
            )
            HorizontalSpacer(of = 8.dp)
            Text(
                text = volume.name,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun VolumeTilePreview() {
    GalleryExplorerTheme {
        Surface {
            VolumeTile(
                volume = Volume(
                    name = "volume1",
                    file = File("/storage/emulated/0"),
                ),
                onClick = {},
            )
        }
    }
}
