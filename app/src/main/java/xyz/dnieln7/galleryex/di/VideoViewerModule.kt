package xyz.dnieln7.galleryex.di

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Hilt module that provides video-playback dependencies scoped to the ViewModel lifecycle.
 *
 * Using [ViewModelScoped] ensures that each [VideoViewerViewModel][xyz.dnieln7.galleryex.feature.viewer.presentation.screen.VideoViewerViewModel]
 * receives its own [ExoPlayer] instance that is released when the ViewModel is cleared.
 */
@Module
@InstallIn(ViewModelComponent::class)
object VideoViewerModule {
    /**
     * Provides a pre-configured [ExoPlayer] instance.
     *
     * The player is built with seek increments set to [PLAYER_SEEK_INCREMENT_MS] and
     * repeat mode [Player.REPEAT_MODE_ONE] so that each video loops until the user swipes
     * to the next one.
     *
     * @param context Application context used to build the player.
     * @return A ready-to-use [ExoPlayer] instance.
     */
    @Provides
    @ViewModelScoped
    fun provideExoPlayer(
        @ApplicationContext context: Context,
    ): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(PLAYER_SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(PLAYER_SEEK_INCREMENT_MS)
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_ONE }
    }
}

private const val PLAYER_SEEK_INCREMENT_MS = 10_000L
