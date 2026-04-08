@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.dnieln7.galleryex.feature.example.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.framework.extension.toastLong
import xyz.dnieln7.galleryex.core.framework.extension.toastShort
import xyz.dnieln7.galleryex.core.presentation.component.VerticalSpacer
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme
import xyz.dnieln7.galleryex.core.presentation.util.CollectEventsWithLifeCycle
import xyz.dnieln7.galleryex.feature.example.domain.model.ExampleAction
import xyz.dnieln7.galleryex.feature.example.domain.model.ExampleEvent
import xyz.dnieln7.galleryex.feature.example.domain.model.ExampleState

class ExampleScreenDestination : Screen {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        LocalNavigator.currentOrThrow

        val viewModel = getViewModel<ExampleViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        CollectEventsWithLifeCycle(viewModel.events) {
            when (it) {
                ExampleEvent.OnDataSubmitted -> {
                    context.toastShort(R.string.data_submitted)
                    viewModel.onAction(ExampleAction.OnRefresh)
                }

                is ExampleEvent.OnError -> {
                    context.toastLong(it.message.asString(context))
                }
            }
        }

        ExampleScreen(
            state = state,
            onAction = viewModel::onAction,
        )
    }
}

@Composable
fun ExampleScreen(
    state: ExampleState,
    onAction: (ExampleAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.example))
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            VerticalSpacer(of = 24.dp)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading,
                value = state.input,
                onValueChange = { onAction(ExampleAction.OnInputChanged(it)) },
                label = { Text(text = stringResource(R.string.input_hint)) },
            )
            VerticalSpacer(of = 20.dp)
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading,
                onClick = { onAction(ExampleAction.OnSubmitClick) },
                content = { Text(text = stringResource(R.string.submit)) }
            )
            VerticalSpacer(of = 20.dp)
            LazyColumn(modifier = Modifier.weight(1F)) {
                items(state.data) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(8.dp),
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LoginPreview() {
    GalleryExplorerTheme {
        Surface {
            ExampleScreen(
                state = ExampleState(
                    loading = true,
                    data = listOf(
                        "DATA_1",
                        "DATA_2",
                        "DATA_3",
                    ),
                ),
                onAction = {},
            )
        }
    }
}
