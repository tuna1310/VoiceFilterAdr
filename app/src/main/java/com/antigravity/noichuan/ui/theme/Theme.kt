package com.antigravity.noichuan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BlueLight,
    onPrimary = Color.Black,
    secondary = AccentYellow,
    onSecondary = Color.Black,
    tertiary = AccentPink,
    background = Color(0xFF0F172A),
    surface = CardBackgroundDark,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    secondary = AccentYellow,
    onSecondary = Color.Black,
    tertiary = AccentPink,
    background = BlueSurface,
    surface = CardBackgroundLight,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun NoiChuanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
