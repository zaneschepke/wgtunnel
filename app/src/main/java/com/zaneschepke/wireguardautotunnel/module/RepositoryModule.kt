package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import com.zaneschepke.wireguardautotunnel.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.data.SettingsDao
import com.zaneschepke.wireguardautotunnel.data.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRoomRepository
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.data.repository.DataStoreAppStateRepository
import com.zaneschepke.wireguardautotunnel.data.repository.RoomSettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.RoomTunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
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
        return RoomTunnelConfigRepository(tunnelConfigDao)
    }

    @Singleton
    @Provides
    fun provideSettingsRepository(settingsDao: SettingsDao): SettingsRepository {
        return RoomSettingsRepository(settingsDao)
    }

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideGeneralStateRepository(dataStoreManager: DataStoreManager): AppStateRepository {
        return DataStoreAppStateRepository(dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideAppDataRepository(
        settingsRepository: SettingsRepository,
        tunnelConfigRepository: TunnelConfigRepository,
        appStateRepository: AppStateRepository
    ): AppDataRepository {
        return AppDataRoomRepository(settingsRepository, tunnelConfigRepository, appStateRepository)
    }


}
