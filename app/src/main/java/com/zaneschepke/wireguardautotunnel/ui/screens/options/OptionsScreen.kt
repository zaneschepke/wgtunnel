package com.zaneschepke.wireguardautotunnel.ui.screens.options

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.iamageo.multifablibrary.FabIcon
import com.iamageo.multifablibrary.FabOption
import com.iamageo.multifablibrary.MultiFabItem
import com.iamageo.multifablibrary.MultiFloatingActionButton
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.Screen
import com.zaneschepke.wireguardautotunnel.ui.common.ClickableIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationToggle
import com.zaneschepke.wireguardautotunnel.ui.common.text.SectionTitle
import com.zaneschepke.wireguardautotunnel.ui.screens.main.ConfigType
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.getMessage
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionsScreen(
	optionsViewModel: OptionsViewModel = hiltViewModel(),
	navController: NavController,
	appViewModel: AppViewModel,
	focusRequester: FocusRequester,
	tunnelId: String,
) {
	val scrollState = rememberScrollState()
	val uiState by optionsViewModel.uiState.collectAsStateWithLifecycle()
	val context = LocalContext.current

	val interactionSource = remember { MutableInteractionSource() }
	val scope = rememberCoroutineScope()
	val focusManager = LocalFocusManager.current
	val screenPadding = 5.dp
	val fillMaxWidth = .85f

	var currentText by remember { mutableStateOf("") }

	LaunchedEffect(Unit) {
		optionsViewModel.init(tunnelId)
		if (context.isRunningOnTv()) {
			delay(Constants.FOCUS_REQUEST_DELAY)
			focusRequester.requestFocus()
		}
	}

	fun saveTrustedSSID() {
		if (currentText.isNotEmpty()) {
			scope.launch {
				optionsViewModel.onSaveRunSSID(currentText).onSuccess {
					currentText = ""
				}.onFailure {
					appViewModel.showSnackbarMessage(it.getMessage(context))
				}
			}
		}
	}

	Scaffold(
		floatingActionButton = {
			val secondaryColor = MaterialTheme.colorScheme.secondary
			val tvFobColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
			val fobColor =
				if (context.isRunningOnTv()) tvFobColor else secondaryColor
			val fobIconColor =
				if (context.isRunningOnTv()) Color.White else MaterialTheme.colorScheme.background
			AnimatedVisibility(
				visible = true,
				enter = slideInVertically(initialOffsetY = { it * 2 }),
				exit = slideOutVertically(targetOffsetY = { it * 2 }),
				modifier =
				Modifier
					.focusRequester(focusRequester)
					.focusGroup(),
			) {
				MultiFloatingActionButton(
					fabIcon =
					FabIcon(
						iconRes = R.drawable.edit,
						iconResAfterRotate = R.drawable.close,
						iconRotate = 180f,
					),
					fabOption =
					FabOption(
						iconTint = fobIconColor,
						backgroundTint = fobColor,
					),
					itemsMultiFab =
					listOf(
						MultiFabItem(
							label = {
								Text(
									stringResource(id = R.string.amnezia),
									color = Color.White,
									textAlign = TextAlign.Center,
									modifier = Modifier.padding(end = 10.dp),
								)
							},
							modifier =
							Modifier
								.size(40.dp),
							icon = R.drawable.edit,
							value = ConfigType.AMNEZIA.name,
							miniFabOption =
							FabOption(
								backgroundTint = fobColor,
								fobIconColor,
							),
						),
						MultiFabItem(
							label = {
								Text(
									stringResource(id = R.string.wireguard),
									color = Color.White,
									textAlign = TextAlign.Center,
									modifier = Modifier.padding(end = 10.dp),
								)
							},
							icon = R.drawable.edit,
							value = ConfigType.WIREGUARD.name,
							miniFabOption =
							FabOption(
								backgroundTint = fobColor,
								fobIconColor,
							),
						),
					),
					onFabItemClicked = {
						val configType = ConfigType.valueOf(it.value)
						navController.navigate(
							"${Screen.Config.route}/$tunnelId?configType=${configType.name}",
						)
					},
					shape = RoundedCornerShape(16.dp),
				)
			}
		},
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Top,
			modifier =
			Modifier
				.fillMaxSize()
				.verticalScroll(scrollState)
				.clickable(
					indication = null,
					interactionSource = interactionSource,
				) {
					focusManager.clearFocus()
				},
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
					.padding(bottom = 10.dp),
			) {
				Column(
					horizontalAlignment = Alignment.Start,
					verticalArrangement = Arrangement.Top,
					modifier = Modifier.padding(15.dp),
				) {
					SectionTitle(
						title = stringResource(id = R.string.general),
						padding = screenPadding,
					)
					ConfigurationToggle(
						stringResource(R.string.set_primary_tunnel),
						enabled = true,
						checked = uiState.isDefaultTunnel,
						modifier =
						Modifier
							.focusRequester(focusRequester),
						padding = screenPadding,
						onCheckChanged = { optionsViewModel.onTogglePrimaryTunnel() },
					)
				}
			}
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
					.padding(bottom = 10.dp),
			) {
				Column(
					horizontalAlignment = Alignment.Start,
					verticalArrangement = Arrangement.Top,
					modifier = Modifier.padding(15.dp),
				) {
					SectionTitle(
						title = stringResource(id = R.string.auto_tunneling),
						padding = screenPadding,
					)
					ConfigurationToggle(
						stringResource(R.string.mobile_data_tunnel),
						enabled = true,
						checked = uiState.tunnel?.isMobileDataTunnel == true,
						padding = screenPadding,
						onCheckChanged = { optionsViewModel.onToggleIsMobileDataTunnel() },
					)
					Column {
						FlowRow(
							modifier =
							Modifier
								.padding(screenPadding)
								.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(5.dp),
						) {
							uiState.tunnel?.tunnelNetworks?.forEach { ssid ->
								ClickableIconButton(
									onClick = {
										if (context.isRunningOnTv()) {
											focusRequester.requestFocus()
											optionsViewModel.onDeleteRunSSID(ssid)
										}
									},
									onIconClick = {
										if (context.isRunningOnTv()) focusRequester.requestFocus()
										optionsViewModel.onDeleteRunSSID(ssid)
									},
									text = ssid,
									icon = Icons.Filled.Close,
									enabled = true,
								)
							}
							if (uiState.tunnel == null || uiState.tunnel?.tunnelNetworks?.isEmpty() == true) {
								Text(
									stringResource(R.string.no_wifi_names_configured),
									fontStyle = FontStyle.Italic,
									color = Color.Gray,
								)
							}
						}
						OutlinedTextField(
							enabled = true,
							value = currentText,
							onValueChange = { currentText = it },
							label = { Text(stringResource(id = R.string.use_tunnel_on_wifi_name)) },
							modifier =
							Modifier
								.padding(
									start = screenPadding,
									top = 5.dp,
									bottom = 10.dp,
								),
							maxLines = 1,
							keyboardOptions =
							KeyboardOptions(
								capitalization = KeyboardCapitalization.None,
								imeAction = ImeAction.Done,
							),
							keyboardActions = KeyboardActions(onDone = { saveTrustedSSID() }),
							trailingIcon = {
								if (currentText != "") {
									IconButton(onClick = { saveTrustedSSID() }) {
										Icon(
											imageVector = Icons.Outlined.Add,
											contentDescription =
											if (currentText == "") {
												stringResource(
													id =
													R.string
														.trusted_ssid_empty_description,
												)
											} else {
												stringResource(
													id =
													R.string
														.trusted_ssid_value_description,
												)
											},
											tint = MaterialTheme.colorScheme.primary,
										)
									}
								}
							},
						)
					}
				}
			}
		}
	}
}
