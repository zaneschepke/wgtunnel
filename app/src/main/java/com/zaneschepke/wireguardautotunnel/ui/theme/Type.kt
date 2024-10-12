package com.zaneschepke.wireguardautotunnel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaneschepke.wireguardautotunnel.R

// Set of Material typography styles to start with

val inter = FontFamily(
	Font(R.font.inter),
)

val Typography =
	Typography(
		bodyLarge =
		TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.Normal,
			fontSize = 16.sp,
			lineHeight = 24.sp,
			letterSpacing = 0.5.sp,
		),
		bodySmall = TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.Normal,
			fontSize = 13.sp,
			lineHeight = 20.sp,
			letterSpacing = 1.sp,
			color = LightGrey,
		),
		labelLarge = TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.Normal,
			fontSize = 15.sp,
			lineHeight = 18.sp,
			letterSpacing = 0.sp,
		),
		labelMedium = TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.SemiBold,
			fontSize = 12.sp,
			lineHeight = 16.sp,
			letterSpacing = 0.5.sp,
		),
		titleMedium = TextStyle(
			fontFamily = inter,
			fontWeight = FontWeight.Bold,
			fontSize = 17.sp,
			lineHeight = 21.sp,
			letterSpacing = 0.sp,
		),
	)

val iconSize = 15.dp
