package com.TOTOMOFYP.VTOAPP.ui.makeup

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

class MakeupStorageFragment : BaseFragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val makeupStorageViewModel: MakeupStorageViewModel by viewModels {
        MakeupStorageViewModel.Factory(authViewModel)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    MakeupStorageScreen(
                        makeupStorageViewModel = makeupStorageViewModel,
                        onBackClick = {
                            findNavController().navigateUp()
                        },
                        onItemClick = { makeupLook ->
                            val bundle = Bundle().apply {
                                putParcelable("makeupLook", makeupLook)
                            }
                            findNavController().navigate(
                                R.id.action_makeupStorageFragment_to_makeupDetailFragment,
                                bundle
                            )
                        }
                    )
                }
            }
        }
    }
}