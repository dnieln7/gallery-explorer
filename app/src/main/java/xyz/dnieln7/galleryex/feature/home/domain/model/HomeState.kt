package xyz.dnieln7.galleryex.feature.home.domain.model

import xyz.dnieln7.galleryex.core.domain.model.Volume
import xyz.dnieln7.galleryex.feature.home.domain.enums.AccessStatus

data class HomeState(
    val accessStatus: AccessStatus = AccessStatus.NONE,
    val volumes: List<Volume> = emptyList(),
)
