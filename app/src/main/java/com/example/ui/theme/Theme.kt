package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RouterTrackerDarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = MidnightBg,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanAccent,
    secondary = PurpleAccent,
    onSecondary = MidnightBg,
    secondaryContainer = PurpleContainer,
    onSecondaryContainer = PurpleAccent,
    tertiary = AmberShield,
    onTertiary = MidnightBg,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberShield,
    background = MidnightBg,
    onBackground = TextPrimary,
    surface = SlateCard,
    onSurface = TextPrimary,
    surfaceVariant = SlateCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    outlineVariant = SlateSubtle,
    error = RoseOffline,
    onError = TextPrimary,
    errorContainer = RoseContainer,
    onErrorContainer = RoseOffline
)

private val RouterTrackerShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RouterTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RouterTrackerDarkColorScheme,
        typography = AppTypography,
        shapes = RouterTrackerShapes,
        content = content
    )
}
