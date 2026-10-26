package cl.oyl.frutossecos.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = OylGreen,
    onPrimary = Color.White,
    primaryContainer = OylGreenContainer,
    onPrimaryContainer = OylGreenDark,
    secondary = OylBrown,
    onSecondary = Color.White,
    secondaryContainer = OylBrownContainer,
    onSecondaryContainer = Color(0xFF3E2410),
    tertiary = OylWarning,
    background = OylCream,
    onBackground = OylInk,
    surface = OylCream,
    onSurface = OylInk,
    surfaceVariant = OylBeige,
    onSurfaceVariant = OylMuted,
    surfaceContainer = OylBeige,
    surfaceContainerLow = Color(0xFFF7F1E4),
    surfaceContainerHigh = OylBeigeDark,
    outline = Color(0xFFB9AE98),
    outlineVariant = OylBeigeDark,
    error = OylError,
    errorContainer = OylErrorContainer,
)

private val DarkColors = darkColorScheme(
    primary = OylGreenLight,
    onPrimary = OylGreenDark,
    primaryContainer = OylGreen,
    onPrimaryContainer = OylGreenContainer,
    secondary = Color(0xFFE2B98F),
    background = Color(0xFF141A16),
    surface = Color(0xFF141A16),
    surfaceVariant = Color(0xFF2B312C),
)

@Composable
fun OylTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = OylTypography,
        shapes = OylShapes,
        content = content,
    )
}
