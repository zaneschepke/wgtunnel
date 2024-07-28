package com.zaneschepke.wireguardautotunnel.ui.screens.support.logs

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.logcatter.LocalLogCollector
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.MainDispatcher
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.chunked
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class LogsViewModel
@Inject
constructor(
	private val localLogCollector: LocalLogCollector,
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

	suspend fun saveLogsToFile(): Result<Unit> {
		val file =
			localLogCollector.getLogFile().getOrElse {
				return Result.failure(it)
			}
		val fileContent = fileUtils.readBytesFromFile(file)
		val fileName = "${Constants.BASE_LOG_FILE_NAME}-${Instant.now().epochSecond}.txt"
		return fileUtils.saveByteArrayToDownloads(fileContent, fileName)
	}
}
