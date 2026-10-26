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
    inversePrimary = OylGreenLight,
    secondary = OylBrown,
    onSecondary = Color.White,
    secondaryContainer = OylBrownContainer,
    onSecondaryContainer = Color(0xFF3E2410),
    tertiary = Color(0xFF8A5300),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2B1700),
    background = OylCream,
    onBackground = OylInk,
    surface = OylCream,
    onSurface = OylInk,
    surfaceVariant = OylBeige,
    onSurfaceVariant = Color(0xFF4A564D),
    surfaceTint = OylGreen,
    inverseSurface = Color(0xFF2E3530),
    inverseOnSurface = Color(0xFFEFF2EE),
    surfaceBright = OylCream,
    surfaceDim = Color(0xFFE3DCCD),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F1E4),
    surfaceContainer = OylBeige,
    surfaceContainerHigh = OylBeigeDark,
    surfaceContainerHighest = Color(0xFFD8CAAE),
    outline = Color(0xFF7D7462),
    outlineVariant = OylBeigeDark,
    error = OylError,
    onError = Color.White,
    errorContainer = OylErrorContainer,
    onErrorContainer = Color(0xFF5F1111),
    scrim = Color.Black,
)

// Esquema oscuro completo (sin heredar los neutros morados por defecto de M3): neutros verdosos,
// cada color con su "on" correspondiente y contraste AA.
private val DarkColors = darkColorScheme(
    primary = OylGreenLight,
    onPrimary = OylGreenDark,
    primaryContainer = OylGreen,
    onPrimaryContainer = OylGreenContainer,
    inversePrimary = OylGreen,
    secondary = Color(0xFFE2B98F),
    onSecondary = Color(0xFF3E2410),
    secondaryContainer = Color(0xFF5A3D20),
    onSecondaryContainer = OylBrownContainer,
    tertiary = Color(0xFFFFB95C),
    onTertiary = Color(0xFF472A00),
    tertiaryContainer = Color(0xFF6A3F00),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF121813),
    onBackground = Color(0xFFE2E8E2),
    surface = Color(0xFF121813),
    onSurface = Color(0xFFE2E8E2),
    surfaceVariant = Color(0xFF2B332D),
    onSurfaceVariant = Color(0xFFBFC9C1),
    surfaceTint = OylGreenLight,
    inverseSurface = Color(0xFFE2E8E2),
    inverseOnSurface = Color(0xFF2B312C),
    surfaceBright = Color(0xFF373E38),
    surfaceDim = Color(0xFF121813),
    surfaceContainerLowest = Color(0xFF0C110D),
    surfaceContainerLow = Color(0xFF1A201B),
    surfaceContainer = Color(0xFF1E251F),
    surfaceContainerHigh = Color(0xFF29302A),
    surfaceContainerHighest = Color(0xFF333B35),
    outline = Color(0xFF8A948C),
    outlineVariant = Color(0xFF404942),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
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
