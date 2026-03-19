package xyz.dnieln7.galleryex.feature.home.domain.model

sealed interface HomeAction {
    data object OnResume : HomeAction
    data object OnRequestAccessClick : HomeAction
    data object OnRefreshVolumes : HomeAction
}
