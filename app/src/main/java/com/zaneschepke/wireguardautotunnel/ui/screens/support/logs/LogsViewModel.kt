package com.zaneschepke.wireguardautotunnel.ui.screens.support.logs

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.MainDispatcher
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.extensions.chunked
import com.zaneschepke.wireguardautotunnel.util.extensions.launchShareFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class LogsViewModel
@Inject
constructor(
	private val localLogCollector: LogReader,
	private val fileUtils: FileUtils,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ViewModel() {
	val logs = mutableStateListOf<LogMessage>()

	init {
		viewModelScope.launch(ioDispatcher) {
			localLogCollector.bufferedLogs.chunked(500, Duration.ofSeconds(1)).collect {
				withContext(mainDispatcher) {
					logs.addAll(it)
				}
				if (logs.size > Constants.LOG_BUFFER_SIZE) {
					withContext(mainDispatcher) {
						logs.removeRange(0, (logs.size - Constants.LOG_BUFFER_SIZE).toInt())
					}
				}
			}
		}
	}

	fun shareLogs(context: Context): Job = viewModelScope.launch(ioDispatcher) {
		runCatching {
			val file = fileUtils.createNewShareFile("${Constants.BASE_LOG_FILE_NAME}-${Instant.now().epochSecond}.zip")
			localLogCollector.zipLogFiles(file.absolutePath)
			val uri = FileProvider.getUriForFile(context, context.getString(R.string.provider), file)
			context.launchShareFile(uri)
		}.onFailure {
			Timber.e(it)
		}
	}
}
