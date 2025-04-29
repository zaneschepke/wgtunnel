package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.launchSupportEmail
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl

@Composable
fun ContactSupportOptions(context: android.content.Context) {
    SurfaceSelectionGroupButton(
        items =
            buildList {
                addAll(
                    listOf(
                        SelectionItem(
                            leadingIcon = ImageVector.vectorResource(R.drawable.matrix),
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.join_matrix),
                                    SelectionLabelType.TITLE,
                                )
                            },
                            trailing = {
                                ForwardButton {
                                    context.openWebUrl(context.getString(R.string.matrix_url))
                                }
                            },
                            onClick = { context.openWebUrl(context.getString(R.string.matrix_url)) },
                        ),
                        SelectionItem(
                            leadingIcon = ImageVector.vectorResource(R.drawable.telegram),
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.join_telegram),
                                    SelectionLabelType.TITLE,
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
                            leadingIcon = ImageVector.vectorResource(R.drawable.github),
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.open_issue),
                                    SelectionLabelType.TITLE,
                                )
                            },
                            trailing = {
                                ForwardButton {
                                    context.openWebUrl(context.getString(R.string.github_url))
                                }
                            },
                            onClick = { context.openWebUrl(context.getString(R.string.github_url)) },
                        ),
                        SelectionItem(
                            leadingIcon = Icons.Filled.Mail,
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.email_description),
                                    SelectionLabelType.TITLE,
                                )
                            },
                            trailing = { ForwardButton { context.launchSupportEmail() } },
                            onClick = { context.launchSupportEmail() },
                        ),
                    )
                )
                if (BuildConfig.FLAVOR != Constants.GOOGLE_PLAY_FLAVOR) {
                    add(
                        SelectionItem(
                            leadingIcon = Icons.Filled.Favorite,
                            title = {
                                SelectionItemLabel(
                                    stringResource(R.string.donate),
                                    SelectionLabelType.TITLE,
                                )
                            },
                            trailing = {
                                ForwardButton {
                                    context.openWebUrl(context.getString(R.string.donate_url))
                                }
                            },
                            onClick = { context.openWebUrl(context.getString(R.string.donate_url)) },
                        )
                    )
                }
            }
    )
}
