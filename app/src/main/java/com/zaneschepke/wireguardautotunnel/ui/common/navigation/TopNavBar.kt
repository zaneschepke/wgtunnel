package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(title: String, trailing: @Composable () -> Unit = {}, showBack: Boolean = true) {
	val navController = LocalNavController.current
	CenterAlignedTopAppBar(
		title = {
			Text(title)
		},
		navigationIcon = {
			if (showBack) {
				IconButton(onClick = {
					navController.popBackStack()
				}) {
					val icon = Icons.AutoMirrored.Outlined.ArrowBack
					Icon(
						imageVector = icon,
						contentDescription = icon.name,
					)
				}
			}
		},
		actions = {
			trailing()
		},
	)
}
