package xyz.dnieln7.galleryex.core.presentation.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun RowScope.HorizontalSpacer(of: Dp) {
    Spacer(modifier = Modifier.width(of))
}

@Composable
fun RowScope.HorizontalExpandedSpacer() {
    Spacer(modifier = Modifier.weight(1F))
}

@Composable
fun ColumnScope.VerticalSpacer(of: Dp) {
    Spacer(modifier = Modifier.height(of))
}

@Composable
fun ColumnScope.VerticalExpandedSpacer() {
    Spacer(modifier = Modifier.weight(1F))
}
