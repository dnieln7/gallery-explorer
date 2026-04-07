package xyz.dnieln7.galleryex.feature.home.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.dnieln7.galleryex.core.domain.model.Volume
import xyz.dnieln7.galleryex.core.presentation.component.HorizontalSpacer
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.core.presentation.theme.LargeTileShape
import java.io.File

@Composable
fun VolumeTile(modifier: Modifier = Modifier, volume: Volume, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(LargeTileShape)
            .clickable { onClick() },
        shape = LargeTileShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            HorizontalSpacer(of = 16.dp)
            Text(
                text = volume.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
                modifier = Modifier.padding(16.dp),
                volume = Volume(
                    name = "internal storage",
                    file = File("/storage/emulated/0"),
                    isRemovable = false,
                ),
                onClick = {},
            )
        }
    }
}
