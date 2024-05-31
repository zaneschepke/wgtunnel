package com.zaneschepke.logcatter

import com.zaneschepke.logcatter.model.LogMessage
import kotlinx.coroutines.flow.Flow
import java.io.File

interface LocalLogCollector {
    fun start(onLogMessage: ((message: LogMessage) -> Unit)? = null)
    fun stop()
    suspend fun getLogFile(): Result<File>
    val bufferedLogs: Flow<LogMessage>
}

