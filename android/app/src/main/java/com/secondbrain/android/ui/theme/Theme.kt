package com.secondbrain.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF245C55), onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAE2), onPrimaryContainer = Color(0xFF173E37),
    secondary = Color(0xFF52675E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EEE7), onSecondaryContainer = Color(0xFF273D33),
    tertiary = Color(0xFFAA603D), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5E4D9), onTertiaryContainer = Color(0xFF65351F),
    background = Color(0xFFF6F7F3), onBackground = Color(0xFF172D29),
    surface = Color.White, onSurface = Color(0xFF172D29),
    surfaceVariant = Color(0xFFE8EEE7), onSurfaceVariant = Color(0xFF63716B),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F3ED),
    surfaceContainer = Color(0xFFEBEFE8), surfaceContainerHigh = Color(0xFFE3E9E1), surfaceContainerHighest = Color(0xFFDCE3DA),
    outline = Color(0xFF76847C), outlineVariant = Color(0xFFDCE3DC),
    error = Color(0xFFB3261E), onError = Color.White
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA4D0BD), onPrimary = Color(0xFF10382F),
    primaryContainer = Color(0xFF285046), onPrimaryContainer = Color(0xFFDBEAE2),
    secondary = Color(0xFFBBCABD), onSecondary = Color(0xFF26392E),
    secondaryContainer = Color(0xFF3C5043), onSecondaryContainer = Color(0xFFDDE9DB),
    tertiary = Color(0xFFE9B494), onTertiary = Color(0xFF532911),
    tertiaryContainer = Color(0xFF713F24), onTertiaryContainer = Color(0xFFF5E4D9),
    background = Color(0xFF111B17), onBackground = Color(0xFFE2E9E0),
    surface = Color(0xFF19251F), onSurface = Color(0xFFE2E9E0),
    surfaceVariant = Color(0xFF3D4C43), onSurfaceVariant = Color(0xFFC0CDC3),
    surfaceContainerLowest = Color(0xFF0C1511), surfaceContainerLow = Color(0xFF19251F),
    surfaceContainer = Color(0xFF202D25), surfaceContainerHigh = Color(0xFF29372F), surfaceContainerHighest = Color(0xFF35443A),
    outline = Color(0xFF8B9C8F), outlineVariant = Color(0xFF3D4C43)
)

@Composable
fun SecondBrainTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // 固定阅读配色，避免系统壁纸取色改变知识正文和状态标签的对比度。
    val scheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, typography = Typography,
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)),
        content = content)
}
