package com.zaneschepke.wireguardautotunnel.ui.common.prompt

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun AuthorizationPrompt(onSuccess: () -> Unit, onFailure: () -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val biometricManager = BiometricManager.from(context)
    val bio = biometricManager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
    val isBiometricAvailable = remember {
        when (bio) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError(context.getString(R.string.bio_not_created))
                false
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                onError(context.getString(R.string.bio_update_required))
                false
            }

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                onError(context.getString(R.string.bio_not_supported))
                false
            }

            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    if (isBiometricAvailable) {
        val executor = remember { ContextCompat.getMainExecutor(context) }

        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .setTitle(context.getString(R.string.bio_auth_title))
                .setSubtitle(context.getString(R.string.bio_subtitle))
                .build()

        val biometricPrompt =
            BiometricPrompt(
                context as FragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onFailure()
                    }

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onFailure()
                    }
                },
            )
        biometricPrompt.authenticate(promptInfo)
    }
}
