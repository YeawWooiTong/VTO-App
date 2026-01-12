package com.TOTOMOFYP.VTOAPP.ui.tryon

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Custom icons that aren't included in the standard Material Icons
 */
object CustomIcons {
    /**
     * Wardrobe icon for clothing selection
     */
    val Wardrobe: ImageVector = materialIcon(name = "CustomIcons.Wardrobe") {
        materialPath {
            // Simple wardrobe/closet icon path
            moveTo(4f, 4f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            
            // Door divider line
            moveTo(12f, 4f)
            verticalLineTo(20f)
            
            // Handles
            moveTo(8f, 12f)
            horizontalLineTo(10f)
            moveTo(14f, 12f)
            horizontalLineTo(16f)
        }
    }
} 