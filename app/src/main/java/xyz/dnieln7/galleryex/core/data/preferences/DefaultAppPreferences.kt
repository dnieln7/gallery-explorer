package xyz.dnieln7.galleryex.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.dnieln7.galleryex.core.domain.enums.SortOrder
import xyz.dnieln7.galleryex.core.domain.enums.SortType
import xyz.dnieln7.galleryex.core.domain.preferences.AppPreferences

private val Context.dataStore by preferencesDataStore(name = "gallery_explorer_preferences")

class DefaultAppPreferences(private val context: Context) : AppPreferences {

    private val sortTypeKey = stringPreferencesKey("sort_type")
    private val sortOrderKey = stringPreferencesKey("sort_order")

    override val sortTypeFlow: Flow<SortType> = context.dataStore.data.map { preferences ->
        val savedType = preferences[sortTypeKey] ?: SortType.NAME.name
        runCatching { SortType.valueOf(savedType) }.getOrDefault(SortType.NAME)
    }

    override val sortOrderFlow: Flow<SortOrder> = context.dataStore.data.map { preferences ->
        val savedOrder = preferences[sortOrderKey] ?: SortOrder.ASCENDING.name
        runCatching { SortOrder.valueOf(savedOrder) }.getOrDefault(SortOrder.ASCENDING)
    }

    override suspend fun saveSortType(type: SortType) {
        context.dataStore.edit { preferences ->
            preferences[sortTypeKey] = type.name
        }
    }

    override suspend fun saveSortOrder(order: SortOrder) {
        context.dataStore.edit { preferences ->
            preferences[sortOrderKey] = order.name
        }
    }
}
