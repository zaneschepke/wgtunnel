package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.viewmodel.ScannerViewModel

@Composable
fun ScannerScreen(viewModel: ScannerViewModel = hiltViewModel()) {
	val context = LocalContext.current
	val navController = LocalNavController.current

	val success = viewModel.success.collectAsStateWithLifecycle(null)

	LaunchedEffect(success.value) {
		if (success.value != null) navController.popBackStack()
	}

	val barcodeView = remember {
		CompoundBarcodeView(context).apply {
			this.initializeFromIntent((context as Activity).intent)
			this.setStatusText("")
			this.decodeSingle { result ->
				result.text?.let { barCodeOrQr ->
					viewModel.onTunnelQrResult(barCodeOrQr)
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
