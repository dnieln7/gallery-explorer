package xyz.dnieln7.galleryex.feature.example.domain.model

data class ExampleState(
    val loading: Boolean = false,
    val input: String = "",
    val data: List<String> = emptyList(),
)
