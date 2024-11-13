package com.zaneschepke.wireguardautotunnel.ui.screens.support

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LineStyle
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.label.VersionLabel
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.theme.topPadding
import com.zaneschepke.wireguardautotunnel.util.extensions.launchSupportEmail
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun SupportScreen() {
	val context = LocalContext.current
	val navController = LocalNavController.current

	Column(
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
		modifier =
		Modifier
			.fillMaxSize()
			.padding(top = topPadding)
			.padding(horizontal = 24.dp.scaledWidth()),
	) {
		GroupLabel(stringResource(R.string.thank_you))
		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Filled.Book,
					title = {
						Text(
							stringResource(R.string.docs_description),
							style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
						)
					},
					trailing = {
						ForwardButton { context.openWebUrl(context.getString(R.string.docs_url)) }
					},
					onClick = {
						context.openWebUrl(context.getString(R.string.docs_url))
					},
				),
				SelectionItem(
					Icons.Filled.LineStyle,
					title = {
						Text(
							stringResource(R.string.read_logs),
							style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
						)
					},
					trailing = {
						ForwardButton {
							navController.navigate(Route.Logs)
						}
					},
					onClick = {
						navController.navigate(Route.Logs)
					},
				),
				SelectionItem(
					Icons.Filled.Policy,
					title = {
						Text(
							stringResource(R.string.privacy_policy),
							style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
						)
					},
					trailing = {
						ForwardButton { context.openWebUrl(context.getString(R.string.privacy_policy_url)) }
					},
					onClick = {
						context.openWebUrl(context.getString(R.string.privacy_policy_url))
					},
				),

			),
		)
		SurfaceSelectionGroupButton(
			buildList {
				addAll(
					listOf(
						SelectionItem(
							ImageVector.vectorResource(R.drawable.telegram),
							title = {
								Text(
									stringResource(R.string.chat_description),
									style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
								)
							},
							trailing = {
								ForwardButton {
									context.openWebUrl(context.getString(R.string.telegram_url))
								}
							},
							onClick = {
								context.openWebUrl(context.getString(R.string.telegram_url))
							},
						),
						SelectionItem(
							ImageVector.vectorResource(R.drawable.github),
							title = {
								Text(
									stringResource(R.string.open_issue),
									style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface))
							},
							trailing = {
								ForwardButton {
									context.openWebUrl(context.getString(R.string.github_url))
								}
							},
							onClick = {
								context.openWebUrl(context.getString(R.string.github_url))
							},
						),
						SelectionItem(
							Icons.Filled.Mail,
							title = {
								Text(
									stringResource(R.string.email_description),
									style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
								)
							},
							trailing = {
								ForwardButton {
									context.launchSupportEmail()
								}
							},
							onClick = {
								context.launchSupportEmail()
							},
						),
					)
				)
				if (BuildConfig.FLAVOR == "fdroid") {
					add(
						SelectionItem(
							Icons.Filled.AttachMoney,
							title = {
								Text(
									stringResource(R.string.donate),
									style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
								)
							},
							trailing = {
								ForwardButton {
									context.openWebUrl(context.getString(R.string.donate_url))
								}
							},
							onClick = {
								context.openWebUrl(context.getString(R.string.donate_url))
							},
						),
					)
				}
			}
		)
		VersionLabel()
	}
}
