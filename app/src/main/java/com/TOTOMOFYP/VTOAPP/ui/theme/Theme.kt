package com.TOTOMOFYP.VTOAPP.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define additional colors
val White = Color(0xFFFFFFFF) // White background
val TextColor = Color(0xFF333333) // Dark gray text for readability
val SecondaryTextColor = Color(0xFF666666) // Lighter gray text

// Define Dark and Light color schemes
private val DarkColorScheme = darkColorScheme(
    primary = SoftBlushPinkDark,
    onPrimary = Color.White,
    primaryContainer = SoftBlushPink.copy(alpha = 0.8f),
    onPrimaryContainer = Color.White,
    secondary = SoftBlushPink,
    onSecondary = Color.White,
    secondaryContainer = SoftBlushPinkDark.copy(alpha = 0.7f),
    onSecondaryContainer = Color.White,
    tertiary = SoftBlushPinkLight,
    background = DarkBackground,
    surface = DarkBackground.copy(alpha = 0.9f),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SoftBlushPink,
    onPrimary = Color.White,
    primaryContainer = SoftBlushPinkLight,
    onPrimaryContainer = DarkGray,
    secondary = SoftBlushPinkDark,
    onSecondary = Color.White,
    secondaryContainer = SoftBlushPinkLight,
    onSecondaryContainer = SoftBlushPinkDark,
    tertiary = SoftBlushPinkLight,
    background = LightBackground,
    surface = Color.White,
    onBackground = DarkGray,
    onSurface = DarkGray
)

@Composable
fun VTOAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Setting to false to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
//        when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Status bar matches primary color
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
