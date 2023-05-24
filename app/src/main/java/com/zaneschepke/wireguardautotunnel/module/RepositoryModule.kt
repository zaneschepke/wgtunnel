package com.zaneschepke.wireguardautotunnel.module

import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.repository.SettingsBox
import com.zaneschepke.wireguardautotunnel.repository.TunnelBox
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun provideSettingsRepository(settingsBox: SettingsBox) : Repository<Settings>

    @Binds
    @Singleton
    abstract fun provideTunnelRepository(tunnelBox: TunnelBox) : Repository<TunnelConfig>
}