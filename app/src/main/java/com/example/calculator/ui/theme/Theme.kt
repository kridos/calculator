package com.example.calculator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CalculatorOrange = Color(0xFFFF9F0A)
private val CalculatorBlue = Color(0xFF5AC8FA)
private val CalculatorPink = Color(0xFFFF6B9A)
private val CalculatorBgDark = Color(0xFF090A0F)
private val CalculatorSurfaceDark = Color(0xFF11131A)
private val CalculatorPanelDark = Color(0xFF171A22)
private val CalculatorTextDark = Color(0xFFF7F8FC)
private val CalculatorMutedDark = Color(0x99FFFFFF)

private val CalculatorBgLight = Color(0xFFFAFAFD)
private val CalculatorSurfaceLight = Color(0xFFFFFFFF)
private val CalculatorPanelLight = Color(0xFFF1F3F8)
private val CalculatorTextLight = Color(0xFF13161F)
private val CalculatorMutedLight = Color(0x99000000)

private val DarkColorScheme = darkColorScheme(
    primary = CalculatorOrange,
    onPrimary = Color(0xFF101214),
    secondary = CalculatorBlue,
    onSecondary = Color(0xFF06131A),
    tertiary = CalculatorPink,
    onTertiary = Color(0xFF1C0710),
    background = CalculatorBgDark,
    onBackground = CalculatorTextDark,
    surface = CalculatorSurfaceDark,
    onSurface = CalculatorTextDark,
    surfaceVariant = CalculatorPanelDark,
    onSurfaceVariant = CalculatorMutedDark,
    outline = Color(0x26FFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = CalculatorOrange,
    onPrimary = Color(0xFF101214),
    secondary = CalculatorBlue,
    onSecondary = Color(0xFF06131A),
    tertiary = CalculatorPink,
    onTertiary = Color(0xFF1C0710),
    background = CalculatorBgLight,
    onBackground = CalculatorTextLight,
    surface = CalculatorSurfaceLight,
    onSurface = CalculatorTextLight,
    surfaceVariant = CalculatorPanelLight,
    onSurfaceVariant = CalculatorMutedLight,
    outline = Color(0x1F000000)
)

@Composable
fun CalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
