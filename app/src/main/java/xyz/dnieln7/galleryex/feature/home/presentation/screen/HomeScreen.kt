@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.dnieln7.galleryex.feature.home.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.Volume
import xyz.dnieln7.galleryex.core.presentation.component.PullToRefresh
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.feature.explorer.presentation.screen.ExplorerScreenDestination
import xyz.dnieln7.galleryex.feature.home.domain.enums.AccessStatus
import xyz.dnieln7.galleryex.feature.home.domain.model.HomeAction
import xyz.dnieln7.galleryex.feature.home.domain.model.HomeState
import xyz.dnieln7.galleryex.feature.home.presentation.component.GalleryEmptyState
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
            navigateToExplorer = {
                val nextDirectory = it.root

                navigator.push(
                    ExplorerScreenDestination(
                        titles = listOf(it.name),
                        directory = nextDirectory,
                    )
                )
            }
        )
    }
}

@Composable
private fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    navigateToExplorer: (Volume) -> Unit,
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
                                    onClick = { navigateToExplorer(it) },
                                )
                            }
                        }
                    }
                }

                AccessStatus.ACCESS_DENIED -> {
                    GalleryEmptyState(
                        onButtonClick = { onAction(HomeAction.OnRequestAccessClick) }
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
                navigateToExplorer = {},
            )
        }
    }
}
