package com.TOTOMOFYP.VTOAPP.ui.outfits

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme

class OutfitsFragment : BaseFragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val outfitsViewModel: OutfitsViewModel by viewModels {
        OutfitsViewModel.Factory(authViewModel)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val selectForMakeup = arguments?.getBoolean("select_for_makeup", false) ?: false
        
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    OutfitsScreen(
                        outfitsViewModel = outfitsViewModel,
                        authViewModel = authViewModel,
                        onCreateOutfitClick = {
                            findNavController().navigate(R.id.action_navigation_outfits_to_createOutfitFragment)
                        },
                        onOutfitClick = { outfit ->
                            if (selectForMakeup) {
                                // Navigate to BanubaActivity with outfit image for makeup
                                navigateToMakeupWithOutfitImage(outfit.imageUrl)
                            } else {
                                // Normal outfit details navigation
                                val bundle = Bundle().apply {
                                    putString("outfitId", outfit.id)
                                }
                                findNavController().navigate(R.id.action_navigation_outfits_to_outfitDetailsFragment, bundle)
                            }
                        },
                        onMakeupStorageClick = {
                            findNavController().navigate(R.id.action_navigation_outfits_to_makeupStorageFragment)
                        },
                        selectForMakeup = selectForMakeup
                    )
                }
            }
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        outfitsViewModel.loadOutfits()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh outfits when returning to this fragment (e.g., after saving an outfit)
        outfitsViewModel.refreshOutfits()
    }
    
    private fun navigateToMakeupWithOutfitImage(imageUrl: String?) {
        try {
            if (imageUrl.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No image available for this outfit", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Launch BanubaActivity with the outfit image for makeup
            val intent = Intent(requireContext(), com.TOTOMOFYP.VTOAPP.BanubaActivity::class.java)
            intent.putExtra("photo_uri", imageUrl)
            intent.putExtra("source_mode", "photo")
            startActivity(intent)
            
            // Navigate back to try-on fragment after starting the activity
            findNavController().navigateUp()
        } catch (e: Exception) {
            android.util.Log.e("OutfitsFragment", "Error launching makeup with outfit image", e)
            Toast.makeText(requireContext(), "Failed to launch makeup. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
} 