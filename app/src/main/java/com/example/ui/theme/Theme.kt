package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    onBackground = ExpressiveDarkSurfaceContainerHighest,
    surface = ExpressiveDarkSurface,
    onSurface = ExpressiveLightBackground,
    surfaceVariant = ExpressiveDarkSurfaceContainerHigh,
    onSurfaceVariant = ExpressiveLightOutlineVariant,
    outline = ExpressiveDarkOutline,
    outlineVariant = ExpressiveDarkOutlineVariant
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
    onBackground = ExpressiveDarkBackground,
    surface = ExpressiveLightSurface,
    onSurface = ExpressiveDarkSurface,
    surfaceVariant = ExpressiveLightSurfaceContainer,
    onSurfaceVariant = ExpressiveLightOutline,
    outline = ExpressiveLightOutline,
    outlineVariant = ExpressiveLightOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // false to preserve ShieldChat's signature high-tech expressive aesthetic
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
