package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.MyObjectBox
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import io.objectbox.BoxStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class BoxModule {

    @Provides
    @Singleton
    fun provideBoxStore(@ApplicationContext context : Context) : BoxStore {
        return MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }

    @Provides
    @Singleton
    fun provideBoxForSettings(store : BoxStore) : Box<Settings> {
        return store.boxFor(Settings::class.java)
    }

    @Provides
    @Singleton
    fun provideBoxForTunnels(store : BoxStore) : Box<TunnelConfig> {
        return store.boxFor(TunnelConfig::class.java)
    }

}