@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.dnieln7.galleryex.feature.home.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import xyz.dnieln7.galleryex.core.presentation.modifier.edgeToEdgePadding
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.Volume
import xyz.dnieln7.galleryex.core.presentation.component.GalleryButtonPrimary
import xyz.dnieln7.galleryex.core.presentation.component.PullToRefresh
import xyz.dnieln7.galleryex.core.presentation.component.VerticalSpacer
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.gallery.presentation.screen.GalleryScreenDestination
import xyz.dnieln7.galleryex.feature.home.domain.enums.AccessStatus
import xyz.dnieln7.galleryex.feature.home.domain.model.HomeAction
import xyz.dnieln7.galleryex.feature.home.domain.model.HomeState
import xyz.dnieln7.galleryex.feature.home.presentation.component.VolumeTile

class HomeScreenDestination : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = getViewModel<HomeViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            viewModel.onAction(HomeAction.OnResume)
        }

        HomeScreen(
            state = state,
            onAction = viewModel::onAction,
            navigateToGallery = {
                val nextDirectory = it.root

                navigator.push(GalleryScreenDestination(
                    titles = listOf(it.name),
                    directory = nextDirectory,
                ))
            }
        )
    }
}

@Composable
private fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    navigateToGallery: (Volume) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.storage))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (state.accessStatus) {
                AccessStatus.NONE -> {
                    CircularProgressIndicator()
                }

                AccessStatus.ACCESS_GRANTED -> {
                    PullToRefresh(onRefresh = { onAction(HomeAction.OnRefreshVolumes) }) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.volumes) {
                                VolumeTile(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    volume = it,
                                    onClick = { navigateToGallery(it) },
                                )
                            }
                        }
                    }
                }

                AccessStatus.ACCESS_DENIED -> {
                    Text(
                        text = stringResource(R.string.all_files_access_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    VerticalSpacer(of = 12.dp)
                    Text(
                        text = stringResource(R.string.all_files_access_description),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    VerticalSpacer(of = 8.dp)
                    Text(
                        text = stringResource(R.string.all_files_access_description_2),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    VerticalSpacer(of = 16.dp)
                    GalleryButtonPrimary(
                        text = stringResource(R.string.grant_access),
                        onClick = { onAction(HomeAction.OnRequestAccessClick) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomePreview() {
    GalleryExplorerTheme {
        Surface {
            HomeScreen(
                state = HomeState(
                    accessStatus = AccessStatus.ACCESS_DENIED,
                    volumes = listOf()
                ),
                onAction = {},
                navigateToGallery = {},
            )
        }
    }
}
