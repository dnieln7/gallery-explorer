package xyz.dnieln7.galleryex.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.dnieln7.galleryex.core.framework.explorer.Explorer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SingletonModule {

    @Provides
    @Singleton
    fun provideExplorer(@ApplicationContext context: Context): Explorer {
        return Explorer(context)
    }
}
