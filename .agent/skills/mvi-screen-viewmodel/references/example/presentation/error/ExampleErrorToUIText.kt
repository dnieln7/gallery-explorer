package xyz.dnieln7.galleryex.feature.example.presentation.error

import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.core.presentation.text.UIText
import xyz.dnieln7.galleryex.feature.example.domain.error.ExampleError

fun ExampleError.toUIText(): UIText {
    return when (this) {
        ExampleError.EmptyInput -> UIText.FromResource(R.string.input_error)
        is ExampleError.Other -> UIText.FromString(message)
    }
}
