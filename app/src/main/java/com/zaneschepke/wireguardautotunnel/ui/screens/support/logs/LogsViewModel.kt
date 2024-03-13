package com.zaneschepke.wireguardautotunnel.ui.screens.support.logs

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.logcatter.Logcatter
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject



@HiltViewModel
class LogsViewModel
@Inject
constructor(
    private val application: Application
) : ViewModel() {

    val logs = mutableStateListOf<LogMessage>()

    fun readLogCatOutput() = viewModelScope.launch(viewModelScope.coroutineContext + Dispatchers.IO) {
        launch {
            Logcatter.logs {
                logs.add(it)
                if (logs.size > Constants.LOG_BUFFER_SIZE) {
                    logs.removeRange(0, (logs.size - Constants.LOG_BUFFER_SIZE).toInt())
                }
            }
        }
    }

    fun clearLogs() {
        logs.clear()
        Logcatter.clear()
    }

    fun saveLogsToFile() {
        val fileName = "${Constants.BASE_LOG_FILE_NAME}-${Instant.now().epochSecond}.txt"
        val content = logs.joinToString(separator = "\n")
        FileUtils.saveFileToDownloads(application.applicationContext, content, fileName)
        Toast.makeText(application, application.getString(R.string.logs_saved), Toast.LENGTH_SHORT).show()
    }

}
