package com.zaneschepke.wireguardautotunnel.ui.screens.config

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.DrawablePainter
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel = hiltViewModel(),
    padding: PaddingValues,
    navController: NavController,
    id : String?
) {

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val tunnel by viewModel.tunnel.collectAsStateWithLifecycle(null)
    val tunnelName = viewModel.tunnelName.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val checkedPackages by viewModel.checkedPackages.collectAsStateWithLifecycle()
    val include by viewModel.include.collectAsStateWithLifecycle()
    val allApplications by viewModel.allApplications.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getTunnelById(id)
        viewModel.emitAllInternetCapablePackages()
        viewModel.emitCurrentPackageConfigurations(id)
    }

    if(tunnel != null) {
        LazyColumn(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = tunnelName.value,
                        onValueChange = {
                            viewModel.onTunnelNameChange(it)
                        },
                        label = { Text(stringResource(id = R.string.tunnel_name)) },
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.onTunnelNameChange(tunnelName.value)
                            }
                        ),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.tunnel_all))
                    Switch(
                        checked = allApplications,
                        onCheckedChange = {
                            viewModel.onAllApplicationsChange(!allApplications)
                        }
                    )
                }
            }
                if (!allApplications) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(id = R.string.include))
                                Checkbox(
                                    checked = include,
                                    onCheckedChange = {
                                        viewModel.onIncludeChange(!include)
                                    }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(id = R.string.exclude))
                                Checkbox(
                                    checked = !include,
                                    onCheckedChange = {
                                        viewModel.onIncludeChange(!include)
                                    }
                                )
                            }
                        }
                    }
//                    LazyColumn(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .fillMaxHeight(.75f)
//                            .padding(horizontal = 14.dp, vertical = 7.dp),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.Start
//                    ) {
                    items(packages) { pack ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(5.dp)
                            ) {
                                val drawable =
                                    pack.applicationInfo?.loadIcon(context.packageManager)
                                if (drawable != null) {
                                    Image(
                                        painter = DrawablePainter(drawable),
                                        stringResource(id = R.string.icon),
                                        modifier = Modifier.size(50.dp, 50.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Android,
                                        stringResource(id = R.string.edit),
                                        modifier = Modifier.size(50.dp, 50.dp)
                                    )
                                }
                                Text(
                                    pack.applicationInfo.loadLabel(context.packageManager)
                                        .toString(), modifier = Modifier.padding(5.dp)
                                )
                            }
                            Checkbox(
                                checked = (checkedPackages.contains(pack.packageName)),
                                onCheckedChange = {
                                    if (it) viewModel.onAddCheckedPackage(pack.packageName) else viewModel.onRemoveCheckedPackage(
                                        pack.packageName
                                    )
                                }
                            )
                        }
                    }
                }
            item {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = {
                        scope.launch {
                            viewModel.onSaveAllChanges()
                            Toast.makeText(
                                context,
                                context.resources.getString(R.string.config_changes_saved),
                                Toast.LENGTH_LONG
                            ).show()
                            navController.navigate(Routes.Main.name)
                        }
                    }, Modifier.padding(25.dp)) {
                        Text(stringResource(id = R.string.save_changes))
                    }
                }
            }
        }
    }
}