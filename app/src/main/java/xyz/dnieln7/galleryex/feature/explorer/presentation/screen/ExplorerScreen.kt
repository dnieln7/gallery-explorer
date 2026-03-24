@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.dnieln7.galleryex.feature.explorer.presentation.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.component.EmptyState
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.explorer.presentation.component.VolumeFileTile
import xyz.dnieln7.galleryex.feature.viewer.presentation.screen.ImageViewerScreenDestination
import xyz.dnieln7.galleryex.feature.viewer.presentation.screen.VideoViewerScreenDestination
import java.io.File

/**
 * Voyager destination that shows the contents of a directory identified by its absolute path.
 *
 * @property titles Breadcrumb labels shown in the top app bar.
 * @property directoryPath Absolute path of the directory to render.
 */
class ExplorerScreenDestination(
    val titles: List<String>,
    val directoryPath: String,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val directory = remember(directoryPath) { directoryFromPath(directoryPath) }

        ExplorerScreen(
            titles = titles,
            directory = directory,
            navigateBack = { navigator.pop() },
            navigateToExplorer = {
                navigator.push(
                    ExplorerScreenDestination(
                        titles = titles + it.name,
                        directoryPath = it.file.absolutePath,
                    )
                )
            },
            navigateToImageViewer = { files, image ->
                val request = createImageViewerRequest(files = files, selectedImage = image)

                navigator.push(
                    ImageViewerScreenDestination(
                        imagePaths = request.imagePaths,
                        selectedIndex = request.selectedIndex,
                    )
                )
            },
            navigateToVideoViewer = { files, video ->
                val request = createVideoViewerRequest(files = files, selectedVideo = video)

                navigator.push(
                    VideoViewerScreenDestination(
                        videoPaths = request.videoPaths,
                        selectedIndex = request.selectedIndex,
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
    navigateToImageViewer: (List<VolumeFile>, VolumeFile.Image) -> Unit,
    navigateToVideoViewer: (List<VolumeFile>, VolumeFile.Video) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val title = remember(titles) { titles.joinToString(separator = " • ") }
    val files = remember(directory) { directory.children }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        text = title,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
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
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (files.isNotEmpty()) {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(3),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(files) {
                            VolumeFileTile(
                                modifier = Modifier,
                                file = it,
                                onClick = {
                                    when (it) {
                                        is VolumeFile.Directory -> navigateToExplorer(it)
                                        is VolumeFile.Image -> navigateToImageViewer(files, it)
                                        is VolumeFile.Video -> navigateToVideoViewer(files, it)
                                        else -> Unit
                                    }
                                },
                            )
                        }
                    }
                } else {
                    EmptyState(
                        icon = Icons.Rounded.FolderOff,
                        title = stringResource(R.string.empty_directory),
                        message = stringResource(R.string.empty_directory_message),
                    )
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
                navigateToImageViewer = { _, _ -> },
                navigateToVideoViewer = { _, _ -> },
            )
        }
    }
}

internal fun directoryFromPath(directoryPath: String): VolumeFile.Directory {
    return VolumeFile.Directory(file = File(directoryPath))
}
