package xyz.dnieln7.galleryex.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun <E> CollectEventsWithLifeCycle(
    events: Flow<E>,
    block: (E) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(key1 = lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
            events.collectLatest { block(it) }
        }
    }
}
