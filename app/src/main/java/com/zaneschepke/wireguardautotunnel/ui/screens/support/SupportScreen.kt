package com.zaneschepke.wireguardautotunnel.ui.screens.support

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
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.launchSupportEmail
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl

@Composable
fun SupportScreen(focusRequester: FocusRequester, appUiState: AppUiState) {
	val context = LocalContext.current
	val navController = LocalNavController.current
	val fillMaxWidth = .85f

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top,
		modifier =
		Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.focusable(),
	) {
		Surface(
			tonalElevation = 2.dp,
			shadowElevation = 2.dp,
			shape = RoundedCornerShape(12.dp),
			color = MaterialTheme.colorScheme.surface,
			modifier =
			(
				if (context.isRunningOnTv()) {
					Modifier
						.height(IntrinsicSize.Min)
						.fillMaxWidth(fillMaxWidth)
						.padding(top = 10.dp)
				} else {
					Modifier
						.fillMaxWidth(fillMaxWidth)
						.padding(top = 20.dp)
				}
				)
				.padding(bottom = 25.dp),
		) {
			Column(modifier = Modifier.padding(20.dp)) {
				val forwardIcon = Icons.AutoMirrored.Rounded.ArrowForward
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
					onClick = {
						context.openWebUrl(
							context.resources.getString(R.string.docs_url),
						)
					},
					modifier =
					Modifier
						.padding(vertical = 5.dp)
						.focusRequester(focusRequester),
				) {
					Row(
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier.fillMaxWidth(),
					) {
						Row {
							val icon = Icons.Rounded.Book
							Icon(icon, icon.name)
							Text(
								stringResource(id = R.string.docs_description),
								textAlign = TextAlign.Justify,
								modifier =
								Modifier
									.padding(start = 10.dp)
									.weight(
										weight = 1.0f,
										fill = false,
									),
								softWrap = true,
							)
						}
						Icon(
							forwardIcon,
							forwardIcon.name,
						)
					}
				}
				HorizontalDivider(
					thickness = 0.5.dp,
					color = MaterialTheme.colorScheme.onBackground,
				)
				TextButton(
					onClick = {
						context.openWebUrl(
							context.resources.getString(R.string.telegram_url),
						)
					},
					modifier = Modifier.padding(vertical = 5.dp),
				) {
					Row(
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier.fillMaxWidth(),
					) {
						Row {
							val icon = ImageVector.vectorResource(R.drawable.telegram)
							Icon(
								icon,
								icon.name,
								Modifier.size(25.dp),
							)
							Text(
								stringResource(id = R.string.chat_description),
								textAlign = TextAlign.Justify,
								modifier = Modifier.padding(start = 10.dp),
							)
						}
						Icon(
							forwardIcon,
							forwardIcon.name,
						)
					}
				}
				HorizontalDivider(
					thickness = 0.5.dp,
					color = MaterialTheme.colorScheme.onBackground,
				)
				TextButton(
					onClick = {
						context.openWebUrl(
							context.resources.getString(R.string.github_url),
						)
					},
					modifier = Modifier.padding(vertical = 5.dp),
				) {
					Row(
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier.fillMaxWidth(),
					) {
						Row {
							val icon = ImageVector.vectorResource(R.drawable.github)
							Icon(
								imageVector = icon,
								icon.name,
								Modifier.size(25.dp),
							)
							Text(
								stringResource(id = R.string.open_issue),
								textAlign = TextAlign.Justify,
								modifier = Modifier.padding(start = 10.dp),
							)
						}
						Icon(
							forwardIcon,
							forwardIcon.name,
						)
					}
				}
				HorizontalDivider(
					thickness = 0.5.dp,
					color = MaterialTheme.colorScheme.onBackground,
				)
				TextButton(
					onClick = { context.launchSupportEmail() },
					modifier = Modifier.padding(vertical = 5.dp),
				) {
					Row(
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier.fillMaxWidth(),
					) {
						Row {
							val icon = Icons.Rounded.Mail
							Icon(icon, icon.name)
							Text(
								stringResource(id = R.string.email_description),
								textAlign = TextAlign.Justify,
								modifier = Modifier.padding(start = 10.dp),
							)
						}
						Icon(
							forwardIcon,
							forwardIcon.name,
						)
					}
				}
				if (!context.isRunningOnTv()) {
					HorizontalDivider(
						thickness = 0.5.dp,
						color = MaterialTheme.colorScheme.onBackground,
					)
					TextButton(
						onClick = { navController.navigate(Route.Logs) },
						modifier = Modifier.padding(vertical = 5.dp),
					) {
						Row(
							horizontalArrangement = Arrangement.SpaceBetween,
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier.fillMaxWidth(),
						) {
							Row {
								val icon = Icons.Rounded.FormatListNumbered
								Icon(icon, icon.name)
								Text(
									stringResource(id = R.string.read_logs),
									textAlign = TextAlign.Justify,
									modifier = Modifier.padding(start = 10.dp),
								)
							}
							Icon(
								Icons.AutoMirrored.Rounded.ArrowForward,
								stringResource(id = R.string.go),
							)
						}
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
				context.openWebUrl(
					context.resources.getString(R.string.privacy_policy_url),
				)
			},
		)
		Row(
			horizontalArrangement = Arrangement.spacedBy(25.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.padding(25.dp),
		) {
			val version =
				buildAnnotatedString {
					append(stringResource(id = R.string.version))
					append(": ")
					append(BuildConfig.VERSION_NAME)
				}
			val mode =
				buildAnnotatedString {
					append(stringResource(R.string.mode))
					append(": ")
					when (appUiState.settings.isKernelEnabled) {
						true -> append(stringResource(id = R.string.kernel))
						false -> append(stringResource(id = R.string.userspace))
					}
				}
			Text(version.text, modifier = Modifier.focusable())
			Text(mode.text)
		}
	}
}
