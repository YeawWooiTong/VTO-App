package com.TOTOMOFYP.VTOAPP.ui.makeup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme

class MakeupDetailFragment : BaseFragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val makeupStorageViewModel: MakeupStorageViewModel by viewModels {
        MakeupStorageViewModel.Factory(authViewModel)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Get the makeup look from arguments manually for now
        val makeupLook = arguments?.getParcelable<MakeupLook>("makeupLook")
            ?: throw IllegalArgumentException("MakeupLook argument is required")
            
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    MakeupDetailScreen(
                        makeupLook = makeupLook,
                        onBackClick = {
                            findNavController().navigateUp()
                        },
                        onDeleteClick = {
                            makeupStorageViewModel.deleteMakeupLook(makeupLook.id)
                            findNavController().navigateUp()
                        }
                    )
                }
            }
        }
    }
}