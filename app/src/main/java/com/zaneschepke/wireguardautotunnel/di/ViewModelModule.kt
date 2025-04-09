package com.zaneschepke.wireguardautotunnel.di

import android.content.Context
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(ViewModelComponent::class)
class ViewModelModule {
    @ViewModelScoped
    @Provides
    fun provideFileUtils(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): FileUtils {
        return FileUtils(context, ioDispatcher)
    }
}
