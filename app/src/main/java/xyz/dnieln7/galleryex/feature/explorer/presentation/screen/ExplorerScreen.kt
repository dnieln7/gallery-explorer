@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.dnieln7.galleryex.feature.explorer.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.explorer.presentation.component.VolumeFileTile
import xyz.dnieln7.galleryex.feature.viewer.presentation.screen.ImageViewerScreenDestination
import java.io.File

class ExplorerScreenDestination(
    val titles: List<String>,
    val directory: VolumeFile.Directory,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        ExplorerScreen(
            titles = titles,
            directory = directory,
            navigateBack = { navigator.pop() },
            navigateToExplorer = {
                navigator.push(
                    ExplorerScreenDestination(
                        titles = titles + it.name,
                        directory = it,
                    )
                )
            },
            navigateToViewer = { files, image ->
                val images = files.filterIsInstance<VolumeFile.Image>()
                val selectedIndex = images.indexOf(image)

                navigator.push(
                    ImageViewerScreenDestination(
                        images = images,
                        selectedIndex = selectedIndex,
                    )
                )
            }
        )
    }
}

@Composable
private fun ExplorerScreen(
    titles: List<String>,
    directory: VolumeFile.Directory,
    navigateBack: () -> Unit,
    navigateToExplorer: (VolumeFile.Directory) -> Unit,
    navigateToViewer: (List<VolumeFile>, VolumeFile.Image) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val title = remember(titles) { titles.joinToString(separator = " > ") }
    val files = remember(directory) { directory.children }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        modifier = Modifier.padding(end = 16.dp),
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.StartEllipsis,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Normal,
                        )
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
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (files.isNotEmpty()) {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(3),
                    ) {
                        items(files) {
                            VolumeFileTile(
                                modifier = Modifier,
                                file = it,
                                onClick = {
                                    when (it) {
                                        is VolumeFile.Directory -> navigateToExplorer(it)
                                        is VolumeFile.Image -> navigateToViewer(files, it)
                                        else -> Unit
                                    }
                                },
                            )
                        }
                    }
                } else {
                    Text(text = "No files", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    )
}

@Preview
@Composable
private fun ExplorerPreview() {
    GalleryExplorerTheme {
        Surface {
            ExplorerScreen(
                titles = listOf(
                    "Internal Storage",
                    "Pictures",
                ),
                directory = VolumeFile.Directory(
                    file = File("/storage/emulated/0/Pictures"),
                ),
                navigateBack = { },
                navigateToExplorer = { },
                navigateToViewer = { _, _ -> },
            )
        }
    }
}
