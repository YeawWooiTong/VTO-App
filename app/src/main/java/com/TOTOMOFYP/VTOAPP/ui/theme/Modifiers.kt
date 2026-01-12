package com.TOTOMOFYP.VTOAPP.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Extension function to add padding that accounts for the bottom navigation bar
 * This ensures content at the bottom of the screen is not covered by the navigation bar
 */
fun Modifier.bottomNavPadding(): Modifier = composed {
    val bottomNavHeight = 72.dp
    this.padding(bottom = bottomNavHeight)
}

/**
 * Extension function for more precise control of bottom padding
 * Use this when you need to add padding specifically for buttons or content at the very bottom
 */
fun Modifier.bottomActionPadding(): Modifier = composed {
    val actionPadding = 84.dp
    this.padding(bottom = actionPadding)
}

/**
 * Extension function to provide safe padding that accounts for system navigation bars
 * and adds extra space for the custom bottom navigation
 */
@Composable
fun Modifier.safeBottomPadding(): Modifier = composed {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomNavHeight = 72.dp
    
    val totalPadding = with(LocalDensity.current) {
        navBarPadding + bottomNavHeight
    }
    
    this.padding(bottom = totalPadding)
} 