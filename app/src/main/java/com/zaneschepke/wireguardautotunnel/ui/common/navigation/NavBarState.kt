package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.ui.theme.Brick
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

data class NavBarState(
    val showTop: Boolean = true,
    val showBottom: Boolean = true,
    val topTitle: @Composable (() -> Unit)? = null,
    val topTrailing: @Composable (() -> Unit)? = null,
    val route: Route? = null,
)

@Composable
fun currentNavBackStackEntryAsNavBarState(
    navController: NavController,
    backStackEntry: NavBackStackEntry?,
    viewModel: AppViewModel,
    uiState: AppUiState,
    appViewState: AppViewState,
): State<NavBarState> {
    fun isActiveSelected(): Boolean =
        uiState.activeTunnels.any { active ->
            appViewState.selectedTunnels.any { active.key.id == it.id }
        }
    return produceState(
        initialValue = NavBarState(),
        key1 = backStackEntry,
        key2 = uiState,
        key3 = appViewState,
    ) {
        value =
            when {
                backStackEntry.isCurrentRoute(Route.Main::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { Text(stringResource(R.string.tunnels)) },
                        {
                            when (appViewState.selectedTunnels.size) {
                                0 -> {
                                    IconButton(
                                        onClick = {
                                            viewModel.handleEvent(AppEvent.ToggleBottomSheet)
                                        }
                                    ) {
                                        val icon = Icons.Rounded.Add
                                        Icon(
                                            icon,
                                            stringResource(R.string.add_tunnel),
                                            modifier = Modifier.size(iconSize),
                                        )
                                    }
                                }
                                1 -> {
                                    Row {
                                        IconButton(onClick = {}) {
                                            Icon(
                                                Icons.Rounded.Share,
                                                stringResource(R.string.share),
                                                modifier = Modifier.size(iconSize),
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.handleEvent(AppEvent.CopySelectedTunnel)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Rounded.CopyAll,
                                                stringResource(R.string.copy),
                                                modifier = Modifier.size(iconSize),
                                            )
                                        }
                                        if (!isActiveSelected()) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.handleEvent(
                                                        AppEvent.SetShowModal(
                                                            AppViewState.ModalType.DELETE
                                                        )
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    stringResource(R.string.delete_tunnel),
                                                    modifier = Modifier.size(iconSize),
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Row {
                                        IconButton(onClick = {}) {
                                            Icon(
                                                Icons.Rounded.Share,
                                                stringResource(R.string.share),
                                                modifier = Modifier.size(iconSize),
                                            )
                                        }
                                        if (!isActiveSelected()) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.handleEvent(
                                                        AppEvent.SetShowModal(
                                                            AppViewState.ModalType.DELETE
                                                        )
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    stringResource(R.string.delete_tunnel),
                                                    modifier = Modifier.size(iconSize),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        route = Route.Main,
                    )
                }
                backStackEntry.isCurrentRoute(Route.AutoTunnel::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { Text(stringResource(R.string.auto_tunnel)) },
                        {
                            IconButton(
                                onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnel) }
                            ) {
                                val (icon, description, color) =
                                    if (uiState.appSettings.isAutoTunnelEnabled) {
                                        Triple(Icons.Rounded.Stop, R.string.stop_auto, Brick)
                                    } else {
                                        Triple(
                                            Icons.Rounded.PlayArrow,
                                            R.string.start_auto,
                                            SilverTree,
                                        )
                                    }
                                Icon(
                                    icon,
                                    stringResource(description),
                                    tint = color,
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                        },
                        route = Route.AutoTunnel,
                    )
                }
                backStackEntry.isCurrentRoute(Route.AutoTunnelAdvanced::class) ||
                    backStackEntry.isCurrentRoute(Route.SettingsAdvanced::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { Text(stringResource(R.string.advanced_settings)) },
                        route = Route.AutoTunnelAdvanced,
                    )
                }
                backStackEntry.isCurrentRoute(Route.Settings::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { Text(stringResource(R.string.settings)) },
                        {
                            IconButton(
                                onClick = { viewModel.handleEvent(AppEvent.ToggleBottomSheet) }
                            ) {
                                val icon = Icons.Rounded.Menu
                                Icon(
                                    icon,
                                    stringResource(R.string.quick_actions),
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                        },
                        route = Route.Settings,
                    )
                }
                backStackEntry.isCurrentRoute(Route.KillSwitch::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { Text(stringResource(R.string.kill_switch)) },
                        route = Route.KillSwitch,
                    )
                }
                backStackEntry.isCurrentRoute(Route.Appearance::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { Text(stringResource(R.string.appearance)) },
                        route = Route.Appearance,
                    )
                }
                backStackEntry.isCurrentRoute(Route.Language::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { Text(stringResource(R.string.language)) },
                        route = Route.Language,
                    )
                }
                backStackEntry.isCurrentRoute(Route.Display::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { Text(stringResource(R.string.display_theme)) },
                        route = Route.Display,
                    )
                }
                backStackEntry.isCurrentRoute(Route.Logs::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = false,
                        { Text(stringResource(R.string.logs)) },
                        {
                            IconButton(
                                onClick = { viewModel.handleEvent(AppEvent.ToggleBottomSheet) }
                            ) {
                                val icon = Icons.Rounded.Menu
                                Icon(
                                    icon,
                                    stringResource(R.string.quick_actions),
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                        },
                        route = Route.Logs,
                    )
                }
                backStackEntry.isCurrentRoute(Route.TunnelOptions::class) -> {
                    val args = backStackEntry?.toRoute<Route.TunnelOptions>()
                    val tunnel = uiState.tunnels.find { it.id == args?.id }
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { tunnel?.name?.let { Text(it) } },
                        {
                            IconButton(
                                onClick = {
                                    tunnel?.id?.let {
                                        navController.navigate(Route.Config(id = it))
                                    }
                                }
                            ) {
                                val icon = Icons.Rounded.Edit
                                Icon(
                                    icon,
                                    stringResource(R.string.edit_tunnel),
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                        },
                        route = args?.let { Route.TunnelOptions(it.id) },
                    )
                }
                backStackEntry.isCurrentRoute(Route.SplitTunnel::class) -> {
                    val args = backStackEntry?.toRoute<Route.SplitTunnel>()
                    val name = uiState.tunnels.find { it.id == args?.id }?.name
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { name?.let { Text(it) } },
                        {
                            IconButton(
                                onClick = { viewModel.handleEvent(AppEvent.InvokeScreenAction) }
                            ) {
                                val icon = Icons.Rounded.Save
                                Icon(
                                    icon,
                                    stringResource(R.string.save),
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                        },
                        route = args?.let { Route.SplitTunnel(it.id) },
                    )
                }
                backStackEntry.isCurrentRoute(Route.Config::class) -> {
                    val args = backStackEntry?.toRoute<Route.Config>()
                    val name = uiState.tunnels.find { it.id == args?.id }?.name
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { name?.let { Text(it) } },
                        {
                            IconButton(
                                onClick = { viewModel.handleEvent(AppEvent.InvokeScreenAction) }
                            ) {
                                val icon = Icons.Rounded.Save
                                Icon(
                                    icon,
                                    stringResource(R.string.save),
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                        },
                        route = args?.let { Route.Config(it.id) },
                    )
                }
                backStackEntry.isCurrentRoute(Route.TunnelAutoTunnel::class) -> {
                    val args = backStackEntry?.toRoute<Route.TunnelAutoTunnel>()
                    val name = uiState.tunnels.find { it.id == args?.id }?.name
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { name?.let { Text(it) } },
                        route = args?.let { Route.TunnelAutoTunnel(it.id) },
                    )
                }
                backStackEntry.isCurrentRoute(Route.Support::class) -> {
                    NavBarState(
                        showTop = true,
                        showBottom = true,
                        { Text(stringResource(R.string.support)) },
                        route = Route.Support,
                    )
                }
                else -> NavBarState(showTop = false, showBottom = false)
            }
    }
}
