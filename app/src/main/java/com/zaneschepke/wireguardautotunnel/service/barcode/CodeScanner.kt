package com.zaneschepke.wireguardautotunnel.service.barcode

import kotlinx.coroutines.flow.Flow

interface CodeScanner {
    fun scan() : Flow<String?>
}