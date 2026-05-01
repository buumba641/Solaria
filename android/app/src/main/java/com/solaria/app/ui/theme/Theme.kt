package com.solaria.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────
// Brand colours – mifos-pay inspired green-on-white palette
// ──────────────────────────────────────────────────────────────
val SolariaGreen      = Color(0xFF00897B)   // teal-green primary
val SolariaGreenDark  = Color(0xFF00695C)
val SolariaGreenLight = Color(0xFF4DB6AC)
val SolariaSurface    = Color(0xFFF5F5F5)
val SolariaOnSurface  = Color(0xFF212121)
val SolariaError      = Color(0xFFD32F2F)

// Crypto-KMP accent for market data
val CryptoPurple      = Color(0xFF7C4DFF)
val CryptoGold        = Color(0xFFFFCA28)
val CryptoGreen       = Color(0xFF00C853)
val CryptoRed         = Color(0xFFFF1744)

private val LightColorScheme = lightColorScheme(
    primary          = SolariaGreen,
    onPrimary        = Color.White,
    primaryContainer = SolariaGreenLight,
    onPrimaryContainer = Color.White,
    secondary        = CryptoPurple,
    onSecondary      = Color.White,
    background       = SolariaSurface,
    onBackground     = SolariaOnSurface,
    surface          = Color.White,
    onSurface        = SolariaOnSurface,
    error            = SolariaError,
    onError          = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary          = SolariaGreenLight,
    onPrimary        = Color.Black,
    primaryContainer = SolariaGreenDark,
    onPrimaryContainer = Color.White,
    secondary        = CryptoPurple,
    onSecondary      = Color.White,
    background       = Color(0xFF121212),
    onBackground     = Color(0xFFE0E0E0),
    surface          = Color(0xFF1E1E1E),
    onSurface        = Color(0xFFE0E0E0),
    error            = SolariaError,
    onError          = Color.White,
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
