package xyz.dnieln7.galleryex.feature.example.domain.error

import xyz.dnieln7.galleryex.core.domain.error.Error

sealed interface ExampleError : Error {
    data object EmptyInput : ExampleError
    data class Other(val message: String) : ExampleError
}


