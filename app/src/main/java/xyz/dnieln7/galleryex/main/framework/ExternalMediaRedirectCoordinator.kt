package xyz.dnieln7.galleryex.main.framework

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.model.Volume
import xyz.dnieln7.galleryex.core.framework.explorer.Explorer
import xyz.dnieln7.galleryex.core.framework.explorer.resolveVolumeForPath
import xyz.dnieln7.galleryex.core.presentation.text.UIText
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackController
import javax.inject.Singleton

/**
 * Coordinates redirect decisions when removable storage disappears while the app is using it.
 *
 * The coordinator keeps track of the screen currently shown by the app, stops playback durably when
 * its removable volume disappears, and emits a single redirect event so the activity can replace the
 * navigator stack and show the user a toast.
 */
interface ExternalMediaRedirectCoordinator {
    /**
     * One-shot redirect events consumed by the activity host.
     *
     * The flow is intentionally one-shot so the activity can react to a single disappearance without
     * replaying stale redirects after configuration changes.
     */
    val events: Flow<ExternalMediaRedirectEvent>

    /**
     * Starts monitoring the removable volume that backs [target], if any.
     *
     * Screens call this when they become visible so the coordinator knows which mounted storage should
     * be watched for removal.
     */
    suspend fun registerTarget(target: ExternalMediaScreenTarget)

    /**
     * Stops monitoring the provided path when it is no longer the active screen.
     *
     * Passing a path that no longer matches the current target is harmless and leaves the active target
     * unchanged.
     */
    suspend fun clearPath(path: String? = null)

    /**
     * Refreshes mounted volumes and re-checks the active monitored target.
     *
     * This is called from `ON_RESUME` so removals that happened while the app was backgrounded are
     * detected before the user interacts again.
     */
    suspend fun refreshAndVerify()
}

/**
 * One-shot commands emitted by the redirect coordinator.
 */
sealed interface ExternalMediaRedirectEvent {
    /**
     * Redirects the user back to home with an explanatory toast.
     *
     * @property message Toast text that explains why the redirect happened.
     */
    data class Redirect(val message: UIText) : ExternalMediaRedirectEvent
}

/**
 * Default coordinator implementation backed by the shared [Explorer] volume snapshot.
 *
 * It listens for volume changes, keeps the currently visible removable root in memory, stops the
 * playback session when that root disappears, and emits a redirect event exactly once.
 */
@Singleton
class DefaultExternalMediaRedirectCoordinator(
    private val explorer: Explorer,
    private val videoPlaybackController: VideoPlaybackController,
    private val scope: CoroutineScope,
) : ExternalMediaRedirectCoordinator {
    private val _events = Channel<ExternalMediaRedirectEvent>(Channel.BUFFERED)
    override val events: Flow<ExternalMediaRedirectEvent> = _events.receiveAsFlow()

    private val mutex = Mutex()
    private var monitoredTarget: MonitoredTarget? = null

    init {
        scope.launch {
            explorer.volumes.collectLatest { volumes ->
                verifyCurrentTarget(volumes)
            }
        }
    }

    /**
     * Updates the active monitored path and stores the backing removable volume when one exists.
     */
    override suspend fun registerTarget(target: ExternalMediaScreenTarget) {
        val monitoredTarget = target.toMonitoredTarget(volumes = explorer.volumes.value)

        if (monitoredTarget == null) {
            clearPath()
            return
        }

        if (!monitoredTarget.isMounted(explorer.volumes.value)) {
            clearPath()
            handleMissingTarget(monitoredTarget)
            return
        }

        mutex.withLock {
            this.monitoredTarget = monitoredTarget
        }
    }

    /**
     * Clears the active target when the caller is leaving the screen that registered it.
     */
    override suspend fun clearPath(path: String?) {
        mutex.withLock {
            if (path == null || monitoredTarget?.path == path) {
                monitoredTarget = null
            }
        }
    }

    /**
     * Refreshes the mounted volumes and emits a redirect if the active removable root disappeared.
     */
    override suspend fun refreshAndVerify() {
        verifyCurrentTarget(explorer.refreshVolumes())
    }

    /**
     * Compares the current mounted volume snapshot with the tracked removable target.
     *
     * A redirect is emitted only when the currently tracked removable root is missing from the latest
     * mounted volumes. The target is cleared before emission so repeated refreshes do not duplicate the
     * event.
     */
    private suspend fun verifyCurrentTarget(volumes: List<Volume>) {
        val target = mutex.withLock {
            monitoredTarget
        } ?: return

        if (target.isMounted(volumes)) {
            return
        }

        val shouldEmit = mutex.withLock {
            if (monitoredTarget == target) {
                monitoredTarget = null
                true
            } else {
                false
            }
        }

        if (shouldEmit) {
            handleMissingTarget(target)
        }
    }

    /**
     * Stops playback durably before notifying the UI that the current removable target disappeared.
     */
    private suspend fun handleMissingTarget(target: MonitoredTarget) {
        videoPlaybackController.stopPlayback()

        val message = target.volumeName.takeIf { it.isNotBlank() }?.let {
            UIText.FromResourceWithArgs(
                id = R.string.external_media_removed_with_name,
                args = arrayOf(it),
            )
        } ?: UIText.FromResource(R.string.external_media_removed)

        _events.send(ExternalMediaRedirectEvent.Redirect(message))
    }
}

/**
 * No-op coordinator used in previews and tests that do not provide the real singleton.
 */
internal data object NoOpExternalMediaRedirectCoordinator : ExternalMediaRedirectCoordinator {
    override val events: Flow<ExternalMediaRedirectEvent> = emptyFlow()

    override suspend fun registerTarget(target: ExternalMediaScreenTarget) {}

    override suspend fun clearPath(path: String?) {}

    override suspend fun refreshAndVerify() {}
}

/**
 * Compose local used by screens to access the app-wide redirect coordinator without threading it
 * through every screen parameter.
 */
internal val LocalExternalMediaRedirectCoordinator = staticCompositionLocalOf<ExternalMediaRedirectCoordinator> {
    NoOpExternalMediaRedirectCoordinator
}

/**
 * Screen-level metadata used to monitor storage loss across resume, restoration, and process death.
 *
 * @property path Absolute path currently explored or viewed.
 * @property removableVolumeRootPath Root path of the removable volume that originally backed [path].
 * @property removableVolumeName User-visible volume label used in the redirect toast.
 */
data class ExternalMediaScreenTarget(
    val path: String,
    val removableVolumeRootPath: String? = null,
    val removableVolumeName: String? = null,
)

/**
 * Tracked target volume metadata for the currently visible screen.
 */
private data class MonitoredTarget(
    val path: String,
    val volumeRootPath: String,
    val volumeName: String,
)

private fun ExternalMediaScreenTarget.toMonitoredTarget(volumes: List<Volume>): MonitoredTarget? {
    removableVolumeRootPath?.let { volumeRootPath ->
        return MonitoredTarget(
            path = path,
            volumeRootPath = volumeRootPath,
            volumeName = removableVolumeName.orEmpty(),
        )
    }

    val resolvedVolume = volumes.resolveVolumeForPath(path)
        ?.takeIf { it.isRemovable }
        ?: return null

    return MonitoredTarget(
        path = path,
        volumeRootPath = resolvedVolume.root.file.absolutePath,
        volumeName = resolvedVolume.name,
    )
}

private fun MonitoredTarget.isMounted(volumes: List<Volume>): Boolean {
    return volumes.any { volume ->
        volume.root.file.absolutePath == volumeRootPath
    }
}
