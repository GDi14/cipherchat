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
    onBackground = Color(0xFFF6EFF2),
    surface = ExpressiveDarkSurface,
    onSurface = Color(0xFFF6EFF2),
    surfaceVariant = ExpressiveDarkSurfaceContainerHigh,
    onSurfaceVariant = Color(0xFFE4C9EA),
    outline = ExpressiveDarkOutline,
    outlineVariant = ExpressiveDarkOutlineVariant,
    surfaceContainerLowest = BrandBlack,
    surfaceContainerLow = PageDarkSurfaceLow,
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
    onBackground = BrandBlack,
    surface = ExpressiveLightSurface,
    onSurface = BrandBlack,
    surfaceVariant = ExpressiveLightSurfaceContainer,
    onSurfaceVariant = Color(0xFF55435C),
    outline = ExpressiveLightOutline,
    outlineVariant = ExpressiveLightOutlineVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = PageLightSurfaceLow,
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

/** Lavender/plum Material 3 theme used by app pages outside the chat surface. */
@Composable
fun ExpressivePagesTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) {
        darkColorScheme(
            primary = PageDarkPrimary,
            onPrimary = PageDarkOnPrimary,
            primaryContainer = PageDarkPrimaryContainer,
            onPrimaryContainer = PageDarkOnPrimaryContainer,
            secondary = PageDarkSecondary,
            onSecondary = PageDarkOnSecondary,
            secondaryContainer = PageDarkSecondaryContainer,
            onSecondaryContainer = PageDarkOnSecondaryContainer,
            tertiary = PageDarkTertiary,
            onTertiary = PageDarkOnTertiary,
            tertiaryContainer = PageDarkTertiaryContainer,
            onTertiaryContainer = PageDarkOnTertiaryContainer,
            error = ExpressiveDarkError,
            onError = ExpressiveDarkOnError,
            errorContainer = ExpressiveDarkErrorContainer,
            onErrorContainer = ExpressiveDarkOnErrorContainer,
            background = PageDarkBackground,
            onBackground = Color(0xFFFFF5FF),
            surface = PageDarkSurface,
            onSurface = Color(0xFFFFF5FF),
            surfaceVariant = PageDarkSurfaceHigh,
            onSurfaceVariant = Color(0xFFE4C9EA),
            outline = PageDarkOutline,
            outlineVariant = PageDarkOutlineVariant,
            surfaceContainerLowest = BrandBlack,
            surfaceContainerLow = PageDarkSurfaceLow,
            surfaceContainer = PageDarkSurface,
            surfaceContainerHigh = PageDarkSurfaceHigh,
            surfaceContainerHighest = ExpressiveDarkSurfaceContainerHighest,
            surfaceDim = ExpressiveDarkBackground,
            surfaceBright = Color(0xFF323741)
        )
    } else {
        lightColorScheme(
            primary = PageLightPrimary,
            onPrimary = PageLightOnPrimary,
            primaryContainer = PageLightPrimaryContainer,
            onPrimaryContainer = PageLightOnPrimaryContainer,
            secondary = PageLightSecondary,
            onSecondary = PageLightOnSecondary,
            secondaryContainer = PageLightSecondaryContainer,
            onSecondaryContainer = PageLightOnSecondaryContainer,
            tertiary = PageLightTertiary,
            onTertiary = PageLightOnTertiary,
            tertiaryContainer = PageLightTertiaryContainer,
            onTertiaryContainer = PageLightOnTertiaryContainer,
            error = ExpressiveLightError,
            onError = ExpressiveLightOnError,
            errorContainer = ExpressiveLightErrorContainer,
            onErrorContainer = ExpressiveLightOnErrorContainer,
            background = PageLightBackground,
            onBackground = BrandBlack,
            surface = PageLightSurface,
            onSurface = BrandBlack,
            surfaceVariant = PageLightSurfaceLow,
            onSurfaceVariant = Color(0xFF55435C),
            outline = PageLightOutline,
            outlineVariant = PageLightOutlineVariant,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = PageLightSurfaceLow,
            surfaceContainer = Color(0xFFF0D9F7),
            surfaceContainerHigh = PageLightSurfaceHigh,
            surfaceContainerHighest = Color(0xFFE4C2EE),
            surfaceDim = Color(0xFFE8D5EC),
            surfaceBright = Color(0xFFFFF7FF)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}
