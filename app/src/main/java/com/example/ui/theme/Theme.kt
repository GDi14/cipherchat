package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = ExpressiveDarkPrimary,
    onPrimary = ExpressiveDarkOnPrimary,
    primaryContainer = ExpressiveDarkPrimaryContainer,
    onPrimaryContainer = ExpressiveDarkOnPrimaryContainer,
    secondary = ExpressiveDarkSecondary,
    onSecondary = ExpressiveDarkOnSecondary,
    secondaryContainer = ExpressiveDarkSecondaryContainer,
    onSecondaryContainer = ExpressiveDarkOnSecondaryContainer,
    tertiary = ExpressiveDarkTertiary,
    onTertiary = ExpressiveDarkOnTertiary,
    tertiaryContainer = ExpressiveDarkTertiaryContainer,
    onTertiaryContainer = ExpressiveDarkOnTertiaryContainer,
    error = ExpressiveDarkError,
    onError = ExpressiveDarkOnError,
    errorContainer = ExpressiveDarkErrorContainer,
    onErrorContainer = ExpressiveDarkOnErrorContainer,
    background = ExpressiveDarkBackground,
    onBackground = Color(0xFFE8E7F0),
    surface = ExpressiveDarkSurface,
    onSurface = Color(0xFFE8E7F0),
    surfaceVariant = ExpressiveDarkSurfaceContainerHigh,
    onSurfaceVariant = Color(0xFFC4C2D0),
    outline = ExpressiveDarkOutline,
    outlineVariant = ExpressiveDarkOutlineVariant,
    surfaceContainerLowest = Color(0xFF0B0B12),
    surfaceContainerLow = Color(0xFF191923),
    surfaceContainer = ExpressiveDarkSurfaceContainer,
    surfaceContainerHigh = ExpressiveDarkSurfaceContainerHigh,
    surfaceContainerHighest = ExpressiveDarkSurfaceContainerHighest
)

private val ExpressiveLightColorScheme = lightColorScheme(
    primary = ExpressiveLightPrimary,
    onPrimary = ExpressiveLightOnPrimary,
    primaryContainer = ExpressiveLightPrimaryContainer,
    onPrimaryContainer = ExpressiveLightOnPrimaryContainer,
    secondary = ExpressiveLightSecondary,
    onSecondary = ExpressiveLightOnSecondary,
    secondaryContainer = ExpressiveLightSecondaryContainer,
    onSecondaryContainer = ExpressiveLightOnSecondaryContainer,
    tertiary = ExpressiveLightTertiary,
    onTertiary = ExpressiveLightOnTertiary,
    tertiaryContainer = ExpressiveLightTertiaryContainer,
    onTertiaryContainer = ExpressiveLightOnTertiaryContainer,
    error = ExpressiveLightError,
    onError = ExpressiveLightOnError,
    errorContainer = ExpressiveLightErrorContainer,
    onErrorContainer = ExpressiveLightOnErrorContainer,
    background = ExpressiveLightBackground,
    onBackground = Color(0xFF1B1A24),
    surface = ExpressiveLightSurface,
    onSurface = Color(0xFF1B1A24),
    surfaceVariant = ExpressiveLightSurfaceContainer,
    onSurfaceVariant = Color(0xFF494755),
    outline = ExpressiveLightOutline,
    outlineVariant = ExpressiveLightOutlineVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FC),
    surfaceContainer = ExpressiveLightSurfaceContainer,
    surfaceContainerHigh = ExpressiveLightSurfaceContainerHigh,
    surfaceContainerHighest = ExpressiveLightSurfaceContainerHighest
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}
