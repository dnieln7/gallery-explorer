package xyz.dnieln7.galleryex.feature.example.presentation.error

import xyz.dnieln7.galleryex.feature.example.domain.error.ExampleError

fun Throwable.toExampleError(): ExampleError {
    return when (this) {
        is IllegalStateException -> ExampleError.EmptyInput
        else -> ExampleError.Other(localizedMessage ?: message ?: this.toString())
    }
}
