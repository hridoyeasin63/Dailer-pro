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

private val DarkColorScheme = darkColorScheme(
    primary = OceanTealDarkPrimary,
    onPrimary = OnOceanTealDarkPrimary,
    primaryContainer = OceanTealDarkPrimaryContainer,
    onPrimaryContainer = OnOceanTealDarkPrimaryContainer,
    secondary = SlateDarkSecondary,
    onSecondary = OnSlateDarkSecondary,
    secondaryContainer = SlateDarkSecondaryContainer,
    onSecondaryContainer = OnSlateDarkSecondaryContainer,
    tertiary = CoralDarkTertiary,
    onTertiary = OnCoralDarkTertiary,
    tertiaryContainer = CoralDarkTertiaryContainer,
    onTertiaryContainer = OnCoralDarkTertiaryContainer,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = OceanTealPrimary,
    onPrimary = OnOceanTealPrimary,
    primaryContainer = OceanTealPrimaryContainer,
    onPrimaryContainer = OnOceanTealPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = OnSlateSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = OnSlateSecondaryContainer,
    tertiary = CoralTertiary,
    onTertiary = OnCoralTertiary,
    tertiaryContainer = CoralTertiaryContainer,
    onTertiaryContainer = OnCoralTertiaryContainer,
    background = LightBackground,
    onBackground = OnLightBackground,
    surface = LightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant
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
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
