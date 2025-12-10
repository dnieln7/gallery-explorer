@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.dnieln7.galleryex.feature.pager.presentation.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import java.io.File

class PagerImageScreenDestination(
    val images: List<VolumeFile.Image>,
    val selectedIndex: Int,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        PagerImageScreen(
            images = images,
            selectedIndex = selectedIndex,
        )
    }
}

@Composable
private fun PagerImageScreen(images: List<VolumeFile.Image>, selectedIndex: Int) {
    val pagerState = rememberPagerState(pageCount = { images.size }, initialPage = selectedIndex)
    val currentImage by remember { derivedStateOf { images[pagerState.currentPage] } }

    var scale by remember { mutableFloatStateOf(1f) }

    val state = rememberTransformableState { zoomChange, _, _ ->
        scale *= zoomChange
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = currentImage.name)
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HorizontalPager(
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale.coerceAtLeast(1F),
                        scaleY = scale.coerceAtLeast(1F),
                    )
                    .transformable(state = state, canPan = { false }, lockRotationOnZoomPan = true)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { scale = if (scale != 1f) 1F else 2F })
                    }
                    .fillMaxSize(),
                state = pagerState,
            ) { index ->
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = images[index].file.toUri(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    error = painterResource(R.drawable.ic_broken_image),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PagerImagePreview() {
    GalleryExplorerTheme {
        Surface {
            PagerImageScreen(
                images = listOf(
                    VolumeFile.Image(
                        file = File("/Users/dniel/Downloads/20251202_122730.jpg"),
                    ),
                    VolumeFile.Image(
                        file = File("/Users/dniel/Downloads/2.jpg"),
                    ),
                    VolumeFile.Image(
                        file = File("/Users/dniel/Downloads/3.jpg"),
                    ),
                ),
                selectedIndex = 0,
            )
        }
    }
}
