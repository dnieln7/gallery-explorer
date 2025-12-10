package xyz.dnieln7.galleryex.feature.home.domain.model

import xyz.dnieln7.galleryex.core.domain.model.Volume

sealed interface HomeAction {
    data object OnResume: HomeAction
    data object OnRequestAccessClick: HomeAction
    data object OnRefreshVolumes: HomeAction
}
