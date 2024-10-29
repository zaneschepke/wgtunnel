package com.zaneschepke.wireguardautotunnel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.extensions.scaled

// Set of Material typography styles to start with

val inter = FontFamily(
	Font(R.font.inter),
)

val Typography =
	Typography(
		bodyLarge = TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.Normal,
			fontSize = 16.sp.scaled(),
			lineHeight = 24.sp.scaled(),
			letterSpacing = 0.5.sp,
		),
		bodySmall = TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.Normal,
			fontSize = 12.sp.scaled(),
			lineHeight = 20.sp.scaled(),
			letterSpacing = 1.sp,
			color = LightGrey,
		),
		bodyMedium = TextStyle(
			fontFamily = inter,
			fontSize = 14.sp.scaled(),
			lineHeight = 20.sp.scaled(),
			fontWeight = FontWeight(400),
			letterSpacing = 0.25.sp,
		),
		labelLarge = TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.Normal,
			fontSize = 15.sp.scaled(),
			lineHeight = 18.sp.scaled(),
			letterSpacing = 0.sp,
		),
		labelMedium = TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.SemiBold,
			fontSize = 12.sp.scaled(),
			lineHeight = 16.sp.scaled(),
			letterSpacing = 0.5.sp,
		),
		titleMedium = TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.Bold,
			fontSize = 16.sp.scaled(),
			lineHeight = 21.sp.scaled(),
			letterSpacing = 0.sp,
		),
	)
