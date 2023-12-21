package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.service.tunnel.WireGuardTunnel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TunnelModule {
    @Provides
    @Singleton
    fun provideRootShell(
        @ApplicationContext context: Context
    ): RootShell {
        return RootShell(context)
    }

    @Provides
    @Singleton
    @Userspace
    fun provideUserspaceBackend(
        @ApplicationContext context: Context
    ): Backend {
        return GoBackend(context)
    }

    @Provides
    @Singleton
    @Kernel
    fun provideKernelBackend(
        @ApplicationContext context: Context,
        rootShell: RootShell
    ): Backend {
        return WgQuickBackend(context, rootShell, ToolsInstaller(context, rootShell))
    }

    @Provides
    @Singleton
    fun provideVpnService(
        @Userspace userspaceBackend: Backend,
        @Kernel kernelBackend: Backend,
        settingsDoa: SettingsDoa
    ): VpnService {
        return WireGuardTunnel(userspaceBackend, kernelBackend, settingsDoa)
    }
}
