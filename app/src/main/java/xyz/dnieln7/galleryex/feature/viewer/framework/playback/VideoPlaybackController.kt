package xyz.dnieln7.galleryex.feature.viewer.framework.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackRestoreRequest
import xyz.dnieln7.galleryex.feature.viewer.domain.model.VideoPlaybackSessionState
import xyz.dnieln7.galleryex.feature.viewer.domain.model.createVideoPlaybackRestoreRequest
import xyz.dnieln7.galleryex.feature.viewer.domain.model.toMediaItems
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI-facing abstraction over the background playback session.
 *
 * Compose screens use this interface instead of talking directly to `MediaController` so the app has
 * a single place that knows how to connect to the service, sanitize playlists, and keep the shared
 * session store synchronized.
 */
interface VideoPlaybackController {
    /**
     * Exposes the connected Media3 player instance when the controller is attached to the service.
     *
     * The viewer uses this to render video and to invoke transport commands such as play, pause,
     * seek, and scrubbing.
     */
    val player: StateFlow<Player?>

    /**
     * Exposes the latest playback snapshot mirrored from [VideoPlaybackSessionStore].
     *
     * This is the state the UI uses to keep the pager, title, and notification re-entry path aligned
     * with the actual service-owned playback session.
     */
    val sessionState: StateFlow<VideoPlaybackSessionState>

    /**
     * Starts the asynchronous connection to [VideoPlaybackService] when it is not already connected.
     */
    fun connect()

    /**
     * Requests playback of a folder-scoped playlist and selected item.
     *
     * @param videoPaths Absolute file paths in folder order.
     * @param selectedIndex Index that should become active.
     */
    fun openPlaylist(videoPaths: List<String>, selectedIndex: Int)

    /**
     * Switches the active media item within the current playlist.
     *
     * @param index Index of the item that should become the active video.
     */
    fun selectVideo(index: Int)

    /**
     * Stops the active playback session in response to an explicit user exit action.
     *
     * This is intended for the "leave the viewer" path, not for temporary background transitions
     * such as pressing Home or locking the device. Implementations must clear both local restore
     * state and the remote Media3 session state so the notification is dismissed and playback cannot
     * be restored accidentally. The durable stop path is also used when removable storage disappears.
     */
    fun stopPlayback()
}

/**
 * Default controller implementation backed by Media3's [MediaController].
 *
 * Responsibilities:
 * - connect the app process to the background [VideoPlaybackService]
 * - translate UI playlist requests into Media3 media items
 * - keep the shared session store updated before and after service callbacks
 * - expose the connected player instance to Compose
 *
 * The controller stores one pending request so the UI can issue playback commands before the async
 * controller connection has completed.
 *
 * @property context Application context used to build the Media3 session token.
 * @property sessionStore Shared in-memory state for the active playback session.
 */
@Singleton
class DefaultVideoPlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: VideoPlaybackSessionStore,
) : VideoPlaybackController {
    private val _player = MutableStateFlow<Player?>(null)

    override val player: StateFlow<Player?> = _player.asStateFlow()
    override val sessionState: StateFlow<VideoPlaybackSessionState> = sessionStore.sessionState

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingRequest: VideoPlaybackRestoreRequest? = null
    private var isConnecting = false

    /**
     * Connects to the service-backed Media3 session if not already connected.
     *
     * Once connected, any queued [pendingRequest] is applied immediately so a viewer opening during
     * the connection phase still starts playback without needing a second user action.
     */
    override fun connect() {
        if (controller != null || isConnecting) {
            return
        }

        isConnecting = true
        controllerFuture = MediaController.Builder(
            context,
            SessionToken(
                context,
                ComponentName(context, VideoPlaybackService::class.java),
            ),
        ).buildAsync().also { future ->
            future.addListener(
                {
                    isConnecting = false

                    runCatching { future.get() }
                        .onSuccess { mediaController ->
                            controller = mediaController
                            _player.value = mediaController
                            pendingRequest?.let { applyRequest(mediaController, it) }
                        }
                        .onFailure { error ->
                            Timber.e(error, "Unable to connect to the video playback service.")
                        }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    /**
     * Opens a sanitized playlist in the service-backed player.
     *
     * The request is stored locally and mirrored into [sessionStore] before the service confirms it,
     * which allows notification restore and UI state to remain coherent during connection/setup.
     */
    override fun openPlaylist(videoPaths: List<String>, selectedIndex: Int) {
        val request = createVideoPlaybackRestoreRequest(
            videoPaths = videoPaths,
            selectedIndex = selectedIndex,
        ) ?: return

        pendingRequest = request
        sessionStore.updateRequestedPlayback(request)
        connect()
        controller?.let { applyRequest(it, request) }
    }

    /**
     * Selects another item from the active playlist without rebuilding the playlist.
     *
     * This is used by the vertical pager so swiping between videos updates the background session and
     * notification metadata immediately.
     */
    override fun selectVideo(index: Int) {
        val mediaController = controller ?: return
        val request = sessionStore.sessionState.value
            .toRestoreRequest(index)
            ?: return

        if (index !in 0 until mediaController.mediaItemCount) {
            return
        }

        if (mediaController.currentMediaItemIndex != index) {
            mediaController.seekToDefaultPosition(index)
            mediaController.play()
        }

        pendingRequest = request
        sessionStore.updateRequestedPlayback(request)
    }

    /**
     * Stops playback and clears the active viewer session.
     *
     * This method is safe to call multiple times. It first clears pending local state so a late
     * controller connection cannot revive an old playlist, then it asks the connected Media3
     * controller to stop transport playback and remove its media items so the session is no longer
     * restorable from the notification. Finally it requests that the playback service stop itself,
     * which covers background cases where no UI collector is available to clean up the session later.
     */
    override fun stopPlayback() {
        pendingRequest = null
        sessionStore.clear()

        controller?.let { mediaController ->
            ContextCompat.getMainExecutor(context).execute {
                mediaController.stop()
                mediaController.clearMediaItems()
            }
        }
        context.stopService(Intent(context, VideoPlaybackService::class.java))
    }

    /**
     * Applies a playback request to the connected Media3 controller.
     *
     * If the playlist already matches, only the current item is updated. Otherwise the whole playlist
     * is replaced and playback starts from the requested index.
     *
     * @param mediaController Connected Media3 controller.
     * @param request Sanitized playlist request originating from the UI or restore flow.
     */
    private fun applyRequest(
        mediaController: MediaController,
        request: VideoPlaybackRestoreRequest,
    ) {
        val currentVideoPaths = mediaController.currentVideoPaths()
        val hasSamePlaylist = currentVideoPaths == request.videoPaths
        val hasSameSelection = mediaController.currentMediaItemIndex == request.selectedIndex

        if (hasSamePlaylist) {
            if (!hasSameSelection && request.selectedIndex in 0 until mediaController.mediaItemCount) {
                mediaController.seekToDefaultPosition(request.selectedIndex)
                mediaController.play()
            }

            if (mediaController.playbackState == Player.STATE_IDLE && mediaController.mediaItemCount > 0) {
                mediaController.prepare()
            }

            return
        }

        mediaController.setMediaItems(
            request.toMediaItems(),
            request.selectedIndex,
            0L,
        )
        mediaController.prepare()
        mediaController.play()
    }

}

/**
 * Composition local used to inject the shared playback controller into viewer composables.
 */
internal val LocalVideoPlaybackController = staticCompositionLocalOf<VideoPlaybackController> {
    error("VideoPlaybackController was not provided.")
}

/**
 * Returns the current controller playlist as absolute file paths.
 *
 * The service stores file paths in the `mediaId`, so comparing playlists is a simple string list
 * equality check.
 *
 * @return The current playlist represented by the connected controller.
 */
private fun MediaController.currentVideoPaths(): List<String> {
    return (0 until mediaItemCount).mapNotNull { index ->
        getMediaItemAt(index).mediaId.takeIf { it.isNotBlank() }
    }
}

/**
 * Builds a direct restore request from the current session state using a replacement selected index.
 *
 * This helper assumes the playlist is already sanitized because it comes from the live session store.
 *
 * @param selectedIndex Index that should become active.
 * @return A restore request for the current playlist, or `null` when the index is invalid.
 */
private fun VideoPlaybackSessionState.toRestoreRequest(
    selectedIndex: Int,
): VideoPlaybackRestoreRequest? {
    if (videoPaths.isEmpty() || selectedIndex !in videoPaths.indices) {
        return null
    }

    return VideoPlaybackRestoreRequest(
        videoPaths = videoPaths,
        selectedIndex = selectedIndex,
    )
}
