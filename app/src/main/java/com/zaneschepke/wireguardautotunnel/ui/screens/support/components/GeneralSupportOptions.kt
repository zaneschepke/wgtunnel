package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Policy
import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl

@Composable
fun GeneralSupportOptions(context: android.content.Context) {
    SurfaceSelectionGroupButton(
        items =
            buildList {
                add(
                    SelectionItem(
                        leadingIcon = Icons.Filled.Book,
                        title = { SelectionItemLabel(R.string.docs_description) },
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
                        title = { SelectionItemLabel(R.string.privacy_policy) },
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
            }
    )
}
