package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WingoDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = Color(0xFF0E3A4D),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = EmeraldPulse,
    onSecondary = ObsidianBg,
    secondaryContainer = Color(0xFF0B3B2E),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = AmberLarge,
    onTertiary = ObsidianBg,
    tertiaryContainer = Color(0xFF452809),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianBorder,
    error = RoseLoss,
    onError = Color.White
)

private val WingoLightColorScheme = lightColorScheme(
    primary = DeepCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF083344),
    secondary = DeepEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = DeepAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    error = RoseLoss,
    onError = Color.White
)

val WingoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WingoPulseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WingoDarkColorScheme else WingoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WingoTypography,
        shapes = WingoShapes,
        content = content
    )
}
