package com.zaneschepke.wireguardautotunnel.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
	darkColorScheme(
		// primary = Purple80,
		primary = virdigris,
		secondary = virdigris,
		// secondary = PurpleGrey80,
		tertiary = virdigris,
		// tertiary = Pink80
	)

private val LightColorScheme =
	lightColorScheme(
		primary = Purple40,
		secondary = PurpleGrey40,
		tertiary = Pink40,
        /* Other default colors to override
        background = Color(0xFFFFFBFE),
        surface = Color(0xFFFFFBFE),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
         */
	)

@Composable
fun WireguardAutoTunnelTheme(
	// force dark theme
	useDarkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit,
) {
	val context = LocalContext.current
	val colorScheme = when {
		(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) -> {
			if (useDarkTheme) {
				dynamicDarkColorScheme(context)
			} else {
				dynamicLightColorScheme(context)
			}
		}
		useDarkTheme -> DarkColorScheme
		else -> LightColorScheme
	}
	val view = LocalView.current
	if (!view.isInEditMode) {
		SideEffect {
			val window = (view.context as Activity).window
			WindowCompat.setDecorFitsSystemWindows(window, false)
			window.statusBarColor = Color.Transparent.toArgb()
			window.navigationBarColor = Color.Transparent.toArgb()
			WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
				!useDarkTheme
		}
	}

	MaterialTheme(
		colorScheme = colorScheme,
		typography = Typography,
		content = content,
	)
}
