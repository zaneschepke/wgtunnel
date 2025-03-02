package com.zaneschepke.wireguardautotunnel.di

import android.content.Context
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.KernelTunnel
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelProvider
import com.zaneschepke.wireguardautotunnel.core.tunnel.UserspaceTunnel
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.GoBackend
import org.amnezia.awg.backend.RootTunnelActionHandler
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
	fun provideAmneziaBackend(@ApplicationContext context: Context): Backend {
		return GoBackend(context, RootTunnelActionHandler(org.amnezia.awg.util.RootShell(context)))
	}

	@Provides
	@Singleton
	fun provideKernelBackend(@ApplicationContext context: Context, @TunnelShell shell: RootShell): com.wireguard.android.backend.Backend {
		return WgQuickBackend(context, shell, ToolsInstaller(context, shell), com.wireguard.android.backend.RootTunnelActionHandler(shell)).also {
			it.setMultipleTunnels(true)
		}
	}

	@Provides
	@Singleton
	@Kernel
	fun provideKernelProvider(
		@IoDispatcher ioDispatcher: CoroutineDispatcher,
		@ApplicationScope applicationScope: CoroutineScope,
		serviceManager: ServiceManager,
		appDataRepository: AppDataRepository,
		networkMonitor: NetworkMonitor,
		notificationManager: NotificationManager,
		backend: com.wireguard.android.backend.Backend,
	): TunnelProvider {
		return KernelTunnel(ioDispatcher, applicationScope, serviceManager, appDataRepository, notificationManager, backend, networkMonitor)
	}

	@Provides
	@Singleton
	@Userspace
	fun provideUserspaceProvider(
		@IoDispatcher ioDispatcher: CoroutineDispatcher,
		@ApplicationScope applicationScope: CoroutineScope,
		serviceManager: ServiceManager,
		appDataRepository: AppDataRepository,
		notificationManager: NotificationManager,
		networkMonitor: NetworkMonitor,
		backend: Backend,
	): TunnelProvider {
		return UserspaceTunnel(ioDispatcher, applicationScope, serviceManager, appDataRepository, notificationManager, backend, networkMonitor)
	}

	@Provides
	@Singleton
	fun provideTunnelManager(
		@Kernel kernelTunnel: TunnelProvider,
		@Userspace userspaceTunnel: TunnelProvider,
		appDataRepository: AppDataRepository,
		@IoDispatcher ioDispatcher: CoroutineDispatcher,
		@ApplicationScope applicationScope: CoroutineScope,
	): TunnelManager {
		return TunnelManager(kernelTunnel, userspaceTunnel, appDataRepository, applicationScope, ioDispatcher)
	}

	@Provides
	@Singleton
	fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
		return AndroidNetworkMonitor(context)
	}

	@Singleton
	@Provides
	fun provideServiceManager(
		@ApplicationContext context: Context,
		@IoDispatcher ioDispatcher: CoroutineDispatcher,
		@ApplicationScope applicationScope: CoroutineScope,
		appDataRepository: AppDataRepository,
	): ServiceManager {
		return ServiceManager(context, ioDispatcher, applicationScope, appDataRepository)
	}
}
