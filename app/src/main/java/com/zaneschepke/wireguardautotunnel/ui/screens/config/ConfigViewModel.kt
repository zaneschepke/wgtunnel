package com.zaneschepke.wireguardautotunnel.ui.screens.config

import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.config.model.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.screens.config.model.PeerProxy
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.removeAt
import com.zaneschepke.wireguardautotunnel.util.extensions.update
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

	private val packageManager = WireGuardAutoTunnel.instance.packageManager

	fun saveConfigChanges(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(tunnelConfig)
		SnackbarController.showMessage(StringValue.StringResource(R.string.config_changes_saved))
	}

	//TODO test this
	fun cleanUpUninstalledApps(tunnelConfig: TunnelConfig) = viewModelScope.launch(ioDispatcher) {
		val amConfig = tunnelConfig.toAmConfig()
		val wgConfig = tunnelConfig.toWgConfig()
		val packages = getQueriedPackages()
		val packageSet = packages.map { pack -> pack.packageName }.toSet()
		val includedApps = amConfig.`interface`.includedApplications.toMutableList()
		val excludedApps = amConfig.`interface`.excludedApplications.toMutableList()
		if (includedApps.isEmpty() && excludedApps.isEmpty()) return@launch
		if (includedApps.retainAll(packageSet) || excludedApps.retainAll(packageSet)) {
			amConfig.`interface`.excludedApplications.addAll(includedApps)
			amConfig.`interface`.includedApplications.addAll(excludedApps)
			wgConfig.`interface`.excludedApplications.addAll(excludedApps)
			wgConfig.`interface`.includedApplications.addAll(includedApps)
			saveConfigChanges(
				tunnelConfig.copy(
					amQuick = amConfig.toAwgQuickString(true),
					wgQuick = wgConfig.toWgQuickString(true),
				),
			)
		}
	}

	private fun getQueriedPackages(query: String = ""): List<PackageInfo> {
		return getAllInternetCapablePackages().filter {
			getPackageLabel(it).lowercase().contains(query.lowercase())
		}
	}

	fun getPackageLabel(packageInfo: PackageInfo): String {
		return packageInfo.applicationInfo?.loadLabel(packageManager).toString()
	}

	private fun getAllInternetCapablePackages(): List<PackageInfo> {
		return getPackagesHoldingPermissions(arrayOf(Manifest.permission.INTERNET))
	}

	private fun getPackagesHoldingPermissions(permissions: Array<String>): List<PackageInfo> {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			packageManager.getPackagesHoldingPermissions(
				permissions,
				PackageManager.PackageInfoFlags.of(0L),
			)
		} else {
			packageManager.getPackagesHoldingPermissions(permissions, 0)
		}
	}
}
