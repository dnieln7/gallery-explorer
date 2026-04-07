package xyz.dnieln7.galleryex.core.framework.explorer

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.dnieln7.galleryex.core.domain.model.Volume
import java.io.File

/**
 * Central storage access helper used by Home, Explorer, and the redirect guard.
 *
 * The class wraps Android's storage APIs so the rest of the app can work with a replaying snapshot of
 * mounted volumes instead of talking to [StorageManager] directly.
 */
class Explorer(
    private val context: Context,
) {
    private val storageManager: StorageManager by lazy {
        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    }

    private val _volumes = MutableStateFlow<List<Volume>>(emptyList())
    val volumes: StateFlow<List<Volume>> = _volumes.asStateFlow()

    init {
        val callback = object : StorageManager.StorageVolumeCallback() {
            override fun onStateChanged(storageVolume: android.os.storage.StorageVolume) {
                refreshVolumes()
            }
        }

        storageManager.registerStorageVolumeCallback(context.mainExecutor, callback)

        refreshVolumes()
    }

    fun hasManagerAccess(): Boolean {
        return Environment.isExternalStorageManager()
    }

    fun requestManagerAccess() {
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        context.startActivity(intent)
    }

    /**
     * Refreshes the mounted storage snapshot and publishes the latest state to [volumes].
     *
     * @return The mounted volumes that were discovered during this refresh.
     */
    fun refreshVolumes(): List<Volume> {
        val currentVolumes = storageManager.storageVolumes.mapNotNull {
            val file = it.directory ?: return@mapNotNull null

            Volume(
                name = it.getDescription(context),
                file = file,
                isRemovable = it.isRemovable,
            )
        }

        _volumes.value = currentVolumes

        return currentVolumes
    }

    /**
     * Resolves the mounted volume that backs [path], if the path belongs to one of the current roots.
     *
     * @param path Absolute file path to inspect.
     * @return The mounted [Volume] with the longest matching root prefix, or `null` when the path does not
     * belong to any mounted volume.
     */
    fun resolveVolumeForPath(path: String): Volume? {
        return volumes.value.resolveVolumeForPath(path)
    }
}

/**
 * Finds the most specific mounted volume whose root contains [path].
 *
 * This is used when a screen registers a path so the redirect coordinator can decide whether the path
 * belongs to removable storage and whether that storage disappears later.
 *
 * @param path Absolute file path to resolve.
 * @return The best matching mounted volume, or `null` when no mounted root matches.
 */
internal fun List<Volume>.resolveVolumeForPath(path: String): Volume? {
    val absolutePath = File(path).absoluteFile.path

    return filter { volume ->
        volume.root.file.absolutePath.matchesPathPrefixOf(absolutePath)
    }.maxByOrNull { volume ->
        volume.root.file.absolutePath.length
    }
}

private fun String.matchesPathPrefixOf(path: String): Boolean {
    return path == this || path.startsWith("$this${File.separator}")
}
