package com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.config

import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

	private val packageManager = WireGuardAutoTunnel.instance.packageManager

	fun saveConfigChanges(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(tunnelConfig)
		SnackbarController.showMessage(StringValue.StringResource(R.string.config_changes_saved))
	}

	private fun getQueriedPackages(query: String = ""): List<PackageInfo> {
		return getAllInternetCapablePackages().filter {
			getPackageLabel(it).lowercase().contains(query.lowercase())
		}
	}

	private fun getPackageLabel(packageInfo: PackageInfo): String {
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
