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

@Composable
fun AuthorizationPrompt(
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val biometricManager = BiometricManager.from(context)
    val bio = biometricManager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
    val isBiometricAvailable =
        remember {
            when (bio) {
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    onError("Biometrics not available")
                    false
                }

                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    onError("Biometrics not created")
                    false
                }

                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    onError("Biometric hardware not found")
                    false
                }

                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    onError("Biometric security update required")
                    false
                }

                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    onError("Biometrics not supported")
                    false
                }

                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    onError("Biometrics status unknown")
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
                .setTitle("Biometric Authentication")
                .setSubtitle("Log in using your biometric credential")
                .build()

        val biometricPrompt =
            BiometricPrompt(
                context as FragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
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
                }
            )
        biometricPrompt.authenticate(promptInfo)
    }
}
