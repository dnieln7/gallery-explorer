package xyz.dnieln7.galleryex.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.dnieln7.galleryex.core.data.preferences.DefaultAppPreferences
import xyz.dnieln7.galleryex.core.domain.preferences.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return DefaultAppPreferences(context)
    }
}
