package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.zaneschepke.wireguardautotunnel.service.barcode.CodeScanner
import com.zaneschepke.wireguardautotunnel.service.barcode.QRScanner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class ScannerModule {

    @ViewModelScoped
    @Provides
    fun provideBarCodeOptions() : GmsBarcodeScannerOptions {
        return GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }

    @ViewModelScoped
    @Provides
    fun provideBarCodeScanner(@ApplicationContext context: Context, options: GmsBarcodeScannerOptions) : GmsBarcodeScanner {
        return GmsBarcodeScanning.getClient(context, options)
    }

    @ViewModelScoped
    @Provides
    fun provideQRScanner(gmsBarcodeScanner: GmsBarcodeScanner) : CodeScanner {
        return QRScanner(gmsBarcodeScanner)
    }
}