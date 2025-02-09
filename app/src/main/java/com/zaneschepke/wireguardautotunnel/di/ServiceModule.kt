package com.zaneschepke.wireguardautotunnel.di

import com.zaneschepke.wireguardautotunnel.core.network.InternetConnectivityMonitor
import com.zaneschepke.wireguardautotunnel.core.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

	@Binds
	abstract fun provideInternetConnectivityService(wifiService: InternetConnectivityMonitor): NetworkMonitor
}
