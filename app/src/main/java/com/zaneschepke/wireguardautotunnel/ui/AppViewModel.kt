package com.zaneschepke.wireguardautotunnel.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppViewModel
@Inject
constructor(
    private val application: Application,
) : ViewModel() {

    private val _snackbarState = MutableStateFlow(SnackBarState())
    val snackBarState = _snackbarState.asStateFlow()

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
        _snackbarState.value = _snackbarState.value.copy(
            snackbarMessage = message,
            snackbarMessageConsumed = false
        )
    }

    fun snackbarMessageConsumed() {
        _snackbarState.value = _snackbarState.value.copy(
            snackbarMessage = "",
            snackbarMessageConsumed = true
        )
    }
}
