package xyz.dnieln7.galleryex.feature.home.data.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class MediaReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.i("ACTION: ${intent?.action}")
        Timber.i("DATA: ${intent?.data}")
    }
}
