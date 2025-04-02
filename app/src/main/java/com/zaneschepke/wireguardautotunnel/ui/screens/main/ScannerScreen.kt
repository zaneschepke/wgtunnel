package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun ScannerScreen(viewModel: AppViewModel) {
	val context = LocalContext.current

	val barcodeView = remember {
		CompoundBarcodeView(context).apply {
			this.initializeFromIntent((context as Activity).intent)
			this.setStatusText("")
			this.decodeSingle { result ->
				result.text?.let { barCodeOrQr ->
					viewModel.handleEvent(AppEvent.ImportTunnelFromQrCode(barCodeOrQr))
				}
			}
		}
	}
	AndroidView(factory = { barcodeView })
	DisposableEffect(Unit) {
		barcodeView.resume()
		onDispose {
			barcodeView.pause()
		}
	}
}
