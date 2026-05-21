package xyz.dnieln7.galleryex.di

import android.content.Context
import coil.ImageLoader
import coil.imageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import xyz.dnieln7.galleryex.core.domain.media.ExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.core.framework.explorer.Explorer
import xyz.dnieln7.galleryex.core.framework.media.DefaultExternalMediaRedirectCoordinator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SingletonModule {
    @Provides
    @Singleton
    fun provideExplorer(
        @ApplicationContext context: Context,
    ): Explorer {
        return Explorer(context)
    }

    /**
     * Provides Coil's singleton [ImageLoader], which includes the [coil.decode.VideoFrameDecoder]
     * registered automatically by the `coil-video` artifact. Used by [xyz.dnieln7.galleryex.feature.viewer.presentation.screen.VideoViewerViewModel]
     * to proactively warm the memory cache with first-frame thumbnails for adjacent videos.
     *
     * @param context Application context used to retrieve Coil's singleton.
     * @return The application-scoped [ImageLoader] instance.
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader {
        return context.imageLoader
    }

    @Provides
    @Singleton
    fun provideExternalMediaRedirectCoordinator(
        explorer: Explorer,
        @ApplicationScope scope: CoroutineScope,
    ): ExternalMediaRedirectCoordinator {
        return DefaultExternalMediaRedirectCoordinator(
            explorer = explorer,
            scope = scope,
        )
    }
}
