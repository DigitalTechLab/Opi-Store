package com.example.myapplication.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accentColor: String = "Standard",
    visualStyle: String = "Classic",
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        visualStyle == "Manga" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color.White,
                    secondary = Color.White,
                    tertiary = Color.White,
                    surface = Color.Black,
                    onSurface = Color.White,
                    background = Color.Black,
                    onBackground = Color.White
                )
            } else {
                lightColorScheme(
                    primary = Color.Black,
                    secondary = Color.Black,
                    tertiary = Color.Black,
                    surface = Color.White,
                    onSurface = Color.Black,
                    background = Color.White,
                    onBackground = Color.Black
                )
            }
        }
        accentColor != "Standard" -> {
            val primary = when(accentColor) {
                "Blue" -> BlueAccent
                "Green" -> GreenAccent
                "Red" -> RedAccent
                else -> if (darkTheme) Purple80 else Purple40
            }
            if (darkTheme) {
                darkColorScheme(
                    primary = primary,
                    secondary = PurpleGrey80,
                    tertiary = Pink80
                )
            } else {
                lightColorScheme(
                    primary = primary,
                    secondary = PurpleGrey40,
                    tertiary = Pink40
                )
            }
        }
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