package xyz.dnieln7.galleryex.feature.viewer.framework.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackRestoreRequest
import xyz.dnieln7.galleryex.main.presentation.screen.MainActivity

/**
 * Centralizes the intent contract used when the media notification asks the app to reopen the
 * current video viewer session.
 *
 * The notification does not carry the whole playlist payload. Instead, it carries a small action
 * marker and [MainActivity] resolves the current restore request from [VideoPlaybackSessionStore].
 */
internal object VideoPlaybackRestoreIntent {
    /**
     * Action applied to notification re-entry intents so the activity can distinguish them from a
     * normal launcher open.
     */
    const val ActionResumeVideoPlayback =
        "xyz.dnieln7.galleryex.feature.viewer.action.RESUME_VIDEO_PLAYBACK"
    private const val ExtraRestoreVideoPlayback =
        "xyz.dnieln7.galleryex.feature.viewer.extra.RESTORE_VIDEO_PLAYBACK"

    /**
     * Creates the immutable pending intent attached to the Media3 session notification.
     *
     * The intent targets [MainActivity] and reuses the existing task when possible instead of
     * spawning duplicate activity instances.
     *
     * @param context Application or service context used to build the pending intent.
     * @return Pending intent used as the session activity for the media notification.
     */
    fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ActionResumeVideoPlayback
            putExtra(ExtraRestoreVideoPlayback, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Resolves a notification re-entry intent into a viewer restore request.
     *
     * The heavy lifting happens in [sessionStore]: it provides the latest in-memory playlist and
     * selected index tracked by the playback service.
     *
     * @param intent Intent delivered to the activity.
     * @param sessionStore Shared session store containing the latest playback snapshot.
     * @return The current restore request when the intent represents a notification reopen action.
     */
    fun consumeRestoreRequest(
        intent: Intent?,
        sessionStore: VideoPlaybackSessionStore,
    ): VideoPlaybackRestoreRequest? {
        val shouldRestore = intent?.action == ActionResumeVideoPlayback &&
            intent.getBooleanExtra(ExtraRestoreVideoPlayback, false)

        if (!shouldRestore) {
            return null
        }

        return sessionStore.currentRestoreRequest()
    }

    /**
     * Removes the restore markers from an intent after it has been consumed.
     *
     * This prevents configuration changes or later lifecycle events from replaying the same restore
     * action more than once.
     *
     * @param intent Intent currently held by the activity.
     * @return A copy of [intent] without the playback restore markers, or `null` when no intent exists.
     */
    fun clearFromIntent(intent: Intent?): Intent? {
        return intent?.let {
            Intent(it).apply {
                action = null
                removeExtra(ExtraRestoreVideoPlayback)
            }
        }
    }
}
