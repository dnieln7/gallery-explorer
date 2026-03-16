@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import java.io.File

class ImageViewerScreenDestination(
    val images: List<VolumeFile.Image>,
    val selectedIndex: Int,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        ImageViewerScreen(
            images = images,
            selectedIndex = selectedIndex,
            navigateBack = { navigator.pop() },
        )
    }
}

@Composable
private fun ImageViewerScreen(
    images: List<VolumeFile.Image>,
    selectedIndex: Int,
    navigateBack: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { images.size }, initialPage = selectedIndex)
    val currentImage by remember { derivedStateOf { images[pagerState.currentPage] } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = currentImage.name)
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    )
                },
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            var pagerScrollEnabled by remember { mutableStateOf(true) }
            var scale by remember { mutableFloatStateOf(1F) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val state = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(1F, 4F)

                // Get the extra width and height only when the image is zoomed in
                val extraWidth = (scale - 1) * constraints.maxWidth
                val extraHeight = (scale - 1) * constraints.maxHeight

                // Divide by the 2 sides from the center of the image
                val maxX = extraWidth / 2
                val maxY = extraHeight / 2

                // If image is zoomed in increase pan to compensate for velocity loss
                val newPanChange = panChange * scale

                offset = Offset(
                    x = (offset.x + newPanChange.x).coerceIn(-maxX, maxX),
                    y = (offset.y + newPanChange.y).coerceIn(-maxY, maxY),
                )

                pagerScrollEnabled = offset.x == maxX || offset.x == -maxX
            }

            val flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = object : PagerSnapDistance {
                    override fun calculateTargetPage(
                        startPage: Int,
                        suggestedTargetPage: Int,
                        velocity: Float,
                        pageSize: Int,
                        pageSpacing: Int
                    ): Int {
                        Timber.tag("PAGER").i("startPage: $startPage, suggestedTargetPage: $suggestedTargetPage, velocity: $velocity, pageSize: $pageSize, pageSpacing: $pageSpacing")
                        return 1
                    }

                },
            )

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collectLatest {
                    scale = 1F
                    offset = Offset.Zero
                }
            }

            HorizontalPager(
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state, canPan = { scale != 1F })
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { scale = if (scale != 1f) 1F else 2F })
                    }
                    .fillMaxSize(),
                state = pagerState,
                flingBehavior = flingBehavior,
                userScrollEnabled = pagerScrollEnabled,
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
private fun ImageViewerPreview() {
    GalleryExplorerTheme {
        Surface {
            ImageViewerScreen(
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
                navigateBack = { },
            )
        }
    }
}
