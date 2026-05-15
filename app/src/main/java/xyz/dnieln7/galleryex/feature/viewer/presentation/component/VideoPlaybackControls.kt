package xyz.dnieln7.galleryex.feature.viewer.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.presentation.component.HorizontalSpacer
import xyz.dnieln7.galleryex.core.presentation.component.VerticalSpacer
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme

@Composable
internal fun VideoPlaybackControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    durationTotalMs: Long,
    durationCurrentMs: Long,
    durationSlider: Float,
    onPlayPauseClick: () -> Unit,
    onSeekBackClick: () -> Unit,
    onSeekForwardClick: () -> Unit,
    onSliderValueChange: (Float) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = durationSlider,
            enabled = durationTotalMs > 0L,
            onValueChange = onSliderValueChange,
            onValueChangeFinished = onSliderValueChangeFinished,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatPlaybackTime(durationCurrentMs),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatPlaybackTime(durationTotalMs),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        VerticalSpacer(of = 12.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            FilledTonalIconButton(onClick = onSeekBackClick) {
                Icon(
                    imageVector = Icons.Rounded.Replay10,
                    contentDescription = stringResource(R.string.rewind_ten_seconds),
                )
            }
            HorizontalSpacer(of = 12.dp)
            FilledTonalIconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) {
                            R.string.pause_video
                        } else {
                            R.string.play_video
                        },
                    ),
                )
            }
            HorizontalSpacer(of = 12.dp)
            FilledTonalIconButton(onClick = onSeekForwardClick) {
                Icon(
                    imageVector = Icons.Rounded.Forward10,
                    contentDescription = stringResource(R.string.advance_ten_seconds),
                )
            }
        }
    }
}

@Preview
@Composable
private fun VideoPlaybackControlsPreview() {
    GalleryExplorerTheme {
        VideoPlaybackControls(
            isPlaying = true,
            durationCurrentMs = 73_000L,
            durationTotalMs = 143_000L,
            durationSlider = 0.51f,
            onPlayPauseClick = {},
            onSeekBackClick = {},
            onSeekForwardClick = {},
            onSliderValueChange = {},
            onSliderValueChangeFinished = {},
        )
    }
}
