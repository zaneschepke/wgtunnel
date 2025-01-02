package com.zaneschepke.wireguardautotunnel.module

import com.zaneschepke.wireguardautotunnel.service.network.EthernetService
import com.zaneschepke.wireguardautotunnel.service.network.MobileDataService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.network.WifiService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

	@Binds
	@Wifi
	abstract fun provideWifiService(wifiService: WifiService): NetworkService

	@Binds
	@MobileData
	abstract fun provideMobileDataService(mobileDataService: MobileDataService): NetworkService

	@Binds
	@Ethernet
	abstract fun provideEthernetService(ethernetService: EthernetService): NetworkService
}
