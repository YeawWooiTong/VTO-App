package com.TOTOMOFYP.VTOAPP.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment
import com.TOTOMOFYP.VTOAPP.ui.base.ComposeScreen
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme
import com.TOTOMOFYP.VTOAPP.ui.outfits.Outfit
import com.TOTOMOFYP.VTOAPP.ui.wardrobe.ClothingItem

class FavoritesFragment : BaseFragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    ComposeScreen {
                        FavoritesScreen(
                            authViewModel = authViewModel,
                            onClothingItemClick = { item ->
                                val bundle = Bundle().apply {
                                    when (item.id) {
                                        is Long -> putLong("itemId", item.id as Long)
                                        is Int -> putInt("itemId", item.id as Int)
                                        is String -> putString("itemId", item.id as String)
                                        else -> putString("itemId", item.id.toString())
                                    }
                                }
                                findNavController().navigate(R.id.action_navigation_favorites_to_clothingItemDetailsFragment, bundle)
                            },
                            onOutfitClick = { outfit ->
                                val bundle = Bundle().apply {
                                    putString("outfitId", outfit.id)
                                }
                                findNavController().navigate(R.id.action_navigation_favorites_to_outfitDetailsFragment, bundle)
                            }
                        )
                    }
                }
            }
        }
    }
} 