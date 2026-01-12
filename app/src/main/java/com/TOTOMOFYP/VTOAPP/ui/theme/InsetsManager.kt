package com.TOTOMOFYP.VTOAPP.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A composition local for insets that can be used across all Compose screens
 */
val LocalInsets = compositionLocalOf { Insets() }

/**
 * Data class representing insets values
 */
data class Insets(
    val statusBarHeight: Dp = 0.dp,
    val navigationBarHeight: Dp = 0.dp
)

/**
 * A composable function that provides insets to its content
 */
@Composable
fun InsetsProvider(content: @Composable () -> Unit) {
    val navigationBarInsets = WindowInsets.navigationBars.asPaddingValues()
    
    val insets = Insets(
        statusBarHeight = 0.dp,
        navigationBarHeight = navigationBarInsets.calculateBottomPadding()
    )
    
    CompositionLocalProvider(LocalInsets provides insets) {
        content()
    }
} 