package com.TOTOMOFYP.VTOAPP.ui.home

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

class HomeFragment : BaseFragment() {

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
                        HomeScreen(
                            authViewModel = authViewModel,
                            onTryOnClick = {
                                findNavController().navigate(R.id.navigation_try_on)
                            },
                            onWardrobeClick = {
                                findNavController().navigate(R.id.navigation_wardrobe)
                            },
                            onOutfitsClick = {
                                findNavController().navigate(R.id.navigation_outfits)
                            },
                            onFavoritesClick = {
                                findNavController().navigate(R.id.navigation_favorites)
                            }
                        )
                    }
                }
            }
        }
    }
} 