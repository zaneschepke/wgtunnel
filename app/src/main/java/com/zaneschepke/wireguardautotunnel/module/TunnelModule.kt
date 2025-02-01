package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.RootTunnelActionHandler
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelFactory
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.UserspaceTunnel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.amnezia.awg.backend.TunnelActionHandler
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TunnelModule {

	@Provides
	@Singleton
	@TunnelShell
	fun provideTunnelRootShell(@ApplicationContext context: Context): RootShell {
		return RootShell(context)
	}

	@Provides
	@Singleton
	@AppShell
	fun provideAppRootShell(@ApplicationContext context: Context): RootShell {
		return RootShell(context)
	}

	@Provides
	@Singleton
	fun provideAmneziaBackend(@ApplicationContext context: Context): org.amnezia.awg.backend.Backend {
		return org.amnezia.awg.backend.GoBackend(context, org.amnezia.awg.backend.RootTunnelActionHandler(org.amnezia.awg.util.RootShell(context)))
	}

	@Provides
	@Singleton
	fun provideKernelTunnelFactory(
		@ApplicationContext context: Context,
		amBackend: Provider<org.amnezia.awg.backend.Backend>,
		appDataRepository: AppDataRepository,
		@ApplicationScope applicationScope: CoroutineScope,
		@IoDispatcher ioDispatcher: CoroutineDispatcher,
		serviceManager: ServiceManager,
		internetConnectivityService: NetworkService,
	): TunnelFactory {
		return TunnelFactory(
			ioDispatcher,
			applicationScope,
			serviceManager,
			appDataRepository,
			amBackend.get(),
			internetConnectivityService,
			context,
		)
	}

	@Singleton
	@Provides
	fun provideServiceManager(
		@ApplicationContext context: Context,
		@IoDispatcher ioDispatcher: CoroutineDispatcher,
		appDataRepository: AppDataRepository,
	): ServiceManager {
		return ServiceManager(context, ioDispatcher, appDataRepository)
	}
}
