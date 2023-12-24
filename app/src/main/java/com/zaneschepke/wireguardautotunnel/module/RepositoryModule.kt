package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import com.zaneschepke.wireguardautotunnel.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.data.SettingsDao
import com.zaneschepke.wireguardautotunnel.data.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepositoryImpl
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {
    @Singleton
    @Provides
    fun provideSettingsDoa(appDatabase: AppDatabase): SettingsDao {
        return appDatabase.settingDao()
    }

    @Singleton
    @Provides
    fun provideTunnelConfigDoa(appDatabase: AppDatabase): TunnelConfigDao {
        return appDatabase.tunnelConfigDoa()
    }

    @Singleton
    @Provides
    fun provideTunnelConfigRepository(tunnelConfigDao: TunnelConfigDao): TunnelConfigRepository {
        return TunnelConfigRepositoryImpl(tunnelConfigDao)
    }

    @Singleton
    @Provides
    fun provideSettingsRepository(settingsDao: SettingsDao): SettingsRepository {
        return SettingsRepositoryImpl(settingsDao)
    }

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }
}
