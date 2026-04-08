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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaScreenTarget
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.presentation.component.EmptyState
import xyz.dnieln7.galleryex.core.presentation.media.LocalExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.explorer.presentation.component.VolumeFileTile
import xyz.dnieln7.galleryex.feature.viewer.presentation.screen.ImageViewerScreenDestination
import xyz.dnieln7.galleryex.feature.viewer.presentation.screen.VideoViewerScreenDestination
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.hilt.getViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import xyz.dnieln7.galleryex.core.domain.enums.SortOrder
import xyz.dnieln7.galleryex.core.domain.enums.SortType
import xyz.dnieln7.galleryex.feature.explorer.domain.model.ExplorerAction
import xyz.dnieln7.galleryex.feature.explorer.domain.model.ExplorerState

/**
 * Voyager destination that shows the contents of a directory identified by its absolute path.
 *
 * @property titles Breadcrumb labels shown in the top app bar.
 * @property directoryPath Absolute path of the directory to render.
 * @property removableVolumeRootPath Removable volume root captured when the destination was created.
 * @property removableVolumeName Removable volume label used if the destination must be redirected home.
 */
data class ExplorerScreenDestination(
    val titles: List<String>,
    val directoryPath: String,
    val removableVolumeRootPath: String? = null,
    val removableVolumeName: String? = null,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getViewModel<ExplorerViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val externalMediaRedirectCoordinator = LocalExternalMediaRedirectCoordinator.current
        val coroutineScope = rememberCoroutineScope()
        val screenTarget = remember(directoryPath, removableVolumeRootPath, removableVolumeName) {
            ExternalMediaScreenTarget(
                path = directoryPath,
                removableVolumeRootPath = removableVolumeRootPath,
                removableVolumeName = removableVolumeName,
            )
        }

        LaunchedEffect(screenTarget) {
            externalMediaRedirectCoordinator.registerTarget(screenTarget)
        }

        DisposableEffect(directoryPath) {
            onDispose {
                coroutineScope.launch {
                    externalMediaRedirectCoordinator.clearPath(directoryPath)
                }
            }
        }

        LaunchedEffect(directoryPath) {
            viewModel.onAction(ExplorerAction.LoadFiles(directoryPath))
        }

        ExplorerScreen(
            titles = titles,
            state = state,
            onAction = viewModel::onAction,
            navigateBack = { navigator.pop() },
            navigateToExplorer = {
                navigator.push(
                    ExplorerScreenDestination(
                        titles = titles + it.name,
                        directoryPath = it.file.absolutePath,
                        removableVolumeRootPath = removableVolumeRootPath,
                        removableVolumeName = removableVolumeName,
                    ),
                )
            },
            navigateToImageViewer = { files, image ->
                val request = createImageViewerRequest(files = files, selectedImage = image)

                navigator.push(
                    ImageViewerScreenDestination(
                        imagePaths = request.imagePaths,
                        selectedIndex = request.selectedIndex,
                        removableVolumeRootPath = removableVolumeRootPath,
                        removableVolumeName = removableVolumeName,
                    ),
                )
            },
            navigateToVideoViewer = { files, video ->
                val request = createVideoViewerRequest(files = files, selectedVideo = video)

                navigator.push(
                    VideoViewerScreenDestination(
                        videoPaths = request.videoPaths,
                        selectedIndex = request.selectedIndex,
                        removableVolumeRootPath = removableVolumeRootPath,
                        removableVolumeName = removableVolumeName,
                    ),
                )
            },
        )
    }
}

@Composable
private fun ExplorerScreen(
    titles: List<String>,
    state: ExplorerState,
    onAction: (ExplorerAction) -> Unit,
    navigateBack: () -> Unit,
    navigateToExplorer: (VolumeFile.Directory) -> Unit,
    navigateToImageViewer: (List<VolumeFile>, VolumeFile.Image) -> Unit,
    navigateToVideoViewer: (List<VolumeFile>, VolumeFile.Video) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val title = remember(titles) { titles.joinToString(separator = " • ") }
    val files = state.files

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
                        ),
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
                        },
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            val nextType = if (state.sortType == SortType.NAME) SortType.DATE else SortType.NAME
                            onAction(ExplorerAction.ChangeSortType(nextType))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = stringResource(R.string.sort_type)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (state.sortType) {
                                SortType.NAME -> stringResource(R.string.name)
                                SortType.DATE -> stringResource(R.string.date)
                            }
                        )
                    }
                    IconButton(
                        onClick = {
                            val nextOrder = if (state.sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                            onAction(ExplorerAction.ChangeSortOrder(nextOrder))
                        }
                    ) {
                        Icon(
                            imageVector = if (state.sortOrder == SortOrder.ASCENDING) {
                                Icons.Rounded.ArrowUpward
                            } else {
                                Icons.Rounded.ArrowDownward
                            },
                            contentDescription = stringResource(R.string.sort_order),
                        )
                    }
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
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else if (files.isNotEmpty()) {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(EXPLORER_GRID_COLUMN_COUNT),
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
        },
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
                state = ExplorerState(),
                onAction = { },
                navigateBack = { },
                navigateToExplorer = { },
                navigateToImageViewer = { _, _ -> },
                navigateToVideoViewer = { _, _ -> },
            )
        }
    }
}

private const val EXPLORER_GRID_COLUMN_COUNT = 3
