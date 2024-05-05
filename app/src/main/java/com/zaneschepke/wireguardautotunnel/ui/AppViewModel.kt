package com.zaneschepke.wireguardautotunnel.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.wireguard.android.backend.GoBackend
import com.zaneschepke.logcatter.Logcatter
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AppViewModel
@Inject
constructor() : ViewModel() {

    val vpnIntent: Intent? = GoBackend.VpnService.prepare(WireGuardAutoTunnel.instance)

    private val _appUiState = MutableStateFlow(
        AppUiState(
            vpnPermissionAccepted = vpnIntent == null,
        ),
    )
    val appUiState = _appUiState.asStateFlow()


    fun isRequiredPermissionGranted(): Boolean {
        val allAccepted =
            (_appUiState.value.vpnPermissionAccepted && _appUiState.value.vpnPermissionAccepted)
        if (!allAccepted) requestPermissions()
        return allAccepted
    }

    private fun requestPermissions() {
        _appUiState.update {
            it.copy(
                requestPermissions = true
            )
        }
    }

    fun permissionsRequested() {
        _appUiState.update {
            it.copy(
                requestPermissions = false
            )
        }
    }

    fun openWebPage(url: String, context : Context) {
        try {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            showSnackbarMessage(context.getString(R.string.no_browser_detected))
        }
    }

    fun onVpnPermissionAccepted() {
        _appUiState.update {
            it.copy(
                vpnPermissionAccepted = true
            )
        }
    }

    fun launchEmail(context: Context) {
        try {
            val intent =
                Intent(Intent.ACTION_SENDTO).apply {
                    type = Constants.EMAIL_MIME_TYPE
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.my_email)))
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.email_chooser)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            showSnackbarMessage(context.getString(R.string.no_email_detected))
        }
    }

    fun showSnackbarMessage(message: String) {
        _appUiState.update {
            it.copy(
                snackbarMessage = message,
                snackbarMessageConsumed = false,
            )
        }
    }

    fun snackbarMessageConsumed() {
        _appUiState.update {
            it.copy(
                snackbarMessage = "",
                snackbarMessageConsumed = true,
            )
        }
    }

    val logs = mutableStateListOf<LogMessage>()

    fun readLogCatOutput() =
        viewModelScope.launch(viewModelScope.coroutineContext + Dispatchers.IO) {
            launch {
                Logcatter.logs(callback = {
                    logs.add(it)
                    if (logs.size > Constants.LOG_BUFFER_SIZE) {
                        logs.removeRange(0, (logs.size - Constants.LOG_BUFFER_SIZE).toInt())
                    }
                })
            }
        }

    fun clearLogs() {
        logs.clear()
        Logcatter.clear()
    }

    fun saveLogsToFile(context: Context) {
        val fileName = "${Constants.BASE_LOG_FILE_NAME}-${Instant.now().epochSecond}.txt"
        val content = logs.joinToString(separator = "\n")
        FileUtils.saveFileToDownloads(context.applicationContext, content, fileName)
        Toast.makeText(context, context.getString(R.string.logs_saved), Toast.LENGTH_SHORT)
            .show()
    }

    fun setNotificationPermissionAccepted(accepted: Boolean) {
        _appUiState.update {
            it.copy(
                notificationPermissionAccepted = accepted,
            )
        }
    }
}
