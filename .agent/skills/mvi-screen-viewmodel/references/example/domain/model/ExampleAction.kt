package xyz.dnieln7.galleryex.feature.example.domain.model

sealed interface ExampleAction {
    data object OnRefresh : ExampleAction
    data class OnInputChanged(val input: String) : ExampleAction
    data object OnSubmitClick : ExampleAction
}
