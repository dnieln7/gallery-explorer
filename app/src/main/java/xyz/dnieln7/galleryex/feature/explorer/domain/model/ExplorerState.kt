package xyz.dnieln7.galleryex.feature.explorer.domain.model

import xyz.dnieln7.galleryex.core.domain.model.VolumeFile
import xyz.dnieln7.galleryex.core.domain.enums.SortOrder
import xyz.dnieln7.galleryex.core.domain.enums.SortType

/**
 * State of the [xyz.dnieln7.galleryex.feature.explorer.presentation.screen.ExplorerScreen].
 *
 * @property isLoading True if the directory contents are currently being loaded.
 * @property files List of files retrieved from the directory.
 * @property sortType Current sort criteria.
 * @property sortOrder Current sort direction.
 */
data class ExplorerState(
    val isLoading: Boolean = false,
    val files: List<VolumeFile> = emptyList(),
    val sortType: SortType = SortType.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
)
