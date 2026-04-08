package xyz.dnieln7.galleryex.core.domain.media

import xyz.dnieln7.galleryex.core.presentation.text.UIText

/**
 * One-shot commands emitted by the external media redirect coordinator.
 */
sealed interface ExternalMediaRedirectEvent {
    /**
     * Redirects the user back to home with an explanatory toast.
     *
     * @property message Toast text that explains why the redirect happened.
     */
    data class Redirect(val message: UIText) : ExternalMediaRedirectEvent
}
