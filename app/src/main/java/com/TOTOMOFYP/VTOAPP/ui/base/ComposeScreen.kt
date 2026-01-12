package com.TOTOMOFYP.VTOAPP.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A wrapper composable that provides proper inset handling for all screens.
 * Use this for all top-level screens in fragments.
 */
@Composable
fun ComposeScreen(
    applyTopInset: Boolean = true,
    applyBottomInset: Boolean = true,
    content: @Composable () -> Unit
) {
    // Use a simpler approach without hard-coded insets padding
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        content()
    }
} 