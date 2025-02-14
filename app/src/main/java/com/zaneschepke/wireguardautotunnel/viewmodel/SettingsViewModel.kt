package com.zaneschepke.wireguardautotunnel.viewmodel

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.extensions.launchShareFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	private val fileUtils: FileUtils,
) : ViewModel() {

	fun exportAllConfigs(context: Context) = viewModelScope.launch {
		runCatching {
			val shareFile = fileUtils.createNewShareFile("wg-export_${Instant.now().epochSecond}.zip")
			val tunnels = appDataRepository.tunnels.getAll()
			val wgFiles = fileUtils.createWgFiles(tunnels)
			val amFiles = fileUtils.createAmFiles(tunnels)
			val allFiles = wgFiles + amFiles
			fileUtils.zipAll(shareFile, allFiles)
			val uri = FileProvider.getUriForFile(context, context.getString(R.string.provider), shareFile)
			context.launchShareFile(uri)
		}
	}
}
