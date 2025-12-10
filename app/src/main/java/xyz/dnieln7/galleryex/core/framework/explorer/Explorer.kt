package xyz.dnieln7.galleryex.core.framework.explorer

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import xyz.dnieln7.galleryex.core.domain.model.Volume

class Explorer(private val context: Context) {
    private val storageManager: StorageManager by lazy {
        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
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

    fun getVolumes(): List<Volume> {
        return storageManager.storageVolumes.mapNotNull {
            val file = it.directory ?: return@mapNotNull null

            Volume(
                name = it.getDescription(context),
                file = file,
            )
        }
    }
}
