package xyz.dnieln7.galleryex.feature.example.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.dnieln7.galleryex.core.domain.repository.ExampleRepository
import xyz.dnieln7.galleryex.di.IO
import xyz.dnieln7.galleryex.feature.example.domain.model.ExampleAction
import xyz.dnieln7.galleryex.feature.example.domain.model.ExampleEvent
import xyz.dnieln7.galleryex.feature.example.domain.model.ExampleState
import xyz.dnieln7.galleryex.feature.example.presentation.error.toUIText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExampleViewModel @Inject constructor(
    @IO private val dispatcher: CoroutineDispatcher,
    private val exampleRepository: ExampleRepository,
) : ViewModel() {
    private val _events = Channel<ExampleEvent>()
    val events = _events.receiveAsFlow()

    private val _uiState = MutableStateFlow(ExampleState())
    val uiState = _uiState.asStateFlow()

    init {
        onAction(ExampleAction.OnRefresh)
    }

    fun onAction(action: ExampleAction) {
        when (action) {
            ExampleAction.OnRefresh -> onRefresh()
            is ExampleAction.OnInputChanged -> onInputChanged(action.input)
            is ExampleAction.OnSubmitClick -> onSubmit()
        }
    }

    private fun onRefresh() {
        viewModelScope.launch(dispatcher) {
            _uiState.update { it.copy(loading = true) }

            exampleRepository.getData().fold(
                { error ->
                    _uiState.update { it.copy(loading = false) }
                    _events.send(ExampleEvent.OnError(error.toUIText()))
                },
                { data ->
                    _uiState.update { it.copy(loading = false, data = data) }
                },
            )
        }
    }

    private fun onInputChanged(input: String) {
        _uiState.update { it.copy(input = input) }
    }

    private fun onSubmit() {
        viewModelScope.launch(dispatcher) {
            _uiState.update { it.copy(loading = true) }

            val input = uiState.value.input

            exampleRepository.submitData(input).fold(
                { error ->
                    _uiState.update { it.copy(loading = false) }
                    _events.send(ExampleEvent.OnError(error.toUIText()))
                },
                {
                    _uiState.update { it.copy(loading = false, input = "") }
                    _events.send(ExampleEvent.OnDataSubmitted)
                },
            )
        }
    }
}
