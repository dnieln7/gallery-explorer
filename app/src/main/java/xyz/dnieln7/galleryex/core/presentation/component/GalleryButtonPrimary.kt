package xyz.dnieln7.galleryex.core.presentation.component

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun GalleryButtonPrimary(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        content = {
            Text(text)
        },
    )
}

@Preview
@Composable
private fun GalleryButtonPrimaryPreview() {
    GalleryButtonPrimary(
        text = "Test",
        onClick = {},
    )
}
