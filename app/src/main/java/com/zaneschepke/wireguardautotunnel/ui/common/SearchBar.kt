package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun SearchBar(onQuery: (queryString: String) -> Unit) {
	// Immediately update and keep track of query from text field changes.
	var query: String by rememberSaveable { mutableStateOf("") }
	var showClearIcon by rememberSaveable { mutableStateOf(false) }

	if (query.isEmpty()) {
		showClearIcon = false
	} else if (query.isNotEmpty()) {
		showClearIcon = true
	}

	TextField(
		value = query,
		onValueChange = { onQueryChanged ->
			// If user makes changes to text, immediately updated it.
			query = onQueryChanged
			onQuery(onQueryChanged)
		},
		leadingIcon = {
			val icon = Icons.Rounded.Search
			Icon(
				imageVector = icon,
				tint = MaterialTheme.colorScheme.onBackground,
				contentDescription = icon.name,
			)
		},
		trailingIcon = {
			if (showClearIcon) {
				IconButton(onClick = { query = "" }) {
					val icon = Icons.Rounded.Clear
					Icon(
						imageVector = icon,
						tint = MaterialTheme.colorScheme.onBackground,
						contentDescription = icon.name,
					)
				}
			}
		},
		maxLines = 1,
		colors =
		TextFieldDefaults.colors(
			focusedContainerColor = Color.Transparent,
			unfocusedContainerColor = Color.Transparent,
			disabledContainerColor = Color.Transparent,
		),
		placeholder = { Text(text = stringResource(R.string.hint_search_packages)) },
		textStyle = MaterialTheme.typography.bodySmall,
		singleLine = true,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
		modifier =
		Modifier
			.fillMaxWidth()
			.background(color = MaterialTheme.colorScheme.background, shape = RectangleShape),
	)
}
