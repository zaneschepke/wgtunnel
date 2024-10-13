package com.zaneschepke.logcatter

import com.zaneschepke.logcatter.model.LogMessage
import kotlinx.coroutines.flow.Flow

interface LogReader {
	suspend fun start(onLogMessage: ((message: LogMessage) -> Unit)? = null)
	fun stop()
	fun zipLogFiles(path: String)
	suspend fun deleteAndClearLogs()
	val bufferedLogs: Flow<LogMessage>
	val liveLogs: Flow<LogMessage>
}
