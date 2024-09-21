package com.zaneschepke.wireguardautotunnel.ui.screens.config.components

import android.content.pm.PackageInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.DrawablePainter
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.SearchBar
import com.zaneschepke.wireguardautotunnel.ui.screens.config.ConfigUiState
import com.zaneschepke.wireguardautotunnel.ui.screens.config.ConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationSelectionDialog(viewModel: ConfigViewModel, uiState: ConfigUiState, onDismiss: () -> Unit) {
	val context = LocalContext.current
	val licenseComparator = compareBy<PackageInfo> { viewModel.getPackageLabel(it) }

	val sortedPackages = remember(uiState.packages, licenseComparator) {
		uiState.packages.sortedWith(licenseComparator)
	}
	BasicAlertDialog(
		onDismissRequest = { onDismiss() },
	) {
		Surface(
			tonalElevation = 2.dp,
			shadowElevation = 2.dp,
			shape = RoundedCornerShape(12.dp),
			color = MaterialTheme.colorScheme.surface,
			modifier =
			Modifier
				.fillMaxWidth()
				.fillMaxHeight(if (uiState.isAllApplicationsEnabled) 1 / 5f else 4 / 5f),
		) {
			Column(
				modifier =
				Modifier
					.fillMaxWidth(),
			) {
				Row(
					modifier =
					Modifier
						.fillMaxWidth()
						.padding(horizontal = 20.dp, vertical = 7.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					Text(stringResource(id = R.string.tunnel_all))
					Switch(
						checked = uiState.isAllApplicationsEnabled,
						onCheckedChange = viewModel::onAllApplicationsChange,
					)
				}
				if (!uiState.isAllApplicationsEnabled) {
					Row(
						modifier =
						Modifier
							.fillMaxWidth()
							.padding(horizontal = 20.dp, vertical = 7.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.SpaceBetween,
						) {
							Text(stringResource(id = R.string.include))
							Checkbox(
								checked = uiState.include,
								onCheckedChange = {
									viewModel.onIncludeChange(!uiState.include)
								},
							)
						}
						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.SpaceBetween,
						) {
							Text(stringResource(id = R.string.exclude))
							Checkbox(
								checked = !uiState.include,
								onCheckedChange = {
									viewModel.onIncludeChange(!uiState.include)
								},
							)
						}
					}
					Row(
						modifier =
						Modifier
							.fillMaxWidth()
							.padding(horizontal = 20.dp, vertical = 7.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						SearchBar(viewModel::emitQueriedPackages)
					}
					Spacer(Modifier.padding(5.dp))
					LazyColumn(
						horizontalAlignment = Alignment.Start,
						verticalArrangement = Arrangement.Top,
						modifier = Modifier.fillMaxHeight(19 / 22f),
					) {
						items(sortedPackages, key = { it.packageName }) { pack ->
							Row(
								verticalAlignment = Alignment.CenterVertically,
								horizontalArrangement = Arrangement.SpaceBetween,
								modifier =
								Modifier
									.fillMaxSize()
									.padding(5.dp).padding(end = 25.dp),
							) {
								Row(modifier = Modifier.fillMaxWidth().padding(start = 5.dp)) {
									val drawable =
										pack.applicationInfo?.loadIcon(context.packageManager)
									val iconSize = 35.dp
									if (drawable != null) {
										Image(
											painter = DrawablePainter(drawable),
											stringResource(id = R.string.icon),
											modifier = Modifier.size(iconSize),
										)
									} else {
										val icon = Icons.Rounded.Android
										Icon(
											icon,
											icon.name,
											modifier = Modifier.size(iconSize),
										)
									}
									Text(
										viewModel.getPackageLabel(pack),
										modifier = Modifier.padding(5.dp),
									)
								}
								Checkbox(
									modifier = Modifier.fillMaxSize(),
									checked =
									(
										uiState.checkedPackageNames.contains(
											pack.packageName,
										)
										),
									onCheckedChange = {
										if (it) {
											viewModel.onAddCheckedPackage(pack.packageName)
										} else {
											viewModel.onRemoveCheckedPackage(pack.packageName)
										}
									},
								)
							}
						}
					}
				}
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier =
					Modifier
						.fillMaxSize()
						.padding(top = 5.dp),
					horizontalArrangement = Arrangement.Center,
				) {
					TextButton(onClick = { onDismiss() }) {
						Text(stringResource(R.string.done))
					}
				}
			}
		}
	}
}
