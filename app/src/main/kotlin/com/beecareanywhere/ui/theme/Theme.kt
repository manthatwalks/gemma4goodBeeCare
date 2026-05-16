package com.beecareanywhere.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = HoneyAmber,
    onPrimary = NightHive,
    secondary = HiveBrown,
    onSecondary = WaxCream,
    tertiary = PollenYellow,
    background = WaxCream,
    surface = WaxCream,
)

private val DarkColors = darkColorScheme(
    primary = HoneyAmberDark,
    onPrimary = WaxCream,
    secondary = PollenYellow,
    onSecondary = NightHive,
    tertiary = HoneyAmber,
    background = NightHive,
    surface = NightHive,
)

@Composable
fun BeeCareAnywhereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Force our honey palette. Material You (dynamic color) overrides the brand
    // with the device wallpaper's tones, which looks generic. Opt-in later if we
    // add a "match system colors" setting.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
