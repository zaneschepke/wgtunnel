package com.zaneschepke.wireguardautotunnel.ui.screens.support

import android.content.Intent
import android.content.Intent.createChooser
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.ui.common.screen.LoadingScreen
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.Event

@Composable
fun SupportScreen(
    viewModel: SupportViewModel = hiltViewModel(),
    showSnackbarMessage: (String) -> Unit,
    focusRequester: FocusRequester
) {
    val context = LocalContext.current
    val fillMaxWidth = .85f

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    fun openWebPage(url: String) {
        try {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            context.startActivity(intent)
        } catch (e: Exception) {
            showSnackbarMessage(Event.Error.Exception(e).message)
        }
    }

    fun launchEmail() {
        try {
            val intent =
                Intent(Intent.ACTION_SENDTO).apply {
                    type = Constants.EMAIL_MIME_TYPE
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.my_email)))
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject))
                }
            startActivity(
                context,
                createChooser(intent, context.getString(R.string.email_chooser)),
                null,
            )
        } catch (e: Exception) {
            showSnackbarMessage(Event.Error.Exception(e).message)
        }
    }

    if (uiState.loading) {
        LoadingScreen()
        return
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .focusable()
    ) {
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier =
                (if (WireGuardAutoTunnel.isRunningOnAndroidTv()) {
                        Modifier.height(IntrinsicSize.Min)
                            .fillMaxWidth(fillMaxWidth)
                            .padding(top = 10.dp)
                    } else {
                        Modifier.fillMaxWidth(fillMaxWidth).padding(top = 20.dp)
                    })
                    .padding(bottom = 25.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.thank_you),
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp),
                    fontSize = 16.sp,
                )
                Text(
                    stringResource(id = R.string.support_help_text),
                    textAlign = TextAlign.Start,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
                TextButton(
                    onClick = { openWebPage(context.resources.getString(R.string.docs_url)) },
                    modifier = Modifier.padding(vertical = 5.dp).focusRequester(focusRequester),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row {
                            Icon(Icons.Rounded.Book, stringResource(id = R.string.docs))
                            Text(
                                stringResource(id = R.string.docs_description),
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            stringResource(id = R.string.go)
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = { openWebPage(context.resources.getString(R.string.discord_url)) },
                    modifier = Modifier.padding(vertical = 5.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.discord),
                                stringResource(id = R.string.discord),
                                Modifier.size(25.dp),
                            )
                            Text(
                                stringResource(id = R.string.discord_description),
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            stringResource(id = R.string.go)
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = { openWebPage(context.resources.getString(R.string.github_url)) },
                    modifier = Modifier.padding(vertical = 5.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.github),
                                stringResource(id = R.string.github),
                                Modifier.size(25.dp),
                            )
                            Text(
                                "Open an issue",
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            stringResource(id = R.string.go)
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = { launchEmail() },
                    modifier = Modifier.padding(vertical = 5.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row {
                            Icon(Icons.Rounded.Mail, stringResource(id = R.string.email))
                            Text(
                                stringResource(id = R.string.email_description),
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            stringResource(id = R.string.go)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            stringResource(id = R.string.privacy_policy),
            style = TextStyle(textDecoration = TextDecoration.Underline),
            fontSize = 16.sp,
            modifier =
                Modifier.clickable {
                    openWebPage(context.resources.getString(R.string.privacy_policy_url))
                },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(25.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(25.dp),
        ) {
            Text("Version: ${BuildConfig.VERSION_NAME}", modifier = Modifier.focusable())
            Text("Mode: ${if (uiState.settings.isKernelEnabled) "Kernel" else "Userspace"}")
        }
    }
}
