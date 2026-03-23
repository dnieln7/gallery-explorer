@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.dnieln7.galleryex.feature.viewer.presentation.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.viewer.presentation.component.ZoomableImage
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
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Text(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        text = currentImage.name,
                        maxLines = 1,
                    )
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
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            VerticalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                flingBehavior = PagerDefaults.flingBehavior(pagerState),
            ) { index ->
                ZoomableImage(
                    image = images[index],
                    isFocused = pagerState.currentPage == index,
                    modifier = Modifier.fillMaxSize()
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
