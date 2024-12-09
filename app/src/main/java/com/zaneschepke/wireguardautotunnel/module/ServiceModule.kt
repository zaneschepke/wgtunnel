package com.zaneschepke.wireguardautotunnel.module

import com.zaneschepke.wireguardautotunnel.service.network.EthernetService
import com.zaneschepke.wireguardautotunnel.service.network.MobileDataService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.network.WifiService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
abstract class ServiceModule {
	@Binds
	@ServiceScoped
	abstract fun provideWifiService(wifiService: WifiService): NetworkService<WifiService>

	@Binds
	@ServiceScoped
	abstract fun provideMobileDataService(mobileDataService: MobileDataService): NetworkService<MobileDataService>

	@Binds
	@ServiceScoped
	abstract fun provideEthernetService(ethernetService: EthernetService): NetworkService<EthernetService>
}
