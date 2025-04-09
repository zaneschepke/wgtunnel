package com.zaneschepke.wireguardautotunnel.util

object InvalidFileExtensionException : Exception() {
    private fun readResolve(): Any = InvalidFileExtensionException
}

object FileReadException : Exception() {
    private fun readResolve(): Any = FileReadException
}

object ConfigExportException : Exception() {
    private fun readResolve(): Any = ConfigExportException
}
