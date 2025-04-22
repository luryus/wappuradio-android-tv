package com.lkoskela.wappuradio.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@Composable
fun WappuradioTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = Pink80,
        secondary = PurpleGrey80,
        tertiary = Purple80,
    )

    val typography = Typography.copy(headlineLarge = Typography.headlineLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    ))

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}