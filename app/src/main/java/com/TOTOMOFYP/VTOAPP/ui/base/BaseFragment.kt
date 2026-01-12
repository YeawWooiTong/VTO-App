package com.TOTOMOFYP.VTOAPP.ui.base

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment

/**
 * Base Fragment class that properly handles system insets for all fragments.
 * Extend this class for all fragments that need proper inset handling.
 */
open class BaseFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Apply inset handling to all child fragments
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            // Only apply bottom padding for navigation, not top padding for status bar
            v.updatePadding(
                bottom = navigationBarInsets.bottom
            )
            
            // Return the insets so that they can be consumed by child views if needed
            insets
        }
    }
} 