package com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.splittunnel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.common.button.SelectionItemButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.CustomTextField
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.getAllInternetCapablePackages
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import java.text.Collator
import java.util.Locale

@Composable
fun SplitTunnelScreen(appUiState: AppUiState, tunnelId: Int, viewModel: SplitTunnelViewModel = hiltViewModel()) {
	val context = LocalContext.current
	val navController = LocalNavController.current

	val inputHeight = 45.dp

	val collator = Collator.getInstance(Locale.getDefault())

	val saved by viewModel.saved.collectAsStateWithLifecycle(false)

	val config by remember { derivedStateOf { appUiState.tunnels.first { it.id == tunnelId } } }

	val derivedSplitOption = remember {
		derivedStateOf {
			config.toWgConfig().let {
				when {
					it.`interface`.excludedApplications.isNotEmpty() -> Pair(SplitOptions.EXCLUDE, it.`interface`.excludedApplications)
					it.`interface`.includedApplications.isNotEmpty() -> Pair(SplitOptions.INCLUDE, it.`interface`.includedApplications)
					else -> Pair(SplitOptions.ALL, emptySet<String>())
				}
			}
		}
	}

	var selectedSplitOption by remember { mutableStateOf(derivedSplitOption.value.first) }

	val derivedSelectedPackages by remember {
		derivedStateOf {
			if (selectedSplitOption == derivedSplitOption.value.first) derivedSplitOption.value.second else emptySet()
		}
	}

	val selectedPackage = remember { mutableStateListOf<String>().apply { addAll(derivedSelectedPackages) } }

	val packages = remember {
		context.getAllInternetCapablePackages().filter { it.applicationInfo != null }.map { pack ->
			SplitTunnelApp(
				context.packageManager.getApplicationIcon(pack.applicationInfo!!),
				context.packageManager.getApplicationLabel(pack.applicationInfo!!).toString(),
				pack.packageName,
			)
		}
	}

	val sortedPackages = remember {
		mutableStateListOf<SplitTunnelApp>().apply {
			addAll(packages.sortedWith(compareBy(collator) { it.name }))
		}
	}

	var query: String by remember { mutableStateOf("") }

	val queriedApps by remember {
		derivedStateOf {
			sortedPackages.filter { app ->
				app.name.contains(query, ignoreCase = true)
			}
		}
	}

	LaunchedEffect(Unit) {
// 		viewModel.cleanUpUninstalledApps(tunnelConfig)
	}

	LaunchedEffect(saved) {
		if (saved) navController.popBackStack()
	}

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.tunneling_apps), trailing = {
				IconButton(onClick = {
					when (selectedSplitOption) {
						SplitOptions.INCLUDE -> viewModel.saveSplitTunnelConfig(selectedPackage, emptySet(), config)
						SplitOptions.ALL -> viewModel.saveSplitTunnelConfig(emptySet(), emptySet(), config)
						SplitOptions.EXCLUDE -> viewModel.saveSplitTunnelConfig(emptySet(), selectedPackage, config)
					}
				}) {
					val icon = Icons.Outlined.Save
					Icon(
						imageVector = icon,
						contentDescription = icon.name,
					)
				}
			})
		},
	) { padding ->

		Column(
			verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.CenterVertically),
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier
				.fillMaxWidth()
				.padding(padding)
				.padding(top = 24.dp.scaledHeight()),
		) {
			MultiChoiceSegmentedButtonRow(
				modifier = Modifier.background(color = MaterialTheme.colorScheme.background).fillMaxWidth()
					.padding(horizontal = 24.dp.scaledWidth()).height(inputHeight),
			) {
				SplitOptions.entries.forEachIndexed { index, entry ->
					val active = selectedSplitOption == entry
					SegmentedButton(
						shape = SegmentedButtonDefaults.itemShape(index = index, count = SplitOptions.entries.size, baseShape = RoundedCornerShape(8.dp)),
						icon = {
							SegmentedButtonDefaults.Icon(active = active, activeContent = {
								val icon = Icons.Outlined.Check
								Icon(imageVector = icon, icon.name, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(SegmentedButtonDefaults.IconSize))
							}) {
								Icon(
									imageVector = entry.icon(),
									contentDescription = entry.icon().name,
									modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
								)
							}
						},
						colors = SegmentedButtonDefaults.colors().copy(
							activeContainerColor = MaterialTheme.colorScheme.surface,
							inactiveContainerColor = MaterialTheme.colorScheme.background,
						),
						onCheckedChange = {
							selectedSplitOption = entry
						},
						checked = active,
					) {
						Text(
							entry.text().asString(context)
								.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
							color = MaterialTheme.colorScheme.onBackground,
						)
					}
				}
			}
			if (selectedSplitOption != SplitOptions.ALL) {
				Column(
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally,
					modifier = Modifier
						.fillMaxWidth(),
				) {
					CustomTextField(
						textStyle = MaterialTheme.typography.bodySmall.copy(
							color = MaterialTheme.colorScheme.onBackground,
						),
						value = query,
						onValueChange = { input ->
							query = input
						},
						interactionSource = remember { MutableInteractionSource() },
						label = {},
						leading = {
							val icon = Icons.Outlined.Search
							Icon(icon, icon.name)
						},
						containerColor = MaterialTheme.colorScheme.background,
						modifier =
						Modifier
							.fillMaxWidth().height(inputHeight).padding(horizontal = 24.dp.scaledWidth()),
						singleLine = true,
						keyboardOptions =
						KeyboardOptions(
							capitalization = KeyboardCapitalization.None,
							imeAction = ImeAction.Done,
						),
						keyboardActions = KeyboardActions(),
					)
					LazyColumn(
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.Top,
					) {
						items(queriedApps, key = { it.`package` }) { app ->
							val checked = selectedPackage.contains(app.`package`)
							val onClick = {
								if (checked) selectedPackage.remove(app.`package`) else selectedPackage.add(app.`package`)
							}
							SelectionItemButton(
								{
									Image(
										rememberDrawablePainter(app.icon),
										app.name,
										modifier =
										Modifier
											.padding(horizontal = 24.dp.scaledWidth())
											.size(
												iconSize,
											),
									)
								},
								buttonText = app.name,
								onClick = {
									onClick()
								},
								trailing = {
									Row(
										modifier = Modifier
											.fillMaxWidth(),
										horizontalArrangement = Arrangement.End,
										verticalAlignment = Alignment.CenterVertically,
									) {
										Checkbox(
											checked = checked,
											onCheckedChange = {
												onClick()
											},
										)
									}
								},
							)
						}
					}
				}
			}
		}
	}
}
