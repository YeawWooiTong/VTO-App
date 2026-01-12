package com.TOTOMOFYP.VTOAPP.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme

class AddEventFragment : BaseFragment() {

    private lateinit var viewModel: EventViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val factory = EventViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[EventViewModel::class.java]
        
        // Check if we're editing an existing event
        val eventToEdit = arguments?.let { bundle ->
            if (bundle.getString("action") == "edit") {
                Event(
                    id = bundle.getString("eventId", ""),
                    title = bundle.getString("eventTitle", ""),
                    date = bundle.getString("eventDate", ""),
                    time = bundle.getString("eventTime", ""),
                    location = bundle.getString("eventLocation", ""),
                    description = bundle.getString("eventDescription", ""),
                    eventType = try {
                        EventType.valueOf(bundle.getString("eventType", EventType.OTHER.name))
                    } catch (e: Exception) {
                        EventType.OTHER
                    },
                    outfitImageUrl = bundle.getString("outfitImageUrl"),
                    plannedOutfitId = bundle.getString("plannedOutfitId"),
                    reminderEnabled = bundle.getBoolean("reminderEnabled", true),
                    isAllDay = bundle.getBoolean("isAllDay", false)
                )
            } else null
        }

        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    AddEventScreen(
                        eventToEdit = eventToEdit,
                        viewModel = viewModel,
                        onNavigateBack = {
                            findNavController().navigateUp()
                        },
                        onEventCreated = {
                            findNavController().navigateUp()
                        }
                    )
                }
            }
        }
    }
}