package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.RootTunnelActionHandler
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.WireGuardTunnel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
	fun provideRootShellAm(@ApplicationContext context: Context): org.amnezia.awg.util.RootShell {
		return org.amnezia.awg.util.RootShell(context)
	}

	@Provides
	@Singleton
	@Userspace
	fun provideUserspaceBackend(@ApplicationContext context: Context, @TunnelShell rootShell: RootShell): Backend {
		return GoBackend(context, RootTunnelActionHandler(rootShell))
	}

	@Provides
	@Singleton
	@Kernel
	fun provideKernelBackend(@ApplicationContext context: Context, @TunnelShell rootShell: RootShell): Backend {
		return WgQuickBackend(context, rootShell, ToolsInstaller(context, rootShell), RootTunnelActionHandler(rootShell))
	}

	@Provides
	@Singleton
	fun provideAmneziaBackend(@ApplicationContext context: Context, rootShell: org.amnezia.awg.util.RootShell): org.amnezia.awg.backend.Backend {
		return org.amnezia.awg.backend.GoBackend(context, org.amnezia.awg.backend.RootTunnelActionHandler(rootShell))
	}

	@Provides
	@Singleton
	fun provideVpnService(
		amneziaBackend: Provider<org.amnezia.awg.backend.Backend>,
		@Kernel kernelBackend: Provider<Backend>,
		appDataRepository: AppDataRepository,
		tunnelConfigRepository: TunnelConfigRepository,
		@ApplicationScope applicationScope: CoroutineScope,
		@IoDispatcher ioDispatcher: CoroutineDispatcher,
		serviceManager: ServiceManager,
		notificationService: NotificationService,
	): TunnelService {
		return WireGuardTunnel(
			amneziaBackend,
			tunnelConfigRepository,
			kernelBackend,
			appDataRepository,
			applicationScope,
			ioDispatcher,
			serviceManager,
			notificationService,
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
