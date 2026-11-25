package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkEmeraldOnPrimary,
    primaryContainer = DarkEmeraldPrimaryContainer,
    onPrimaryContainer = DarkEmeraldOnPrimaryContainer,
    secondary = DarkAmberSecondary,
    onSecondary = DarkAmberOnSecondary,
    secondaryContainer = DarkAmberSecondaryContainer,
    onSecondaryContainer = DarkAmberOnSecondaryContainer,
    tertiary = DarkTealTertiary,
    onTertiary = DarkTealOnTertiary,
    tertiaryContainer = DarkTealTertiaryContainer,
    onTertiaryContainer = DarkTealOnTertiaryContainer,
    background = DarkLedgerBackground,
    onBackground = DarkLedgerOnBackground,
    surface = DarkLedgerSurface,
    onSurface = DarkLedgerOnSurface,
    surfaceVariant = DarkLedgerSurfaceVariant,
    onSurfaceVariant = DarkLedgerOnSurfaceVariant,
    outline = DarkLedgerOutline,
    error = LossRed
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = AmberOnSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = TealOnTertiary,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = TealOnTertiaryContainer,
    background = LedgerBackground,
    onBackground = LedgerOnBackground,
    surface = LedgerSurface,
    onSurface = LedgerOnSurface,
    surfaceVariant = LedgerSurfaceVariant,
    onSurfaceVariant = LedgerOnSurfaceVariant,
    outline = LedgerOutline,
    outlineVariant = LedgerOutlineVariant,
    error = LossRed
)

val AfiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AfiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AfiTypography,
        shapes = AfiShapes,
        content = content
    )
}
