package com.TOTOMOFYP.VTOAPP.ui.tryon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.ui.base.ComposeScreen
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme

class VirtualTryOnFragment : Fragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var tryOnViewModel: TryOnViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the TryOnViewModel with AuthViewModel dependency
        tryOnViewModel = ViewModelProvider(
            requireActivity(),
            TryOnViewModel.Factory(authViewModel)
        )[TryOnViewModel::class.java]
        
        // Load wardrobe items when this screen is created
        tryOnViewModel.loadWardrobeItems()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    ComposeScreen {
                        VirtualTryOnScreen(
                            tryOnViewModel = tryOnViewModel,
                            onBackClick = { 
                                // Reset state and navigate back
                                tryOnViewModel.resetState()
                                findNavController().popBackStack() 
                            },
                            onCameraClick = {
                                // Navigate back to the TryOn screen to take a new photo
                                findNavController().popBackStack(findNavController().graph.startDestinationId, false)
                            }
                        )
                    }
                }
            }
        }
    }
} 