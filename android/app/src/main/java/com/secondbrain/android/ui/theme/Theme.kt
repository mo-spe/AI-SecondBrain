package com.secondbrain.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF395BCE), onPrimary = Color.White,
    primaryContainer = Color(0xFFE5ECFF), onPrimaryContainer = Color(0xFF172858),
    secondary = Color(0xFF53617F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E3FF), onSecondaryContainer = Color(0xFF243653),
    tertiary = Color(0xFF395BCE), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E3FF), onTertiaryContainer = Color(0xFF182F65),
    background = Color(0xFFF7F8FC), onBackground = Color(0xFF182039),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF182039),
    surfaceVariant = Color(0xFFEEF1F8), onSurfaceVariant = Color(0xFF4F5B73),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF3F5FB),
    surfaceContainer = Color(0xFFEEF1F8), surfaceContainerHigh = Color(0xFFE7ECF6), surfaceContainerHighest = Color(0xFFE0E6F1),
    outline = Color(0xFF65708B), outlineVariant = Color(0xFFD5DCEB)
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB5C6FF), onPrimary = Color(0xFF172858),
    primaryContainer = Color(0xFF2C4498), onPrimaryContainer = Color(0xFFE5ECFF),
    secondary = Color(0xFFC2CBE0), onSecondary = Color(0xFF28334D),
    secondaryContainer = Color(0xFF3E4A65), onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFFB5C6FF), onTertiary = Color(0xFF182F65),
    tertiaryContainer = Color(0xFF2C4498), onTertiaryContainer = Color(0xFFD6E3FF),
    background = Color(0xFF111520), onBackground = Color(0xFFE4E8F3),
    surface = Color(0xFF191E2B), onSurface = Color(0xFFE4E8F3),
    surfaceVariant = Color(0xFF4F5B73), onSurfaceVariant = Color(0xFFD5DCEB),
    surfaceContainerLowest = Color(0xFF0D111B), surfaceContainerLow = Color(0xFF191E2B),
    surfaceContainer = Color(0xFF202637), surfaceContainerHigh = Color(0xFF2A3245), surfaceContainerHighest = Color(0xFF353F55),
    outline = Color(0xFF929CB5), outlineVariant = Color(0xFF4F5B73)
)

@Composable
fun SecondBrainTheme(content: @Composable () -> Unit) {
    // A stable brand palette keeps the knowledge workspace recognisable across device wallpapers.
    val scheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
