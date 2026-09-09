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

private val DarkColorScheme = darkColorScheme(
    primary = LeoIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = LeoIndigoContainerDark,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = LeoCyanSecondary,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = LeoCyanContainerDark,
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = LeoAmberAccent,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = LeoAmberContainerDark,
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = LeoDarkBackground,
    onBackground = LeoDarkOnSurface,
    surface = LeoDarkSurface,
    onSurface = LeoDarkOnSurface,
    surfaceVariant = LeoDarkSurfaceVariant,
    onSurfaceVariant = LeoDarkOnSurfaceVariant,
    outline = LeoDarkOutline,
    error = LeoError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LeoIndigoDark,
    onPrimary = Color.White,
    primaryContainer = LeoIndigoContainerLight,
    onPrimaryContainer = Color(0xFF312E81),
    secondary = LeoCyanSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = LeoCyanContainerLight,
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = LeoAmberAccent,
    onTertiary = Color.White,
    background = LeoLightBackground,
    onBackground = LeoLightOnSurface,
    surface = LeoLightSurface,
    onSurface = LeoLightOnSurface,
    surfaceVariant = LeoLightSurfaceVariant,
    onSurfaceVariant = LeoLightOnSurfaceVariant,
    outline = LeoLightOutline,
    error = LeoError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Default to the custom Leo AI theme for cohesive brand identity
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
