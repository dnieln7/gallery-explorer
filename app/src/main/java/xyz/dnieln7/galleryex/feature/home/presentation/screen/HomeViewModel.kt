package xyz.dnieln7.galleryex.feature.home.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.dnieln7.galleryex.core.framework.explorer.Explorer
import xyz.dnieln7.galleryex.di.IO
import xyz.dnieln7.galleryex.feature.home.domain.enums.AccessStatus
import xyz.dnieln7.galleryex.feature.home.domain.model.HomeAction
import xyz.dnieln7.galleryex.feature.home.domain.model.HomeState
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @IO private val dispatcher: CoroutineDispatcher,
    private val explorer: Explorer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeState())
    val uiState get() = _uiState.asStateFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnResume -> viewModelScope.launch(dispatcher) {
                val accessStatus = if (explorer.hasManagerAccess()) {
                    AccessStatus.ACCESS_GRANTED
                } else {
                    AccessStatus.ACCESS_DENIED
                }

                val volumes = explorer.getVolumes()

                _uiState.update {
                    it.copy(accessStatus = accessStatus, volumes = volumes)
                }
            }

            HomeAction.OnRequestAccessClick -> {
                explorer.requestManagerAccess()
            }

            HomeAction.OnRefreshVolumes -> viewModelScope.launch(dispatcher){
                val volumes = explorer.getVolumes()

                _uiState.update { it.copy(volumes = volumes) }
            }
        }
    }
}
