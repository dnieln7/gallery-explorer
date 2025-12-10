package xyz.dnieln7.galleryex.core.presentation.modifier

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier

fun Modifier.edgeToEdgePadding(): Modifier {
    return this
        .displayCutoutPadding()
        .statusBarsPadding()
        .navigationBarsPadding()
}


