package com.zaneschepke.wireguardautotunnel.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.GoBackend
import com.zaneschepke.logcatter.Logcatter
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AppViewModel
@Inject
constructor(
    private val application: Application,
) : ViewModel() {
    
    val vpnIntent: Intent? = GoBackend.VpnService.prepare(WireGuardAutoTunnel.instance)

    private val _appUiState = MutableStateFlow(AppUiState(
        vpnPermissionAccepted = vpnIntent  == null
    ))
    val appUiState = _appUiState.asStateFlow()
    
    
    fun isRequiredPermissionGranted() : Boolean {
        val allAccepted = (_appUiState.value.vpnPermissionAccepted && _appUiState.value.vpnPermissionAccepted)
        if(!allAccepted) requestPermissions()
        return allAccepted
    }
    
    private fun requestPermissions() {
        _appUiState.value = _appUiState.value.copy(
            requestPermissions = true
        )
    }

    fun permissionsRequested() {
        _appUiState.value = _appUiState.value.copy(
            requestPermissions = false
        )
    }

    fun openWebPage(url: String) {
        try {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            showSnackbarMessage(application.getString(R.string.no_browser_detected))
        }
    }
    
    fun onVpnPermissionAccepted() {
        _appUiState.value = _appUiState.value.copy(
            vpnPermissionAccepted = true
        )
    }

    fun launchEmail() {
        try {
            val intent =
                Intent(Intent.ACTION_SENDTO).apply {
                    type = Constants.EMAIL_MIME_TYPE
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(application.getString(R.string.my_email)))
                    putExtra(Intent.EXTRA_SUBJECT, application.getString(R.string.email_subject))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            application.startActivity(
                Intent.createChooser(intent, application.getString(R.string.email_chooser)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            showSnackbarMessage(application.getString(R.string.no_email_detected))
        }
    }
    fun showSnackbarMessage(message : String) {
        _appUiState.value = _appUiState.value.copy(
            snackbarMessage = message,
            snackbarMessageConsumed = false
        )
    }

    fun snackbarMessageConsumed() {
        _appUiState.value = _appUiState.value.copy(
            snackbarMessage = "",
            snackbarMessageConsumed = true
        )
    }
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

    fun setNotificationPermissionAccepted(accepted: Boolean) {
        _appUiState.value = _appUiState.value.copy(
            notificationPermissionAccepted = accepted
        )
    }
}
