package com.zaneschepke.wireguardautotunnel.di

import android.content.Context
import androidx.room.Room
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.data.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.DatabaseCallback
import com.zaneschepke.wireguardautotunnel.data.dao.SettingsDao
import com.zaneschepke.wireguardautotunnel.data.dao.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.network.GitHubApi
import com.zaneschepke.wireguardautotunnel.data.network.KtorClient
import com.zaneschepke.wireguardautotunnel.data.network.KtorGitHubApi
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRoomRepository
import com.zaneschepke.wireguardautotunnel.data.repository.DataStoreAppStateRepository
import com.zaneschepke.wireguardautotunnel.data.repository.GitHubUpdateRepository
import com.zaneschepke.wireguardautotunnel.data.repository.RoomSettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.RoomTunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.AppSettingRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.domain.repository.UpdateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                context.getString(R.string.db_name),
            )
            .fallbackToDestructiveMigration(true)
            .addCallback(DatabaseCallback())
            .build()
    }

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
    fun provideTunnelConfigRepository(
        tunnelConfigDao: TunnelConfigDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): TunnelRepository {
        return RoomTunnelRepository(tunnelConfigDao, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideSettingsRepository(
        settingsDao: SettingsDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AppSettingRepository {
        return RoomSettingsRepository(settingsDao, ioDispatcher)
    }

    @Singleton
    @Provides
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStoreManager {
        return DataStoreManager(context, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGeneralStateRepository(dataStoreManager: DataStoreManager): AppStateRepository {
        return DataStoreAppStateRepository(dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideAppDataRepository(
        settingsRepository: AppSettingRepository,
        tunnelRepository: TunnelRepository,
        appStateRepository: AppStateRepository,
    ): AppDataRepository {
        return AppDataRoomRepository(settingsRepository, tunnelRepository, appStateRepository)
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return KtorClient.create()
    }

    @Provides
    @Singleton
    fun provideGitHubApi(client: HttpClient): GitHubApi {
        return KtorGitHubApi(client)
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(
        gitHubApi: GitHubApi,
        client: HttpClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @ApplicationContext context: Context,
    ): UpdateRepository {
        return GitHubUpdateRepository(
            gitHubApi,
            client,
            "wgtunnel",
            "wgtunnel",
            context,
            ioDispatcher,
        )
    }
}
