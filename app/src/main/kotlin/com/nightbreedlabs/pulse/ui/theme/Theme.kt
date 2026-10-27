package com.nightbreedlabs.pulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TokyoNightColorScheme = darkColorScheme(
    primary = TokyoPrimary,
    onPrimary = TokyoOnPrimary,
    primaryContainer = TokyoPrimaryContainer,
    onPrimaryContainer = TokyoOnPrimaryContainer,
    secondary = TokyoSecondary,
    onSecondary = TokyoOnSecondary,
    secondaryContainer = TokyoSecondaryContainer,
    onSecondaryContainer = TokyoOnSecondaryContainer,
    tertiary = TokyoTertiary,
    onTertiary = TokyoOnTertiary,
    tertiaryContainer = TokyoTertiaryContainer,
    onTertiaryContainer = TokyoOnTertiaryContainer,
    background = TokyoBackground,
    onBackground = TokyoTextPrimary,
    surface = TokyoSurface,
    onSurface = TokyoTextPrimary,
    surfaceVariant = TokyoSurfaceVariant,
    onSurfaceVariant = TokyoTextSecondary,
    surfaceContainer = TokyoSurfaceCard,
    error = TokyoError,
    onError = TokyoOnError,
    errorContainer = TokyoErrorContainer,
    onErrorContainer = TokyoOnErrorContainer,
    outline = TokyoOutline,
    outlineVariant = TokyoOutlineVariant
)

@Composable
fun NightBreedPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TokyoNightColorScheme,
        typography = Typography,
        content = content
    )
}
