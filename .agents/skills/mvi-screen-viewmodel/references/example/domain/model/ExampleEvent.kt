package xyz.dnieln7.galleryex.feature.example.domain.model

import xyz.dnieln7.galleryex.core.presentation.text.UIText

sealed interface ExampleEvent {
    data object OnDataSubmitted : ExampleEvent
    data class OnError(val message: UIText) : ExampleEvent
}
