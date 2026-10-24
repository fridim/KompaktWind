package com.kompaktwind.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mudita.mmd.ThemeMMD

private val DarkScreenColors: ColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,
    inversePrimary = Color.Black,
    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = Color.Black,
    onSecondaryContainer = Color.White,
    tertiary = Color.White,
    onTertiary = Color.Black,
    tertiaryContainer = Color.Black,
    onTertiaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color.White,
    surfaceTint = Color.White,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    error = Color.White,
    onError = Color.Black,
    errorContainer = Color.Black,
    onErrorContainer = Color.White,
    outline = Color.White,
    outlineVariant = Color.White,
    scrim = Color.Black,
    surfaceBright = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainerLowest = Color.Black,
)

@Composable
fun KompaktWindTheme(
    darkScreen: Boolean,
    content: @Composable () -> Unit
) {
    if (darkScreen) {
        ThemeMMD(colorScheme = DarkScreenColors, content = content)
    } else {
        ThemeMMD(content = content)
    }
}
