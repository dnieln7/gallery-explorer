package xyz.dnieln7.galleryex.core.domain.preferences

import kotlinx.coroutines.flow.Flow
import xyz.dnieln7.galleryex.core.domain.enums.SortOrder
import xyz.dnieln7.galleryex.core.domain.enums.SortType

/**
 * Interface that holds globally accessible preferences spanning across the app.
 */
interface AppPreferences {
    val sortTypeFlow: Flow<SortType>
    val sortOrderFlow: Flow<SortOrder>

    /**
     * Updates the persistent SortType.
     * @param type The type constraint to be saved.
     */
    suspend fun saveSortType(type: SortType)

    /**
     * Updates the persistent SortOrder.
     * @param order The directional sort constraint to be saved.
     */
    suspend fun saveSortOrder(order: SortOrder)
}
