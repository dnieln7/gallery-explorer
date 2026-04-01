package xyz.dnieln7.galleryex.feature.explorer.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.domain.enums.SortOrder
import xyz.dnieln7.galleryex.core.domain.enums.SortType
import xyz.dnieln7.galleryex.core.domain.preferences.AppPreferences
import xyz.dnieln7.galleryex.feature.explorer.domain.model.ExplorerAction
import xyz.dnieln7.galleryex.feature.explorer.domain.model.ExplorerEvent
import xyz.dnieln7.galleryex.feature.explorer.domain.model.ExplorerState
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<ExplorerEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            appPreferences.sortTypeFlow.collect { type ->
                if (_uiState.value.sortType != type || _uiState.value.files.isNotEmpty()) {
                    _uiState.update { it.copy(sortType = type) }
                    resortFiles()
                }
            }
        }
        viewModelScope.launch {
            appPreferences.sortOrderFlow.collect { order ->
                 if (_uiState.value.sortOrder != order || _uiState.value.files.isNotEmpty()) {
                    _uiState.update { it.copy(sortOrder = order) }
                    resortFiles()
                }
            }
        }
    }

    fun onAction(action: ExplorerAction) {
        when (action) {
            is ExplorerAction.LoadFiles -> loadFiles(action.directoryPath)
            is ExplorerAction.ChangeSortOrder -> changeSortOrder(action.order)
            is ExplorerAction.ChangeSortType -> changeSortType(action.type)
        }
    }

    private fun loadFiles(directoryPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val type = appPreferences.sortTypeFlow.first()
            val order = appPreferences.sortOrderFlow.first()

            val files = withContext(Dispatchers.IO) {
                val directory = VolumeFile.Directory(file = File(directoryPath))
                sortFiles(
                    files = directory.children,
                    sortType = type,
                    sortOrder = order,
                )
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    files = files,
                    sortType = type,
                    sortOrder = order,
                )
            }
        }
    }

    private fun changeSortType(type: SortType) {
        if (_uiState.value.sortType == type) return
        viewModelScope.launch {
            appPreferences.saveSortType(type)
        }
    }

    private fun changeSortOrder(order: SortOrder) {
        if (_uiState.value.sortOrder == order) return
        viewModelScope.launch {
            appPreferences.saveSortOrder(order)
        }
    }

    private fun resortFiles() {
        val currentState = _uiState.value
        if (currentState.files.isEmpty()) return

        viewModelScope.launch {
            val sortedFiles = withContext(Dispatchers.Default) {
                sortFiles(currentState.files, currentState.sortType, currentState.sortOrder)
            }
            _uiState.update { it.copy(files = sortedFiles) }
        }
    }

    private fun sortFiles(
        files: List<VolumeFile>,
        sortType: SortType,
        sortOrder: SortOrder,
    ): List<VolumeFile> {
        val comparator = when (sortType) {
            SortType.NAME -> compareBy<VolumeFile> { it.name.lowercase() }
            SortType.DATE -> compareBy<VolumeFile> { it.file.lastModified() }
        }

        return when (sortOrder) {
            SortOrder.ASCENDING -> files.sortedWith(comparator)
            SortOrder.DESCENDING -> files.sortedWith(comparator.reversed())
        }
    }
}
