package com.secondbrain.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF4F62D7), onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E7FF), onPrimaryContainer = Color(0xFF17245C),
    secondary = Color(0xFF59627A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EDF7), onSecondaryContainer = Color(0xFF283249),
    tertiary = Color(0xFF9C4D24), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCCB), onTertiaryContainer = Color(0xFF3C1606),
    background = Color(0xFFF7F8FC), onBackground = Color(0xFF17213B),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF17213B),
    surfaceVariant = Color(0xFFEEF1F7), onSurfaceVariant = Color(0xFF58627A),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF3F5FA),
    surfaceContainer = Color(0xFFEDF0F7), surfaceContainerHigh = Color(0xFFE7EBF4), surfaceContainerHighest = Color(0xFFE0E5F0),
    outline = Color(0xFF69738A), outlineVariant = Color(0xFFD7DDEA)
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFBAC5FF), onPrimary = Color(0xFF202E78),
    primaryContainer = Color(0xFF34489F), onPrimaryContainer = Color(0xFFE1E7FF),
    secondary = Color(0xFFC1C8DB), onSecondary = Color(0xFF2A334A),
    secondaryContainer = Color(0xFF414A60), onSecondaryContainer = Color(0xFFE0E5F0),
    tertiary = Color(0xFFFFB692), onTertiary = Color(0xFF5C250E),
    tertiaryContainer = Color(0xFF7A3B1E), onTertiaryContainer = Color(0xFFFFDCCB),
    background = Color(0xFF111522), onBackground = Color(0xFFE4E8F3),
    surface = Color(0xFF191E2C), onSurface = Color(0xFFE4E8F3),
    surfaceVariant = Color(0xFF454E65), onSurfaceVariant = Color(0xFFC6CDDF),
    surfaceContainerLowest = Color(0xFF0C101A), surfaceContainerLow = Color(0xFF191E2C),
    surfaceContainer = Color(0xFF202637), surfaceContainerHigh = Color(0xFF2A3245), surfaceContainerHighest = Color(0xFF363F54),
    outline = Color(0xFF909AB2), outlineVariant = Color(0xFF454E65)
)

@Composable
fun SecondBrainTheme(content: @Composable () -> Unit) {
    // A stable brand palette keeps the knowledge workspace recognisable across device wallpapers.
    val scheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
