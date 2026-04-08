package xyz.dnieln7.galleryex.core.framework.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaRedirectEvent
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaScreenTarget
import xyz.dnieln7.galleryex.core.domain.model.Volume
import xyz.dnieln7.galleryex.core.framework.explorer.Explorer
import xyz.dnieln7.galleryex.core.framework.explorer.resolveVolumeForPath
import xyz.dnieln7.galleryex.core.presentation.text.UIText
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackController
import javax.inject.Singleton

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
     * mounted volumes. The target is cleared before emission so repeated refreshes do not duplicate
     * the event.
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
