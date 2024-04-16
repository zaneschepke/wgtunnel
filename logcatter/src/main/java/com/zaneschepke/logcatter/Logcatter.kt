package com.zaneschepke.logcatter

import com.zaneschepke.logcatter.model.LogMessage

object Logcatter {

    private val findKeyRegex = """[A-Za-z0-9+/]{42}[AEIMQUYcgkosw480]=""".toRegex()
    private val findIpv6AddressRegex = """(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))""".toRegex()
    private val findIpv4AddressRegex = """((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}""".toRegex()
    private val findTunnelNameRegex = """(?<=tunnel ).*?(?= UP| DOWN)""".toRegex()


    fun logs(callback: (input: LogMessage) -> Unit, obfuscator: (log : String) -> String = { log ->  this.obfuscator(log)}){
        clear()
        Runtime.getRuntime().exec("logcat -v epoch")
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                lines.forEach { callback(LogMessage.from(obfuscator(it))) }
        }
    }

    private fun obfuscator(log : String) : String {
        return findKeyRegex.replace(log, "<crypto-key>").let { first ->
            findIpv6AddressRegex.replace(first, "<ipv6-address>").let { second ->
                findTunnelNameRegex.replace(second, "<tunnel>")
            }
        }.let{ last ->  findIpv4AddressRegex.replace(last,"<ipv4-address>") }
    }

    fun clear() {
        Runtime.getRuntime().exec("logcat -c")
    }
}
