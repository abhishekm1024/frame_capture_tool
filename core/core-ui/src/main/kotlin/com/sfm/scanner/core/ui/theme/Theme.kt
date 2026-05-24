package com.sfm.scanner.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ScanBlue80,
    onPrimary = ScanOnBlue80,
    primaryContainer = ScanBlueContainer80,
    onPrimaryContainer = ScanOnBlueContainer80,
    secondary = ScanNeutral80,
    onSecondary = ScanOnNeutral80,
    secondaryContainer = ScanNeutralContainer80,
    error = ErrorRed80,
    onError = OnErrorRed80,
    errorContainer = ErrorRedContainer80,
    onErrorContainer = OnErrorRedContainer80,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = ScanBlue40,
    onPrimary = ScanOnBlue40,
    primaryContainer = ScanBlueContainer40,
    onPrimaryContainer = ScanOnBlueContainer40,
    secondary = ScanNeutral40,
    error = ErrorRed40,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
)

@Composable
fun ScanAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScanAppTypography,
        shapes = ScanAppShapes,
        content = content,
    )
}
