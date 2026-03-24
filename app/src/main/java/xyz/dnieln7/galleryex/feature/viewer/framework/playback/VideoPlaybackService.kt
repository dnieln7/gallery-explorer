package xyz.dnieln7.galleryex.feature.viewer.framework.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import xyz.dnieln7.galleryex.R
import javax.inject.Inject

/**
 * Media3 service that owns the app's long-lived video playback session.
 *
 * This service keeps the real `ExoPlayer` outside the UI lifecycle so playback can continue while
 * the app is backgrounded. It also provides the Media3 session used by the system media
 * notification, audio focus integration, and notification re-entry into the viewer.
 */
@AndroidEntryPoint
class VideoPlaybackService : MediaSessionService() {
    @Inject
    internal lateinit var sessionStore: VideoPlaybackSessionStore

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    /**
     * Mirrors player changes into the shared session store so the UI and notification restore path
     * always reflect the current service-owned playback state.
     */
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sessionStore.updateFromPlayer(player)
        }
    }

    /**
     * Pauses playback when the current output becomes noisy, for example when headphones are
     * unplugged or a Bluetooth route disconnects.
     */
    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player.pause()
            }
        }
    }

    /**
     * Creates the service-owned player and media session.
     *
     * The player is configured for:
     * - 10-second rewind/forward increments required by the feature
     * - platform audio focus handling for media playback
     * - repeat-one playback to preserve the existing viewer behavior
     *
     * The session activity points notification taps back to [VideoPlaybackRestoreIntent].
     */
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            addListener(playerListener)
        }
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(VideoPlaybackRestoreIntent.createPendingIntent(this))
            .setMediaButtonPreferences(createMediaButtonPreferences())
            .build()

        ContextCompat.registerReceiver(
            this,
            audioBecomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /**
     * Returns the single Media3 session exposed by this service.
     *
     * @param controllerInfo Information about the connecting controller.
     * @return The shared media session used across the app.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * Releases service-owned playback resources and clears the shared in-memory session snapshot.
     */
    override fun onDestroy() {
        unregisterReceiver(audioBecomingNoisyReceiver)
        player.removeListener(playerListener)
        player.release()
        mediaSession?.release()
        mediaSession = null
        sessionStore.clear()
        super.onDestroy()
    }

    /**
     * Creates the custom media button preferences shown by system media controls when supported.
     *
     * The system still owns the final visual layout, but these preferences request the explicit
     * 10-second rewind/forward controls required by the feature.
     *
     * @return Media3 command button preferences for the session notification and system UI.
     */
    @OptIn(UnstableApi::class)
    private fun createMediaButtonPreferences(): List<CommandButton> {
        return listOf(
            CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
                .setDisplayName(getString(R.string.rewind_ten_seconds))
                .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                .setSlots(CommandButton.SLOT_BACK)
                .build(),
            CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_10)
                .setDisplayName(getString(R.string.advance_ten_seconds))
                .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                .setSlots(CommandButton.SLOT_FORWARD)
                .build(),
        )
    }
}
