package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Policy
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionLabelType
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl

@Composable
fun GeneralSupportOptions(context: android.content.Context) {
    val navController = LocalNavController.current
    SurfaceSelectionGroupButton(
        items =
            buildList {
                add(
                    SelectionItem(
                        leadingIcon = Icons.Filled.Book,
                        title = {
                            SelectionItemLabel(
                                stringResource(R.string.docs_description),
                                SelectionLabelType.TITLE,
                            )
                        },
                        trailing = {
                            ForwardButton {
                                context.openWebUrl(context.getString(R.string.docs_url))
                            }
                        },
                        onClick = { context.openWebUrl(context.getString(R.string.docs_url)) },
                    )
                )
                add(
                    SelectionItem(
                        leadingIcon = Icons.Filled.Policy,
                        title = {
                            SelectionItemLabel(
                                stringResource(R.string.privacy_policy),
                                SelectionLabelType.TITLE,
                            )
                        },
                        trailing = {
                            ForwardButton {
                                context.openWebUrl(context.getString(R.string.privacy_policy_url))
                            }
                        },
                        onClick = {
                            context.openWebUrl(context.getString(R.string.privacy_policy_url))
                        },
                    )
                )
                add(
                    SelectionItem(
                        leadingIcon = Icons.Filled.Balance,
                        title = {
                            SelectionItemLabel(
                                stringResource(R.string.licenses),
                                SelectionLabelType.TITLE,
                            )
                        },
                        trailing = { ForwardButton { navController.navigate(Route.License) } },
                        onClick = { navController.navigate(Route.License) },
                    )
                )
            }
    )
}
