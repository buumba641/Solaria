package com.solaria.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────────────
// Solaria vibrant sleek palette: Blue & Purple with Green accents
// ──────────────────────────────────────────────────────────────
val SolariaBlack       = Color(0xFF0B0B0F)
val SolariaSurface     = Color(0xFF14151B)
val SolariaSurfaceAlt  = Color(0xFF1C1E26)
val SolariaOnSurface   = Color(0xFFE9ECF3)
val SolariaMuted       = Color(0xFF9AA1B5)

val SolariaPurple      = Color(0xFF8E24AA) // Sleek Deep Purple
val SolariaPurpleLight = Color(0xFFCE93D8)
val SolariaPurpleSoft  = Color(0xFF1A1221)

val SolariaBlue        = Color(0xFF2979FF) // Vibrant Blue
val SolariaBlueLight   = Color(0xFF82B1FF)
val SolariaBlueSoft    = Color(0xFF0D1726)

val SolariaGreen       = Color(0xFF00E676) // Neon Green for borders/accents
val SolariaGreenDark   = Color(0xFF00C853)
val SolariaGreenLight  = Color(0xFF6EF2A0)

val SolariaError       = Color(0xFFFF5252)

// Aliases
val CryptoGreen = SolariaGreen
val CryptoRed   = SolariaError

// ──────────────────────────────────────────────────────────────
// Typography — uses system sans-serif with curated weight/size scale.
// Swap SolariaFontFamily to a Google Font (e.g. Inter, Space Grotesk)
// by adding the font files to res/font/ and updating the FontFamily below.
// ──────────────────────────────────────────────────────────────
private val SolariaFontFamily = FontFamily.SansSerif

private val SolariaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SolariaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = SolariaPurple,
    onPrimary = Color.White,
    primaryContainer = SolariaPurpleSoft,
    onPrimaryContainer = SolariaPurpleLight,
    secondary = SolariaBlue,
    onSecondary = Color.White,
    secondaryContainer = SolariaBlueSoft,
    onSecondaryContainer = SolariaBlueLight,
    tertiary = SolariaGreen,
    onTertiary = SolariaBlack,
    background = SolariaBlack,
    onBackground = SolariaOnSurface,
    surface = SolariaSurface,
    onSurface = SolariaOnSurface,
    surfaceVariant = SolariaSurfaceAlt,
    onSurfaceVariant = SolariaMuted,
    outline = SolariaGreen.copy(alpha = 0.5f), // Green borders by default
    error = SolariaError,
    onError = Color.White,
)

// We focus on the Dark theme for that "sleek" look requested
private val LightColorScheme = lightColorScheme(
    primary = SolariaPurple,
    onPrimary = Color.White,
    secondary = SolariaBlue,
    onSecondary = Color.White,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    outline = SolariaGreen.copy(alpha = 0.3f)
)

@Composable
fun SolariaTheme(
    darkTheme: Boolean = true, // Force dark for sleek design unless specified
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SolariaTypography,
        content     = content
    )
}
