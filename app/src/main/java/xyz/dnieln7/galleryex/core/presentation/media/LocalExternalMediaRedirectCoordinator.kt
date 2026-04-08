package xyz.dnieln7.galleryex.core.presentation.media

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaRedirectEvent
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaScreenTarget

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
