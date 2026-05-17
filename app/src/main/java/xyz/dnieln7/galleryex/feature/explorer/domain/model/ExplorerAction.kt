package xyz.dnieln7.galleryex.feature.explorer.domain.model

import xyz.dnieln7.galleryex.core.domain.enums.SortOrder
import xyz.dnieln7.galleryex.core.domain.enums.SortType

/**
 * Intents for the [xyz.dnieln7.galleryex.feature.explorer.presentation.screen.ExplorerScreen].
 */
sealed interface ExplorerAction {
    /**
     * Intended to be triggered when the screen requires the directory contents to be loaded.
     *
     * @property directoryPath Absolute path to load files from.
     * @property refresh When true, bypasses the one-time initialization guard to allow a forced reload (e.g. pull-to-refresh).
     */
    data class LoadFiles(
        val directoryPath: String,
        val refresh: Boolean = false,
    ) : ExplorerAction

    /**
     * Intended to be triggered when the user taps on the sort type button.
     *
     * @property type New sort criteria to be applied.
     */
    data class ChangeSortType(val type: SortType) : ExplorerAction

    /**
     * Intended to be triggered when the user taps on the sort order button.
     *
     * @property order New sort direction to be applied.
     */
    data class ChangeSortOrder(val order: SortOrder) : ExplorerAction
}
