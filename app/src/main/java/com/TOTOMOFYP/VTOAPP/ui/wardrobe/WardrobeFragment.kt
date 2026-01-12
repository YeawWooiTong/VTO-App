package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme

class WardrobeFragment : BaseFragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val wardrobeViewModel: WardrobeViewModel by viewModels {
        WardrobeViewModel.Factory(authViewModel)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    WardrobeScreen(
                        authViewModel = authViewModel,
                        onAddClothingItem = {
                            // This action is handled internally in the Compose screen
                        },
                        onClothingItemClick = { clothingItem ->
                            val bundle = Bundle().apply {
                                putString("itemId", clothingItem.id)
                            }
                            findNavController().navigate(
                                R.id.action_navigation_wardrobe_to_clothingItemDetailsFragment,
                                bundle
                            )
                        },
                        wardrobeViewModel = wardrobeViewModel
                    )
                }
            }
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load wardrobe items
        wardrobeViewModel.loadClothingItems()
    }
} 