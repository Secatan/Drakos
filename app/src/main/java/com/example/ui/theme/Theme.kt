package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DrakosShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = DrakosRed,
    onPrimary = DrakosWhite,
    primaryContainer = DrakosRedContainer,
    onPrimaryContainer = DrakosRedDark,
    secondary = DrakosBlack,
    onSecondary = DrakosWhite,
    secondaryContainer = DrakosSurfaceTint,
    onSecondaryContainer = DrakosBlack,
    tertiary = SuccessGreen,
    onTertiary = DrakosWhite,
    tertiaryContainer = SuccessGreenContainer,
    onTertiaryContainer = SuccessGreenDark,
    error = DangerRed,
    errorContainer = DangerRedContainer,
    onError = DrakosWhite,
    onErrorContainer = Color(0xFF7F1D1D),
    background = DrakosCanvas,
    onBackground = DrakosBlack,
    surface = DrakosWhite,
    onSurface = DrakosBlack,
    surfaceVariant = DrakosSurfaceTint,
    onSurfaceVariant = DrakosMutedInk,
    outline = DrakosBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = DrakosRedBright,
    onPrimary = DrakosWhite,
    primaryContainer = DrakosRedDark,
    onPrimaryContainer = DrakosWhite,
    secondary = DrakosWhite,
    onSecondary = DrakosBlack,
    secondaryContainer = DrakosGraphite,
    onSecondaryContainer = DrakosWhite,
    tertiary = SuccessGreen,
    onTertiary = DrakosWhite,
    error = DangerRed,
    background = DrakosBlack,
    onBackground = DrakosWhite,
    surface = DrakosCharcoal,
    onSurface = DrakosWhite,
    surfaceVariant = DrakosGraphite,
    onSurfaceVariant = DrakosBorder,
    outline = DrakosMutedInk
)

@Composable
fun KioscoPOSTheme(
    darkTheme: Boolean = false, // Estética de alto contraste Blanco/Rojo/Negro Drakos para mostrador
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
        shapes = DrakosShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = KioscoPOSTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
