package com.zaneschepke.wireguardautotunnel.module

import com.zaneschepke.wireguardautotunnel.repository.AppDatabase
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Singleton
    @Provides
    fun provideSettingsRepository(appDatabase: AppDatabase) : SettingsDoa {
        return appDatabase.settingDao()
    }

    @Singleton
    @Provides
    fun provideTunnelConfigRepository(appDatabase: AppDatabase) : TunnelConfigDao {
        return appDatabase.tunnelConfigDoa()
    }
}