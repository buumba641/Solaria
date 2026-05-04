package com.solaria.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────
// Solaria vibrant palette
// ──────────────────────────────────────────────────────────────
val SolariaBlack       = Color(0xFF0B0B0F)
val SolariaSurface     = Color(0xFF14151B)
val SolariaSurfaceAlt  = Color(0xFF1C1E26)
val SolariaOnSurface   = Color(0xFFE9ECF3)
val SolariaMuted       = Color(0xFF9AA1B5)

val SolariaPurple      = Color(0xFF7C4DFF)
val SolariaPurpleLight = Color(0xFFB388FF)
val SolariaPurpleSoft  = Color(0xFFEDE7F6)

val SolariaBlue        = Color(0xFF2196F3)
val SolariaBlueLight   = Color(0xFF82B1FF)
val SolariaBlueSoft    = Color(0xFFE3F2FD)

val SolariaTeal        = Color(0xFF00B8D9)

val SolariaGreen       = Color(0xFF00C853)
val SolariaGreenDark   = Color(0xFF00A444)
val SolariaGreenLight  = Color(0xFF6EF2A0)
val SolariaGreenSoft   = Color(0xFFE8F5E9)

val SolariaError       = Color(0xFFFF5A5F)

// Aliases used by ApprovalCard and other components
val CryptoGreen = SolariaGreen
val CryptoRed   = SolariaError

private val LightColorScheme = lightColorScheme(
    primary = SolariaGreen,
    onPrimary = Color.White,
    primaryContainer = SolariaGreenSoft,
    onPrimaryContainer = SolariaGreenDark,
    secondary = SolariaPurple,
    onSecondary = Color.White,
    secondaryContainer = SolariaPurpleSoft,
    onSecondaryContainer = SolariaPurple,
    tertiary = SolariaBlue,
    onTertiary = Color.White,
    tertiaryContainer = SolariaBlueSoft,
    onTertiaryContainer = SolariaBlue,
    background = Color(0xFFF8FAFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF1F4F9),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFD8DBE5),
    error = SolariaError,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = SolariaGreenLight,
    onPrimary = Color(0xFF003912),
    primaryContainer = Color(0xFF00531D),
    onPrimaryContainer = SolariaGreenLight,
    secondary = SolariaPurpleLight,
    onSecondary = Color(0xFF430099),
    secondaryContainer = Color(0xFF5D31D1),
    onSecondaryContainer = SolariaPurpleLight,
    tertiary = SolariaBlueLight,
    onTertiary = Color(0xFF00315E),
    background = SolariaBlack,
    onBackground = SolariaOnSurface,
    surface = SolariaSurface,
    onSurface = SolariaOnSurface,
    surfaceVariant = SolariaSurfaceAlt,
    onSurfaceVariant = SolariaMuted,
    outline = Color(0xFF3F4759),
    error = SolariaError,
    onError = Color(0xFF690005),
)

@Composable
fun SolariaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
