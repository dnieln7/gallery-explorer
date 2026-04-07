package xyz.dnieln7.galleryex.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import xyz.dnieln7.galleryex.core.framework.explorer.Explorer
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.DefaultVideoPlaybackController
import xyz.dnieln7.galleryex.feature.viewer.framework.playback.VideoPlaybackController
import xyz.dnieln7.galleryex.main.framework.DefaultExternalMediaRedirectCoordinator
import xyz.dnieln7.galleryex.main.framework.ExternalMediaRedirectCoordinator
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

    @Provides
    @Singleton
    fun provideVideoPlaybackController(
        defaultVideoPlaybackController: DefaultVideoPlaybackController,
    ): VideoPlaybackController {
        return defaultVideoPlaybackController
    }

    @Provides
    @Singleton
    fun provideExternalMediaRedirectCoordinator(
        explorer: Explorer,
        videoPlaybackController: VideoPlaybackController,
        @ApplicationScope scope: CoroutineScope,
    ): ExternalMediaRedirectCoordinator {
        return DefaultExternalMediaRedirectCoordinator(
            explorer = explorer,
            videoPlaybackController = videoPlaybackController,
            scope = scope,
        )
    }
}
