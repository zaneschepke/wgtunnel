package com.zaneschepke.wireguardautotunnel.module

import com.zaneschepke.wireguardautotunnel.service.network.InternetConnectivityService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

	@Binds
	abstract fun provideInternetConnectivityService(wifiService: InternetConnectivityService): NetworkService
}
