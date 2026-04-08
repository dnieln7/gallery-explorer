package xyz.dnieln7.galleryex.core.domain.media

import kotlinx.coroutines.flow.Flow

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
     * Screens call this when they become visible so the coordinator knows which mounted storage
     * should be watched for removal.
     *
     * @param target Screen metadata used to resolve the removable volume being monitored.
     */
    suspend fun registerTarget(target: ExternalMediaScreenTarget)

    /**
     * Stops monitoring the provided path when it is no longer the active screen.
     *
     * Passing a path that no longer matches the current target is harmless and leaves the active
     * target unchanged.
     *
     * @param path Absolute path that should be cleared from monitoring.
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
