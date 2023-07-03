package com.zaneschepke.wireguardautotunnel.service.barcode

import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

class QRScanner @Inject constructor(private val gmsBarcodeScanner: GmsBarcodeScanner) : CodeScanner {
    override fun scan(): Flow<String?> {
        return callbackFlow {
            gmsBarcodeScanner.startScan().addOnSuccessListener {
                trySend(it.rawValue)
            }.addOnFailureListener {
                Timber.e(it.message)
            }
            awaitClose {
            }
        }
    }
}