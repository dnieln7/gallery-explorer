package xyz.dnieln7.galleryex.core.framework.explorer

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import xyz.dnieln7.galleryex.core.domain.model.Volume

class Explorer(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val storageManager: StorageManager by lazy {
        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    }

    private val _volumes = Channel<List<Volume>>(Channel.CONFLATED)
    val volumes: Flow<List<Volume>> = _volumes.receiveAsFlow()

    init {
        val callback =
            object : StorageManager.StorageVolumeCallback() {
                override fun onStateChanged(storageVolume: android.os.storage.StorageVolume) {
                    refreshVolumes()
                }
            }
        storageManager.registerStorageVolumeCallback(context.mainExecutor, callback)
    }

    fun hasManagerAccess(): Boolean {
        return Environment.isExternalStorageManager()
    }

    fun requestManagerAccess() {
        val intent =
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }

        context.startActivity(intent)
    }

    fun refreshVolumes() {
        val currentVolumes =
            storageManager.storageVolumes.mapNotNull {
                val file = it.directory ?: return@mapNotNull null

                Volume(
                    name = it.getDescription(context),
                    file = file,
                )
            }

        scope.launch {
            _volumes.send(currentVolumes)
        }
    }
}
