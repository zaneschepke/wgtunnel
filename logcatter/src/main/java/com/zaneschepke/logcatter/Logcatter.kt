package com.zaneschepke.logcatter

import com.zaneschepke.logcatter.model.LogMessage

object Logcatter {
    fun logs(callback: (input: LogMessage) -> Unit) {
        clear()
        Runtime.getRuntime().exec("logcat -v epoch")
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                lines.forEach { callback(LogMessage.from(it)) }
            }
    }

    fun clear() {
        Runtime.getRuntime().exec("logcat -c")
    }
}
