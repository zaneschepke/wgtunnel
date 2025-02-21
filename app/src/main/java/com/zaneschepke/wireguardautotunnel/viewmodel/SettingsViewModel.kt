package com.zaneschepke.wireguardautotunnel.viewmodel

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.extensions.launchShareFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	private val fileUtils: FileUtils,
) : ViewModel() {

	fun exportAllConfigs(context: Context, configType: ConfigType) = viewModelScope.launch {
		runCatching {
			val tunnels = appDataRepository.tunnels.getAll()
			val (files, shareFileName) = when (configType) {
				ConfigType.AMNEZIA -> {
					Pair(fileUtils.createAmFiles(tunnels), "am-export_${Instant.now().epochSecond}.zip")
				}
				ConfigType.WG -> {
					Pair(fileUtils.createWgFiles(tunnels), "wg-export_${Instant.now().epochSecond}.zip")
				}
			}
			val shareFile = fileUtils.createNewShareFile(shareFileName)
			fileUtils.zipAll(shareFile, files)
			val uri = FileProvider.getUriForFile(context, context.getString(R.string.provider), shareFile)
			context.launchShareFile(uri)
		}.onFailure {
			Timber.e(it)
		}
	}
}
